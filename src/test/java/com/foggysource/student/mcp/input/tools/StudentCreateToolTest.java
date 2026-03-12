package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import com.foggysource.student.service.ClassNameMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCreateToolTest {

    @Mock private InputDataRepository dataRepo;
    @Mock private BatchLogRepository batchRepo;

    private StudentCreateTool tool;

    @BeforeEach
    void setUp() {
        tool = new StudentCreateTool(dataRepo, batchRepo, new ClassNameMatcher());
    }

    @Test
    void testSuccessfulCreate() {
        List<Map<String, Object>> classes = List.of(
                Map.of("class_id", 1L, "class_name", "初一(1)班"));
        when(dataRepo.findAllClasses()).thenReturn(classes);
        when(dataRepo.findMaxStudentNoByClassId(1L)).thenReturn("070138");
        when(dataRepo.findStudentByNo("070139")).thenReturn(null);
        when(batchRepo.createBatch(anyString(), anyString(), anyInt(), anyString())).thenReturn(20L);
        when(dataRepo.insertStudent(anyString(), anyString(), any(), any(), anyLong(), any(), anyLong())).thenReturn(100L);

        Map<String, Object> args = new HashMap<>();
        args.put("student_name", "李华");
        args.put("class_name", "初一(1)班");
        args.put("gender", "M");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("success");
        assertThat(resp.getMessage()).contains("李华");
        assertThat(resp.getMessage()).contains("070139");

        verify(dataRepo).updateClassStudentCount(1L, 1);
    }

    @Test
    void testClassNotFound() {
        List<Map<String, Object>> classes = List.of(
                Map.of("class_id", 1L, "class_name", "初一(1)班"));
        when(dataRepo.findAllClasses()).thenReturn(classes);

        Map<String, Object> args = new HashMap<>();
        args.put("student_name", "李华");
        args.put("class_name", "高一(1)班");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("未找到");
    }

    @Test
    void testFuzzyClassMatch() {
        List<Map<String, Object>> classes = List.of(
                Map.of("class_id", 1L, "class_name", "初一(1)班"));
        when(dataRepo.findAllClasses()).thenReturn(classes);
        when(dataRepo.findMaxStudentNoByClassId(1L)).thenReturn("070138");
        when(dataRepo.findStudentByNo("070139")).thenReturn(null);
        when(batchRepo.createBatch(anyString(), anyString(), anyInt(), anyString())).thenReturn(21L);
        when(dataRepo.insertStudent(anyString(), anyString(), any(), any(), anyLong(), any(), anyLong())).thenReturn(101L);

        Map<String, Object> args = new HashMap<>();
        args.put("student_name", "王明");
        args.put("class_name", "七年级一班"); // Should match 初一(1)班

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("success");
    }

    @Test
    void testDuplicateStudentNo() {
        List<Map<String, Object>> classes = List.of(
                Map.of("class_id", 1L, "class_name", "初一(1)班"));
        when(dataRepo.findAllClasses()).thenReturn(classes);

        Map<String, Object> args = new HashMap<>();
        args.put("student_name", "李华");
        args.put("class_name", "初一(1)班");
        args.put("student_no", "070101");

        when(dataRepo.findStudentByNo("070101")).thenReturn(Map.of("student_id", 1L));

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("已存在");
    }

    @Test
    void testInvalidGender() {
        List<Map<String, Object>> classes = List.of(
                Map.of("class_id", 1L, "class_name", "初一(1)班"));
        when(dataRepo.findAllClasses()).thenReturn(classes);
        when(dataRepo.findMaxStudentNoByClassId(1L)).thenReturn("070138");
        when(dataRepo.findStudentByNo("070139")).thenReturn(null);

        Map<String, Object> args = new HashMap<>();
        args.put("student_name", "李华");
        args.put("class_name", "初一(1)班");
        args.put("gender", "X");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("性别无效");
    }
}
