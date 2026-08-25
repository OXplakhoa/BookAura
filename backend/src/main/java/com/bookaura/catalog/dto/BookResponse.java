package com.bookaura.catalog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String title,
        String isbn,
        String description,
        Integer publicationYear,
        int totalQuantity,
        int availableQuantity,
        boolean active,
        Instant deletedAt,
        List<String> authors,
        List<String> categories,
        Integer pageCount,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
}
