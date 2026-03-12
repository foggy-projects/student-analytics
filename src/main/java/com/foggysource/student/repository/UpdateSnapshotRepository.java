package com.foggysource.student.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class UpdateSnapshotRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UpdateSnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createSnapshot(Long batchId, String targetTable, Long recordId,
                               Map<String, Object> oldValues, Map<String, Object> newValues) {
        try {
            jdbc.update("INSERT INTO sys_update_snapshot (batch_id, target_table, record_id, old_values, new_values) VALUES (?, ?, ?, ?, ?)",
                    batchId, targetTable, recordId,
                    objectMapper.writeValueAsString(oldValues),
                    objectMapper.writeValueAsString(newValues));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize snapshot values", e);
        }
    }

    public List<Map<String, Object>> findByBatchId(Long batchId) {
        return jdbc.queryForList("SELECT * FROM sys_update_snapshot WHERE batch_id = ?", batchId);
    }
}
