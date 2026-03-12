package com.foggysource.student.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClassNameMatcherTest {

    private final ClassNameMatcher matcher = new ClassNameMatcher();

    private final List<Map<String, Object>> classes = List.of(
            Map.of("class_id", 1L, "class_name", "初一(1)班"),
            Map.of("class_id", 2L, "class_name", "初一(2)班"),
            Map.of("class_id", 3L, "class_name", "初二(1)班"),
            Map.of("class_id", 4L, "class_name", "初三(3)班")
    );

    @Test
    void testExactMatch() {
        Map<String, Object> result = matcher.match("初一(1)班", classes);
        assertThat(result).isNotNull();
        assertThat(result.get("class_id")).isEqualTo(1L);
    }

    @Test
    void testFullWidthParentheses() {
        Map<String, Object> result = matcher.match("初一（1）班", classes);
        assertThat(result).isNotNull();
        assertThat(result.get("class_id")).isEqualTo(1L);
    }

    @Test
    void testGradeNameConversion() {
        // 七年级 → 初一
        Map<String, Object> result = matcher.match("七年级(2)班", classes);
        assertThat(result).isNotNull();
        assertThat(result.get("class_id")).isEqualTo(2L);
    }

    @Test
    void testChineseNumberConversion() {
        // 一班 → (1)班
        Map<String, Object> result = matcher.match("初一一班", classes);
        assertThat(result).isNotNull();
        assertThat(result.get("class_id")).isEqualTo(1L);
    }

    @Test
    void testNumberWithoutParentheses() {
        // 1班 → (1)班
        Map<String, Object> result = matcher.match("初二1班", classes);
        assertThat(result).isNotNull();
        assertThat(result.get("class_id")).isEqualTo(3L);
    }

    @Test
    void testNoMatch() {
        Map<String, Object> result = matcher.match("高一(1)班", classes);
        assertThat(result).isNull();
    }

    @Test
    void testNullInput() {
        assertThat(matcher.match(null, classes)).isNull();
        assertThat(matcher.match("", classes)).isNull();
    }
}
