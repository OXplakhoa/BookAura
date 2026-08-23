package com.bookaura.catalog.service;

import com.bookaura.catalog.dto.*;
import com.bookaura.catalog.entity.Book;
import com.bookaura.catalog.repository.BookRepository;
import com.bookaura.catalog.specification.BookSpecifications;
import com.bookaura.catalog.validation.IsbnUtils;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.logging.LogOperation;
import com.bookaura.common.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@LogOperation
public class BookService {

    private final BookRepository bookRepository;
    private final CatalogRelationResolver relationResolver;
    private final BookPageRequestFactory pageRequestFactory;
    private final BookMapper mapper;

    public BookService(BookRepository bookRepository, CatalogRelationResolver relationResolver,
                       BookPageRequestFactory pageRequestFactory, BookMapper mapper) {
        this.bookRepository = bookRepository;
        this.relationResolver = relationResolver;
        this.pageRequestFactory = pageRequestFactory;
        this.mapper = mapper;
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        String isbn = IsbnUtils.normalize(request.isbn());
        if (bookRepository.existsByIsbn(isbn)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ISBN, "ISBN is already registered");
        }
        Book book = new Book();
        applyRequest(book, request, true);
        return mapper.toResponse(bookRepository.save(book));
    }

    @Transactional
    public BookResponse update(UUID id, BookRequest request) {
        Book book = bookRepository.findDetailedById(id)
                .orElseThrow(() -> notFound(id));
        String isbn = IsbnUtils.normalize(request.isbn());
        if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ISBN, "ISBN is already registered");
        }

        // Future-proof for loans: changing total inventory preserves currently borrowed copies.
        int borrowedCopies = book.getTotalQuantity() - book.getAvailableQuantity();
        if (request.totalQuantity() < borrowedCopies) {
            throw new BusinessException(ErrorCode.INVENTORY_BELOW_BORROWED_COUNT,
                    "Total quantity cannot be lower than currently borrowed copies: " + borrowedCopies);
        }
        applyRequest(book, request, false);
        book.setAvailableQuantity(request.totalQuantity() - borrowedCopies);
        return mapper.toResponse(book);
    }

    /** Soft delete is idempotent; loan history and FK references remain intact. */
    @Transactional
    public void softDelete(UUID id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> notFound(id));
        if (book.isActive()) {
            book.setActive(false);
            book.setDeletedAt(Instant.now());
        }
    }

    @Transactional(readOnly = true)
    public BookResponse getPublic(UUID id) {
        return mapper.toResponse(bookRepository.findActiveDetailedById(id)
                .orElseThrow(() -> notFound(id)));
    }

    @Transactional(readOnly = true)
    public BookResponse getAdmin(UUID id) {
        return mapper.toResponse(bookRepository.findDetailedById(id)
                .orElseThrow(() -> notFound(id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> searchPublic(BookSearchCriteria criteria, int page, int size, String sort) {
        return search(criteria, page, size, sort, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> searchAdmin(BookSearchCriteria criteria, int page, int size, String sort) {
        return search(criteria, page, size, sort, false);
    }

    private PageResponse<BookResponse> search(BookSearchCriteria criteria, int page, int size,
                                              String sort, boolean forceActive) {
        Page<Book> result = bookRepository.findAll(
                BookSpecifications.from(criteria, forceActive),
                pageRequestFactory.create(page, size, sort));
        // Mapping occurs inside the transaction. @BatchSize turns lazy authors/categories into
        // two bounded batch queries instead of up to 2N queries for a page.
        return PageResponse.from(result, mapper::toResponse);
    }

    private void applyRequest(Book book, BookRequest request, boolean creating) {
        book.setTitle(request.title().trim());
        book.setIsbn(IsbnUtils.normalize(request.isbn()));
        book.setDescription(blankToNull(request.description()));
        book.setPublicationYear(request.publicationYear());
        book.setTotalQuantity(request.totalQuantity());
        if (creating) {
            book.setAvailableQuantity(request.totalQuantity());
        }
        book.setAuthors(relationResolver.resolveAuthors(request.authors()));
        book.setCategories(relationResolver.resolveCategories(request.categories()));

        boolean active = request.active() == null ? (creating || book.isActive()) : request.active();
        book.setActive(active);
        book.setDeletedAt(active ? null : (book.getDeletedAt() == null ? Instant.now() : book.getDeletedAt()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessException notFound(UUID id) {
        return new BusinessException(ErrorCode.BOOK_NOT_FOUND, "Book not found: " + id);
    }
}
