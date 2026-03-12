package com.foggysource.student.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 班级名称模糊匹配器。
 * 支持：「初一1班」→「初一(1)班」、「七年级一班」→「初一(1)班」 等
 */
@Component
public class ClassNameMatcher {

    /**
     * 从班级列表中匹配最佳结果。
     *
     * @param input 用户输入的班级名称
     * @param classes 数据库中的班级列表，每行包含 class_id, class_name, grade_name
     * @return 匹配的班级记录，未匹配返回 null
     */
    public Map<String, Object> match(String input, List<Map<String, Object>> classes) {
        if (input == null || input.isBlank()) return null;

        String normalized = normalize(input);

        // 精确匹配
        for (Map<String, Object> cls : classes) {
            if (normalize((String) cls.get("class_name")).equals(normalized)) {
                return cls;
            }
        }

        // 包含匹配
        for (Map<String, Object> cls : classes) {
            String clsNorm = normalize((String) cls.get("class_name"));
            if (clsNorm.contains(normalized) || normalized.contains(clsNorm)) {
                return cls;
            }
        }

        return null;
    }

    private String normalize(String name) {
        if (name == null) return "";
        return name
                .replaceAll("[（(]", "(")
                .replaceAll("[）)]", ")")
                .replaceAll("\\s+", "")
                .replace("七年级", "初一")
                .replace("八年级", "初二")
                .replace("九年级", "初三")
                .replace("一班", "(1)班")
                .replace("二班", "(2)班")
                .replace("三班", "(3)班")
                .replace("四班", "(4)班")
                .replace("1班", "(1)班")
                .replace("2班", "(2)班")
                .replace("3班", "(3)班")
                .replace("4班", "(4)班");
    }
}
