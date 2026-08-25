package com.bookaura.catalog.service;

import com.bookaura.catalog.dto.BookResponse;
import com.bookaura.catalog.entity.Author;
import com.bookaura.catalog.entity.Book;
import com.bookaura.catalog.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(), book.getTitle(), book.getIsbn(), book.getDescription(), book.getPublicationYear(),
                book.getTotalQuantity(), book.getAvailableQuantity(), book.isActive(), book.getDeletedAt(),
                book.getAuthors().stream().map(Author::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                book.getCategories().stream().map(Category::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                book.getPageCount(),
                book.getTags().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                book.getCreatedAt(), book.getUpdatedAt());
    }
}
