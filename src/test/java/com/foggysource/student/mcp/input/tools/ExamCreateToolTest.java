package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamCreateToolTest {

    @Mock private InputDataRepository dataRepo;
    @Mock private BatchLogRepository batchRepo;

    private ExamCreateTool tool;

    @BeforeEach
    void setUp() {
        tool = new ExamCreateTool(dataRepo, batchRepo);
    }

    @Test
    void testSuccessfulCreate() {
        when(dataRepo.findExamByName("第三次月考")).thenReturn(null);
        when(dataRepo.findSemesterByDate(any(Date.class)))
                .thenReturn(Map.of("semester_id", 1L, "semester_name", "2025-2026第一学期"));
        when(batchRepo.createBatch(anyString(), anyString(), anyInt(), anyString())).thenReturn(10L);
        when(dataRepo.insertExam(anyString(), anyString(), any(Date.class), anyLong(), anyLong())).thenReturn(5L);

        Map<String, Object> args = new HashMap<>();
        args.put("exam_name", "第三次月考");
        args.put("exam_type", "unit");
        args.put("exam_date", "2026-03-15");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("success");
        assertThat(resp.getBatchId()).isEqualTo(10L);
        assertThat(resp.getMessage()).contains("第三次月考");
    }

    @Test
    void testDuplicateExam() {
        when(dataRepo.findExamByName("期中考试")).thenReturn(Map.of("exam_id", 1L));

        Map<String, Object> args = new HashMap<>();
        args.put("exam_name", "期中考试");
        args.put("exam_type", "midterm");
        args.put("exam_date", "2026-03-15");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("已存在");
    }

    @Test
    void testInvalidExamType() {
        when(dataRepo.findExamByName("测试")).thenReturn(null);

        Map<String, Object> args = new HashMap<>();
        args.put("exam_name", "测试");
        args.put("exam_type", "invalid_type");
        args.put("exam_date", "2026-03-15");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("无效");
    }

    @Test
    void testInvalidDateFormat() {
        when(dataRepo.findExamByName("测试")).thenReturn(null);

        Map<String, Object> args = new HashMap<>();
        args.put("exam_name", "测试");
        args.put("exam_type", "daily");
        args.put("exam_date", "2026/03/15");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("日期格式");
    }

    @Test
    void testSemesterAutoMatch() {
        when(dataRepo.findExamByName("月考")).thenReturn(null);
        // 日期匹配不到学期，回退到当前学期
        when(dataRepo.findSemesterByDate(any(Date.class))).thenReturn(null);
        when(dataRepo.findCurrentSemester())
                .thenReturn(Map.of("semester_id", 2L, "semester_name", "当前学期"));
        when(batchRepo.createBatch(anyString(), anyString(), anyInt(), anyString())).thenReturn(11L);
        when(dataRepo.insertExam(anyString(), anyString(), any(Date.class), anyLong(), anyLong())).thenReturn(6L);

        Map<String, Object> args = new HashMap<>();
        args.put("exam_name", "月考");
        args.put("exam_type", "unit");
        args.put("exam_date", "2026-03-15");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("success");
        assertThat(resp.getMessage()).contains("当前学期");
    }
}
