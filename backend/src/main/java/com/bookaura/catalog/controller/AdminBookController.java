package com.bookaura.catalog.controller;

import com.bookaura.catalog.dto.BookRequest;
import com.bookaura.catalog.dto.BookResponse;
import com.bookaura.catalog.dto.BookSearchCriteria;
import com.bookaura.catalog.importcsv.CsvImportResult;
import com.bookaura.catalog.importcsv.CsvImportService;
import com.bookaura.catalog.service.BookService;
import com.bookaura.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Admin books", description = "ADMIN-only book management")
@RestController
@RequestMapping("/api/admin/books")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookController {

    private final BookService bookService;
    private final CsvImportService csvImportService;

    public AdminBookController(BookService bookService, CsvImportService csvImportService) {
        this.bookService = bookService;
        this.csvImportService = csvImportService;
    }

    @Operation(summary = "Create a book (ADMIN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) {
        return bookService.create(request);
    }

    @Operation(summary = "Update a book while preserving borrowed-copy count (ADMIN)")
    @PutMapping("/{id}")
    public BookResponse update(@PathVariable UUID id, @Valid @RequestBody BookRequest request) {
        return bookService.update(id, request);
    }

    @Operation(summary = "Soft-delete a book; repeated delete is idempotent (ADMIN)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        bookService.softDelete(id);
    }

    @Operation(summary = "Read book detail including inactive books (ADMIN)")
    @GetMapping("/{id}")
    public BookResponse detail(@PathVariable UUID id) {
        return bookService.getAdmin(id);
    }

    @Operation(summary = "Import books from CSV (ADMIN)",
            description = "Multipart .csv strictly below 5 MiB. Legacy header: " +
                    "title,isbn,authors,categories,publicationYear,totalQuantity,description; " +
                    "optional aura columns may append pageCount,tags. Authors/categories/tags use | separator. " +
                    "All-or-nothing transaction with row-level errors.")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvImportResult importCsv(@RequestPart("file") MultipartFile file) {
        return csvImportService.importBooks(file);
    }

    @Operation(summary = "Search books including active-status filter (ADMIN)",
            description = "Sort format: title:asc,publicationYear:desc. Max page size: 10.")
    @GetMapping
    public PageResponse<BookResponse> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Integer publicationYear,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return bookService.searchAdmin(
                new BookSearchCriteria(title, isbn, author, category, available, publicationYear, active),
                page, size, sort);
    }
}
