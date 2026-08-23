package com.bookaura.catalog.dto;

/** All non-null/non-blank fields are AND-composed by BookSpecifications. */
public record BookSearchCriteria(
        String title,
        String isbn,
        String author,
        String category,
        Boolean available,
        Integer publicationYear,
        Boolean active
) {
}
