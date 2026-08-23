package com.bookaura.catalog.specification;

import com.bookaura.catalog.dto.BookSearchCriteria;
import com.bookaura.catalog.entity.Author;
import com.bookaura.catalog.entity.Book;
import com.bookaura.catalog.entity.Category;
import com.bookaura.catalog.validation.IsbnUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Small reusable specifications composed with AND. Every filter runs in SQL;
 * no findAll + Java-side filtering.
 */
public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> from(BookSearchCriteria c, boolean forceActive) {
        Specification<Book> spec = Specification.unrestricted();
        if (hasText(c.title())) spec = spec.and(contains("title", c.title()));
        if (hasText(c.isbn())) spec = spec.and(isbnContains(c.isbn()));
        if (hasText(c.author())) spec = spec.and(authorContains(c.author()));
        if (hasText(c.category())) spec = spec.and(categoryContains(c.category()));
        if (c.available() != null) spec = spec.and(availability(c.available()));
        if (c.publicationYear() != null) spec = spec.and(publicationYear(c.publicationYear()));
        if (forceActive) {
            spec = spec.and(active(true));
        } else if (c.active() != null) {
            spec = spec.and(active(c.active()));
        }
        return spec;
    }

    private static Specification<Book> contains(String attribute, String value) {
        String pattern = "%" + escapeLike(value.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get(attribute)), pattern, '\\');
    }

    private static Specification<Book> isbnContains(String value) {
        String normalized = IsbnUtils.normalize(value);
        return (root, query, cb) -> cb.like(root.get("isbn"), "%" + normalized + "%");
    }

    private static Specification<Book> authorContains(String value) {
        String pattern = "%" + escapeLike(value.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            Join<Book, Author> join = root.join("authors", JoinType.INNER);
            query.distinct(true);
            return cb.like(cb.lower(join.get("name")), pattern, '\\');
        };
    }

    private static Specification<Book> categoryContains(String value) {
        String pattern = "%" + escapeLike(value.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            Join<Book, Category> join = root.join("categories", JoinType.INNER);
            query.distinct(true);
            return cb.like(cb.lower(join.get("name")), pattern, '\\');
        };
    }

    private static Specification<Book> availability(boolean available) {
        return (root, query, cb) -> available
                ? cb.greaterThan(root.get("availableQuantity"), 0)
                : cb.equal(root.get("availableQuantity"), 0);
    }

    private static Specification<Book> publicationYear(int year) {
        return (root, query, cb) -> cb.equal(root.get("publicationYear"), year);
    }

    private static Specification<Book> active(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
