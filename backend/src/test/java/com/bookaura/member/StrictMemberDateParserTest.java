package com.bookaura.member;

import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.member.service.StrictMemberDateParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class StrictMemberDateParserTest {

    private final StrictMemberDateParser parser = new StrictMemberDateParser();

    @Test
    void parsesMentorFormatStrictly() {
        assertThat(parser.parseOptional("1995/02/9", "from")).isEqualTo(LocalDate.of(1995, 2, 9));
        assertThat(parser.parseOptional(null, "from")).isNull();
    }

    @Test
    void rejectsWrongFormatImpossibleDateAndReverseRange() {
        assertCode(() -> parser.parseOptional("1995-02-09", "from"), ErrorCode.INVALID_DATE_FORMAT);
        assertCode(() -> parser.parseOptional("2024/02/30", "from"), ErrorCode.INVALID_DATE_FORMAT);
        assertCode(() -> parser.validateRange(LocalDate.of(2000, 1, 1), LocalDate.of(1999, 1, 1)),
                ErrorCode.INVALID_DATE_RANGE);
    }

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code()).isEqualTo(code);
    }
}
