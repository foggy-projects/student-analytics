package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import com.foggysource.student.service.ScoreLevelCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreCreateToolTest {

    @Mock private InputDataRepository dataRepo;
    @Mock private BatchLogRepository batchRepo;

    private ScoreCreateTool tool;

    @BeforeEach
    void setUp() {
        tool = new ScoreCreateTool(dataRepo, batchRepo, new ScoreLevelCalculator());
    }

    @Test
    void testSuccessfulCreate() {
        Map<String, Object> student = Map.of("student_id", 1L, "student_name", "张三",
                "student_no", "070101", "class_id", 1L);
        Map<String, Object> subject = Map.of("subject_id", 1L, "subject_name", "数学",
                "full_score", 120, "pass_score", 72, "excellent_score", 102);
        Map<String, Object> exam = Map.of("exam_id", 1L, "exam_name", "期中考试");

        when(dataRepo.findStudentByNo("070101")).thenReturn(student);
        when(dataRepo.findSubjectByName("数学")).thenReturn(subject);
        when(dataRepo.findExamByName("期中考试")).thenReturn(exam);
        when(dataRepo.findScore(1L, 1L, 1L)).thenReturn(null);
        when(batchRepo.createBatch(anyString(), anyString(), anyInt(), anyString())).thenReturn(42L);

        Map<String, Object> args = new HashMap<>();
        args.put("student_no", "070101");
        args.put("subject_name", "数学");
        args.put("exam_name", "期中考试");
        args.put("score", 95);

        Object result = tool.execute(args);
        assertThat(result).isInstanceOf(InputToolResponse.class);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("success");
        assertThat(resp.getBatchId()).isEqualTo(42L);
        assertThat(resp.getMessage()).contains("070101");

        verify(dataRepo).insertScore(eq(1L), eq(1L), eq(1L), eq(1L),
                eq(new BigDecimal("95")), eq("B"), eq(42L));
    }

    @Test
    void testStudentNotFound() {
        when(dataRepo.findStudentByNo("999999")).thenReturn(null);

        Map<String, Object> args = new HashMap<>();
        args.put("student_no", "999999");
        args.put("subject_name", "数学");
        args.put("exam_name", "期中考试");
        args.put("score", 95);

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("not_found");
    }

    @Test
    void testScoreExceedsFullScore() {
        Map<String, Object> student = Map.of("student_id", 1L, "student_name", "张三",
                "student_no", "070101", "class_id", 1L);
        Map<String, Object> subject = Map.of("subject_id", 1L, "subject_name", "数学",
                "full_score", 120, "pass_score", 72, "excellent_score", 102);

        when(dataRepo.findStudentByNo("070101")).thenReturn(student);
        when(dataRepo.findSubjectByName("数学")).thenReturn(subject);

        Map<String, Object> args = new HashMap<>();
        args.put("student_no", "070101");
        args.put("subject_name", "数学");
        args.put("exam_name", "期中考试");
        args.put("score", 150);

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("超过满分");
    }

    @Test
    void testDuplicateScore() {
        Map<String, Object> student = Map.of("student_id", 1L, "student_name", "张三",
                "student_no", "070101", "class_id", 1L);
        Map<String, Object> subject = Map.of("subject_id", 1L, "subject_name", "数学",
                "full_score", 120, "pass_score", 72, "excellent_score", 102);
        Map<String, Object> exam = Map.of("exam_id", 1L, "exam_name", "期中考试");

        when(dataRepo.findStudentByNo("070101")).thenReturn(student);
        when(dataRepo.findSubjectByName("数学")).thenReturn(subject);
        when(dataRepo.findExamByName("期中考试")).thenReturn(exam);
        when(dataRepo.findScore(1L, 1L, 1L)).thenReturn(Map.of("score_id", 99L));

        Map<String, Object> args = new HashMap<>();
        args.put("student_no", "070101");
        args.put("subject_name", "数学");
        args.put("exam_name", "期中考试");
        args.put("score", 95);

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("已存在");
    }

    @Test
    void testAutoLevelCalculation() {
        Map<String, Object> student = Map.of("student_id", 1L, "student_name", "张三",
                "student_no", "070101", "class_id", 1L);
        Map<String, Object> subject = Map.of("subject_id", 1L, "subject_name", "数学",
                "full_score", 120, "pass_score", 72, "excellent_score", 102);
        Map<String, Object> exam = Map.of("exam_id", 1L, "exam_name", "期中考试");

        when(dataRepo.findStudentByNo("070101")).thenReturn(student);
        when(dataRepo.findSubjectByName("数学")).thenReturn(subject);
        when(dataRepo.findExamByName("期中考试")).thenReturn(exam);
        when(dataRepo.findScore(1L, 1L, 1L)).thenReturn(null);
        when(batchRepo.createBatch(anyString(), anyString(), anyInt(), anyString())).thenReturn(1L);

        // Score 110 >= excellent(102) → A
        Map<String, Object> args = new HashMap<>();
        args.put("student_no", "070101");
        args.put("subject_name", "数学");
        args.put("exam_name", "期中考试");
        args.put("score", 110);

        tool.execute(args);

        verify(dataRepo).insertScore(eq(1L), eq(1L), eq(1L), eq(1L),
                eq(new BigDecimal("110")), eq("A"), eq(1L));
    }
}
