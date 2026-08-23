package com.bookaura.catalog.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IsbnUtilsTest {

    @Test
    void validatesAndNormalizesKnownIsbns() {
        assertThat(IsbnUtils.isValid("978-0-13-235088-4")).isTrue();
        assertThat(IsbnUtils.normalize("978-0-13-235088-4")).isEqualTo("9780132350884");
        assertThat(IsbnUtils.isValid("0-13-235088-2")).isTrue();
        assertThat(IsbnUtils.normalize("0-13-235088-2")).isEqualTo("0132350882");
    }

    @Test
    void rejectsInvalidChecksumAndShape() {
        assertThat(IsbnUtils.isValid("9780132350885")).isFalse();
        assertThat(IsbnUtils.isValid("not-isbn")).isFalse();
        assertThat(IsbnUtils.isValid(null)).isFalse();
    }
}
