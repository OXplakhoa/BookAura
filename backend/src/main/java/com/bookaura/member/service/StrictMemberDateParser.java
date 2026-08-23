package com.bookaura.member.service;

import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

@Component
public class StrictMemberDateParser {

    /** Mentor-requested format: four-digit year, two-digit month, one/two-digit day. */
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("uuuu/MM/d")
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    public LocalDate parseOptional(String raw, String field) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.INVALID_DATE_FORMAT,
                    field + " must use yyyy/MM/d and contain a real calendar date");
        }
    }

    public void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE,
                    "dateOfBirthFrom must be on or before dateOfBirthTo");
        }
    }
}
