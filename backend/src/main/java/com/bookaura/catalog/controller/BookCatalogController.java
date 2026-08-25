package com.bookaura.catalog.controller;

import com.bookaura.catalog.dto.BookResponse;
import com.bookaura.catalog.dto.BookSearchCriteria;
import com.bookaura.catalog.repository.CategoryRepository;
import com.bookaura.catalog.service.BookService;
import com.bookaura.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Public catalog", description = "Read/search active books; authentication not required")
@RestController
@RequestMapping("/api/books")
public class BookCatalogController {

    private final BookService bookService;
    private final CategoryRepository categoryRepository;

    public BookCatalogController(BookService bookService, CategoryRepository categoryRepository) {
        this.bookService = bookService;
        this.categoryRepository = categoryRepository;
    }

    @Operation(summary = "Search active books",
            description = "All filters are AND-composed in SQL using Specification. " +
                    "Sort format: title:asc,publicationYear:desc. Max page size: 10.")
    @SecurityRequirements
    @GetMapping
    public PageResponse<BookResponse> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Integer publicationYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return bookService.searchPublic(
                new BookSearchCriteria(title, isbn, author, category, available, publicationYear, true),
                page, size, sort);
    }

    @Operation(summary = "List all category names (theme chips for catalog filters and Shelf Aura)")
    @SecurityRequirements
    @GetMapping("/categories")
    public List<String> categories() {
        return categoryRepository.findAllNames();
    }

    @Operation(summary = "Read active book detail")
    @SecurityRequirements
    @GetMapping("/{id}")
    public BookResponse detail(@PathVariable UUID id) {
        return bookService.getPublic(id);
    }
}
