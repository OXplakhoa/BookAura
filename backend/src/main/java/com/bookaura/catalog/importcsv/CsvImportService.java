package com.bookaura.catalog.importcsv;

import com.bookaura.catalog.entity.Author;
import com.bookaura.catalog.entity.Book;
import com.bookaura.catalog.entity.Category;
import com.bookaura.catalog.repository.BookRepository;
import com.bookaura.catalog.service.CatalogRelationResolver;
import com.bookaura.catalog.validation.IsbnUtils;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CsvImportService {

    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024; // strictly below 5 MiB
    public static final int MAX_ROWS = 10_000;
    private static final int MAX_REPORTED_ERRORS = 100;
    private static final List<String> EXPECTED_HEADERS = List.of(
            "title", "isbn", "authors", "categories", "publicationYear", "totalQuantity", "description");
    private static final Set<String> ACCEPTED_MEDIA_TYPES = Set.of(
            "text/csv", "application/csv", "application/vnd.ms-excel", "text/plain", "application/octet-stream");
    private static final Logger AUDIT = LoggerFactory.getLogger("com.bookaura.audit");

    private final BookRepository bookRepository;
    private final CatalogRelationResolver relationResolver;
    private final ObjectProvider<CsvImportFailureHook> failureHooks;

    public CsvImportService(BookRepository bookRepository, CatalogRelationResolver relationResolver,
                            ObjectProvider<CsvImportFailureHook> failureHooks) {
        this.bookRepository = bookRepository;
        this.relationResolver = relationResolver;
        this.failureHooks = failureHooks;
    }

    /**
     * All-or-nothing import. The input stream is parsed record-by-record (not readAll/getBytes),
     * while a bounded validated row model (max 10k rows / <5MiB file) is retained for one bulk DB flow.
     */
    @Transactional
    public CsvImportResult importBooks(MultipartFile file) {
        validateFile(file);
        List<ParsedRow> rows = parseRows(file);
        validateAgainstDatabase(rows);

        // Aggregate names once, then two bulk relation queries/saves. No DB call inside a row loop.
        List<String> allAuthorNames = rows.stream().flatMap(row -> row.authors().stream()).toList();
        List<String> allCategoryNames = rows.stream().flatMap(row -> row.categories().stream()).toList();
        Map<String, Author> authorsByName = relationResolver.resolveAuthors(allAuthorNames).stream()
                .collect(Collectors.toMap(Author::getNormalizedName, Function.identity()));
        Map<String, Category> categoriesByName = relationResolver.resolveCategories(allCategoryNames).stream()
                .collect(Collectors.toMap(Category::getNormalizedName, Function.identity()));

        List<Book> books = rows.stream()
                .map(row -> toBook(row, authorsByName, categoriesByName))
                .toList();
        bookRepository.saveAll(books);
        bookRepository.flush(); // proves actual DB mutations occurred before a test-only failure hook
        failureHooks.orderedStream().forEach(CsvImportFailureHook::afterPersist);

        AUDIT.info("event=BOOK_CSV_IMPORT importedCount={}", books.size());
        return new CsvImportResult(books.size());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE, "CSV file is required and cannot be empty");
        }
        if (file.getSize() >= MAX_FILE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "CSV file size must be strictly below 5 MiB");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".csv")) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE, "Only .csv files are supported");
        }
        String mediaType = file.getContentType();
        if (mediaType != null && !ACCEPTED_MEDIA_TYPES.contains(mediaType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE, "Unsupported CSV media type: " + mediaType);
        }
    }

    private List<ParsedRow> parseRows(MultipartFile file) {
        LinkedHashMap<String, String> errors = new LinkedHashMap<>();
        List<ParsedRow> rows = new ArrayList<>();
        Set<String> seenIsbns = new HashSet<>(); // O(n), replacing O(n²) pairwise duplicate checks

        CSVFormat format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (Reader reader = utf8BomAwareReader(file); CSVParser parser = format.parse(reader)) {
            if (!EXPECTED_HEADERS.equals(parser.getHeaderNames())) {
                throw new BusinessException(ErrorCode.CSV_HEADER_INVALID,
                        "CSV header must be exactly: " + String.join(",", EXPECTED_HEADERS));
            }
            for (CSVRecord record : parser) {
                long rowNumber = record.getRecordNumber() + 1; // + header row
                if (rows.size() >= MAX_ROWS) {
                    errors.put("file.rows", "CSV supports at most " + MAX_ROWS + " data rows");
                    break;
                }
                int errorsBefore = errors.size();
                if (!record.isConsistent()) {
                    errors.put("row[" + rowNumber + "]", "Column count does not match the header");
                } else {
                    ParsedRow row = parseRecord(record, rowNumber, errors, seenIsbns);
                    if (errors.size() == errorsBefore && row != null) rows.add(row);
                }
                if (errors.size() >= MAX_REPORTED_ERRORS) {
                    errors.put("file.errors", "Error output limited to " + MAX_REPORTED_ERRORS + " entries");
                    break;
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException | UncheckedIOException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.CSV_VALIDATION_ERROR, "Malformed CSV: " + ex.getMessage());
        }

        if (rows.isEmpty() && errors.isEmpty()) {
            errors.put("file.rows", "CSV must contain at least one data row");
        }
        if (!errors.isEmpty()) {
            throw new CsvImportException("CSV contains invalid rows; nothing was imported", errors);
        }
        return rows;
    }

    private ParsedRow parseRecord(CSVRecord record, long row, Map<String, String> errors, Set<String> seenIsbns) {
        String prefix = "row[" + row + "].";
        String title = required(record.get("title"), prefix + "title", 255, errors);
        String rawIsbn = required(record.get("isbn"), prefix + "isbn", 20, errors);
        String isbn = IsbnUtils.normalize(rawIsbn);
        if (rawIsbn != null && !IsbnUtils.isValid(rawIsbn)) {
            errors.put(prefix + "isbn", "Invalid ISBN-10/ISBN-13 checksum");
        } else if (isbn != null && !seenIsbns.add(isbn)) {
            errors.put(prefix + "isbn", "Duplicate ISBN within this file");
        }

        Integer year = parseInteger(record.get("publicationYear"), prefix + "publicationYear", errors);
        if (year != null && (year < 1450 || year > 2100)) {
            errors.put(prefix + "publicationYear", "Publication year must be between 1450 and 2100");
        }
        Integer quantity = parseInteger(record.get("totalQuantity"), prefix + "totalQuantity", errors);
        if (quantity != null && (quantity < 0 || quantity > 100_000)) {
            errors.put(prefix + "totalQuantity", "Total quantity must be between 0 and 100000");
        }

        List<String> authors = splitNames(record.get("authors"), prefix + "authors", 20, 120, errors);
        List<String> categories = splitNames(record.get("categories"), prefix + "categories", 10, 80, errors);
        String description = record.get("description").trim();
        if (description.length() > 4000) errors.put(prefix + "description", "Description exceeds 4000 characters");

        return new ParsedRow(row, title, isbn, authors, categories, year, quantity,
                description.isBlank() ? null : description);
    }

    private void validateAgainstDatabase(List<ParsedRow> rows) {
        Set<String> requested = rows.stream().map(ParsedRow::isbn).collect(Collectors.toSet());
        Set<String> existing = bookRepository.findAllByIsbnIn(requested).stream()
                .map(Book::getIsbn).collect(Collectors.toSet());
        if (!existing.isEmpty()) {
            LinkedHashMap<String, String> errors = new LinkedHashMap<>();
            rows.stream().filter(row -> existing.contains(row.isbn()))
                    .forEach(row -> errors.put("row[" + row.rowNumber() + "].isbn", "ISBN already exists"));
            throw new CsvImportException("CSV conflicts with existing books; nothing was imported", errors);
        }
    }

    private Book toBook(ParsedRow row, Map<String, Author> authors, Map<String, Category> categories) {
        Book book = new Book();
        book.setTitle(row.title());
        book.setIsbn(row.isbn());
        book.setDescription(row.description());
        book.setPublicationYear(row.publicationYear());
        book.setTotalQuantity(row.totalQuantity());
        book.setAvailableQuantity(row.totalQuantity());
        book.setActive(true);
        row.authors().forEach(name -> book.getAuthors().add(authors.get(CatalogRelationResolver.normalizeName(name))));
        row.categories().forEach(name -> book.getCategories().add(categories.get(CatalogRelationResolver.normalizeName(name))));
        return book;
    }

    private String required(String value, String key, int max, Map<String, String> errors) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) errors.put(key, "Value is required");
        else if (trimmed.length() > max) errors.put(key, "Value exceeds " + max + " characters");
        return trimmed;
    }

    private Integer parseInteger(String value, String key, Map<String, String> errors) {
        try {
            return Integer.valueOf(value.trim());
        } catch (Exception ex) {
            errors.put(key, "Must be a valid integer");
            return null;
        }
    }

    private List<String> splitNames(String raw, String key, int maxItems, int maxLength,
                                    Map<String, String> errors) {
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        Arrays.stream(raw.split("\\|"))
                .map(String::trim).filter(value -> !value.isBlank())
                .forEach(value -> unique.putIfAbsent(CatalogRelationResolver.normalizeName(value), value));
        if (unique.isEmpty()) errors.put(key, "At least one value is required (separate values with |)");
        if (unique.size() > maxItems) errors.put(key, "At most " + maxItems + " values are allowed");
        if (unique.values().stream().anyMatch(value -> value.length() > maxLength)) {
            errors.put(key, "Each value must be at most " + maxLength + " characters");
        }
        return List.copyOf(unique.values());
    }

    private Reader utf8BomAwareReader(MultipartFile file) throws IOException {
        PushbackInputStream input = new PushbackInputStream(file.getInputStream(), 3);
        byte[] first = input.readNBytes(3);
        boolean bom = first.length == 3 && first[0] == (byte) 0xEF && first[1] == (byte) 0xBB && first[2] == (byte) 0xBF;
        if (!bom && first.length > 0) input.unread(first);
        return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
    }

    private record ParsedRow(long rowNumber, String title, String isbn, List<String> authors,
                             List<String> categories, Integer publicationYear, Integer totalQuantity,
                             String description) {
    }
}
