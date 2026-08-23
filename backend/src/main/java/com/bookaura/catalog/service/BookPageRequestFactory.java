package com.bookaura.catalog.service;

import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;

/** Parses API sort contract: sort=title:asc,publicationYear:desc (max 5 fields). */
@Component
public class BookPageRequestFactory {

    private static final int MAX_PAGE_SIZE = 10;
    private static final int MAX_SORT_FIELDS = 5;
    private static final Map<String, String> ALLOWED_SORTS = Map.of(
            "title", "title",
            "isbn", "isbn",
            "publicationYear", "publicationYear",
            "availableQuantity", "availableQuantity",
            "createdAt", "createdAt"
    );

    public Pageable create(int page, int size, String rawSort) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_PAGE, "Page index must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PAGE_SIZE_EXCEEDED, "Page size must be between 1 and 10");
        }
        return PageRequest.of(page, size, parseSort(rawSort));
    }

    private Sort parseSort(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return Sort.by(Sort.Order.asc("title"));
        }
        String[] parts = rawSort.split(",");
        if (parts.length > MAX_SORT_FIELDS) {
            throw invalidSort("At most 5 sort fields are allowed");
        }
        List<Sort.Order> orders = new ArrayList<>();
        Set<String> seenFields = new HashSet<>();
        for (String part : parts) {
            String[] pair = part.trim().split(":", -1);
            if (pair.length != 2 || !ALLOWED_SORTS.containsKey(pair[0]) || !seenFields.add(pair[0])) {
                throw invalidSort("Sort must use unique allowlisted fields: " + ALLOWED_SORTS.keySet());
            }
            Sort.Direction direction;
            try {
                direction = Sort.Direction.fromString(pair[1]);
            } catch (IllegalArgumentException ex) {
                throw invalidSort("Sort direction must be asc or desc");
            }
            orders.add(new Sort.Order(direction, ALLOWED_SORTS.get(pair[0])));
        }
        return Sort.by(orders);
    }

    private BusinessException invalidSort(String message) {
        return new BusinessException(ErrorCode.INVALID_SORT, message);
    }
}
