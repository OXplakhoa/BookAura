package com.bookaura.catalog.dto;

import com.bookaura.catalog.validation.ValidIsbn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record BookRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @ValidIsbn String isbn,
        @Size(max = 4000) String description,
        @NotNull @Min(1450) @Max(2100) Integer publicationYear,
        @NotNull @Min(0) @Max(100_000) Integer totalQuantity,
        @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 120) String> authors,
        @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 80) String> categories,
        /** null means true on create and unchanged on update. */
        Boolean active
) {
}
