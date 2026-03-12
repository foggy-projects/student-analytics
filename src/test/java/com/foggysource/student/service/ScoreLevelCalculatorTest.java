package com.foggysource.student.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreLevelCalculatorTest {

    private final ScoreLevelCalculator calc = new ScoreLevelCalculator();

    // 语文：满分120，及格72，优秀102
    @ParameterizedTest
    @CsvSource({
            "120, 72, 102, A",   // 满分 >= excellent
            "102, 72, 102, A",   // 刚好优秀线
            "101, 72, 102, B",   // 差一分优秀
            "72,  72, 102, B",   // 刚好及格线
            "71,  72, 102, C",   // 差一分及格，但 >= pass*0.8=57.6
            "58,  72, 102, C",   // >= 57.6
            "57,  72, 102, D",   // < 57.6
            "0,   72, 102, D",   // 零分
    })
    void testCalculate(int score, int passScore, int excellentScore, String expected) {
        String result = calc.calculate(new BigDecimal(score), passScore, excellentScore);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testDecimalScore() {
        // 99.5 >= 90 (excellent) → A
        assertThat(calc.calculate(new BigDecimal("99.5"), 60, 90)).isEqualTo("A");
        // 59.9 < 60 (pass) but >= 48 (pass*0.8) → C
        assertThat(calc.calculate(new BigDecimal("59.9"), 60, 90)).isEqualTo("C");
    }
}
