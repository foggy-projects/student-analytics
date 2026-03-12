package com.foggysource.student.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 成绩等级自动计算器。
 * A: score >= excellent_score
 * B: score >= pass_score
 * C: score >= pass_score * 0.8
 * D: score < pass_score * 0.8
 */
@Component
public class ScoreLevelCalculator {

    public String calculate(BigDecimal score, int passScore, int excellentScore) {
        double s = score.doubleValue();
        if (s >= excellentScore) return "A";
        if (s >= passScore) return "B";
        if (s >= passScore * 0.8) return "C";
        return "D";
    }
}
