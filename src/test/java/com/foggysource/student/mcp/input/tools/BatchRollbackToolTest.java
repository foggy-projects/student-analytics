package com.foggysource.student.mcp.input.tools;

import com.foggysource.student.mcp.input.dto.InputToolResponse;
import com.foggysource.student.repository.BatchLogRepository;
import com.foggysource.student.repository.InputDataRepository;
import com.foggysource.student.repository.UpdateSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchRollbackToolTest {

    @Mock private BatchLogRepository batchRepo;
    @Mock private InputDataRepository dataRepo;
    @Mock private UpdateSnapshotRepository snapshotRepo;
    @Mock private JdbcTemplate jdbc;

    private BatchRollbackTool tool;

    @BeforeEach
    void setUp() {
        tool = new BatchRollbackTool(batchRepo, dataRepo, snapshotRepo, jdbc);
    }

    @Test
    void testBatchNotFound() {
        when(batchRepo.findById(999L)).thenReturn(null);

        Map<String, Object> args = new HashMap<>();
        args.put("batch_id", 999);

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("not_found");
    }

    @Test
    void testAlreadyRolledBack() {
        when(batchRepo.findById(42L)).thenReturn(Map.of(
                "batch_id", 42L, "status", "rolled_back", "batch_type", "score_create"));

        Map<String, Object> args = new HashMap<>();
        args.put("batch_id", 42);

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("已回撤");
    }

    @Test
    void testRollbackScoreCreate() {
        when(batchRepo.findById(42L)).thenReturn(Map.of(
                "batch_id", 42L, "status", "committed", "batch_type", "score_create",
                "summary", "期中考试 | 数学"));
        when(dataRepo.countScoresByBatchId(42L)).thenReturn(38);

        Map<String, Object> args = new HashMap<>();
        args.put("batch_id", 42);

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("success");
        assertThat(resp.getMessage()).contains("38");

        verify(dataRepo).deleteScoresByBatchId(42L);
        verify(batchRepo).markRolledBack(42L);
    }

    @Test
    void testRollbackScoreUpdate() {
        when(batchRepo.findById(43L)).thenReturn(Map.of(
                "batch_id", 43L, "status", "committed", "batch_type", "score_update",
                "summary", "修改成绩"));
        when(snapshotRepo.findByBatchId(43L)).thenReturn(List.of(
                Map.of("record_id", 100L,
                        "old_values", "{\"score\":85,\"score_level\":\"B\"}",
                        "new_values", "{\"score\":95,\"score_level\":\"A\"}")
        ));

        Map<String, Object> args = new HashMap<>();
        args.put("batch_id", 43);

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("success");

        verify(dataRepo).updateScore(eq(100L), any(), eq("B"), isNull());
        verify(batchRepo).markRolledBack(43L);
    }

    @Test
    void testRollbackExamWithScoresBlocked() {
        when(batchRepo.findById(44L)).thenReturn(Map.of(
                "batch_id", 44L, "status", "committed", "batch_type", "exam_create",
                "summary", "创建考试"));
        when(jdbc.queryForList(anyString(), eq(44L)))
                .thenReturn(List.of(Map.of("exam_id", 5L)));
        when(dataRepo.examHasScores(5L)).thenReturn(true);

        Map<String, Object> args = new HashMap<>();
        args.put("batch_id", 44);

        Object result = tool.execute(args);
        InputToolResponse resp = (InputToolResponse) result;
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).contains("关联成绩");
    }
}
