package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
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
class AttendanceRecordToolTest {

    @Mock private InputDataRepository dataRepo;
    @Mock private BatchLogRepository batchRepo;

    private AttendanceRecordTool tool;

    @BeforeEach
    void setUp() {
        tool = new AttendanceRecordTool(dataRepo, batchRepo);
    }

    @Test
    void testSuccessfulRecord() {
        when(dataRepo.findStudentsByNos(List.of("090101", "090102"))).thenReturn(List.of(
                Map.of("student_id", 1L, "student_no", "090101", "student_name", "张三", "class_id", 1L),
                Map.of("student_id", 2L, "student_no", "090102", "student_name", "李四", "class_id", 1L)
        ));
        when(dataRepo.findAttendance(anyLong(), anyString(), any())).thenReturn(null);
        when(batchRepo.createBatch(anyString(), anyString(), anyInt(), anyString())).thenReturn(30L);

        Map<String, Object> args = new HashMap<>();
        args.put("student_nos", List.of("090101", "090102"));
        args.put("status", "absent");
        args.put("date", "2026-03-12");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("success");
        assertThat(resp.getSuccessCount()).isEqualTo(2);

        verify(dataRepo, times(2)).insertAttendance(anyLong(), anyLong(), eq("2026-03-12"),
                eq("absent"), isNull(), isNull(), eq(30L));
    }

    @Test
    void testInvalidStatus() {
        Map<String, Object> args = new HashMap<>();
        args.put("student_nos", List.of("090101"));
        args.put("status", "invalid");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("无效");
    }

    @Test
    void testEmptyStudentNos() {
        Map<String, Object> args = new HashMap<>();
        args.put("student_nos", List.of());
        args.put("status", "absent");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
    }

    @Test
    void testStudentNotFound() {
        when(dataRepo.findStudentsByNos(List.of("999999"))).thenReturn(List.of());
        when(batchRepo.createBatch(anyString(), anyString(), anyInt(), anyString())).thenReturn(31L);

        Map<String, Object> args = new HashMap<>();
        args.put("student_nos", List.of("999999"));
        args.put("status", "late");
        args.put("date", "2026-03-12");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getErrorCount()).isEqualTo(1);
    }

    @Test
    void testSkipDuplicate() {
        when(dataRepo.findStudentsByNos(List.of("090101"))).thenReturn(List.of(
                Map.of("student_id", 1L, "student_no", "090101", "student_name", "张三", "class_id", 1L)
        ));
        when(dataRepo.findAttendance(1L, "2026-03-12", null)).thenReturn(Map.of("attendance_id", 1L));
        when(batchRepo.createBatch(anyString(), anyString(), anyInt(), anyString())).thenReturn(32L);

        Map<String, Object> args = new HashMap<>();
        args.put("student_nos", List.of("090101"));
        args.put("status", "absent");
        args.put("date", "2026-03-12");

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getSkipCount()).isEqualTo(1);
        assertThat(resp.getSuccessCount()).isEqualTo(0);
    }
}
