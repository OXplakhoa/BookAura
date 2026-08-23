package com.bookaura.loan;

import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.loan.service.LoanPageRequestFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LoanPageRequestFactoryTest {

    private final LoanPageRequestFactory factory = new LoanPageRequestFactory();

    @Test
    void parsesAllowlistedMultiSort() {
        assertThat(factory.create(0, 10, "dueAt:asc,borrowedAt:desc").getSort().toList())
                .extracting(order -> order.getProperty() + ":" + order.getDirection())
                .containsExactly("dueAt:ASC", "borrowedAt:DESC");
    }

    @Test
    void rejectsOversizedPageAndUnknownSort() {
        assertThatThrownBy(() -> factory.create(0, 11, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.PAGE_SIZE_EXCEEDED);
        assertThatThrownBy(() -> factory.create(0, 10, "member.password:asc"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.INVALID_SORT);
    }
}
