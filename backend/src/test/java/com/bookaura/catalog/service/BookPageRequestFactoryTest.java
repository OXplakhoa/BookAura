package com.bookaura.catalog.service;

import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.*;

class BookPageRequestFactoryTest {

    private final BookPageRequestFactory factory = new BookPageRequestFactory();

    @Test
    void parsesMultipleAllowlistedSortsInOrder() {
        Pageable pageable = factory.create(2, 10, "publicationYear:desc,title:asc");
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getSort().toList())
                .extracting(order -> order.getProperty() + ":" + order.getDirection().name())
                .containsExactly("publicationYear:DESC", "title:ASC");
    }

    @Test
    void rejectsPageSizeAboveTen() {
        assertThatThrownBy(() -> factory.create(0, 11, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.PAGE_SIZE_EXCEEDED);
    }

    @Test
    void rejectsUnknownOrDuplicateSortFields() {
        assertThatThrownBy(() -> factory.create(0, 10, "passwordHash:asc"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.INVALID_SORT);
        assertThatThrownBy(() -> factory.create(0, 10, "title:asc,title:desc"))
                .isInstanceOf(BusinessException.class);
    }
}
