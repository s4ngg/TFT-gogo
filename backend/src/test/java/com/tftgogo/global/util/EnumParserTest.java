package com.tftgogo.global.util;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumParserTest {

    @Test
    void enum_문자열은_trim과_대문자_변환_후_파싱한다() {
        // when
        TestType result = EnumParser.from(TestType.class, " alpha ");

        // then
        assertThat(result).isEqualTo(TestType.ALPHA);
    }

    @Test
    void null이나_blank는_INVALID_INPUT_예외를_던진다() {
        // when, then
        assertThatThrownBy(() -> EnumParser.from(TestType.class, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThatThrownBy(() -> EnumParser.from(TestType.class, " "))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private enum TestType {
        ALPHA
    }
}
