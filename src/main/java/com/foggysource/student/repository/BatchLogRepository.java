package com.foggysource.student.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class BatchLogRepository {

    private final JdbcTemplate jdbc;

    public BatchLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long createBatch(String batchType, String targetTable, int recordCount, String summary) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO sys_batch_log (batch_type, target_table, record_count, summary) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, batchType);
            ps.setString(2, targetTable);
            ps.setInt(3, recordCount);
            ps.setString(4, summary);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Map<String, Object> findById(Long batchId) {
        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT * FROM sys_batch_log WHERE batch_id = ?", batchId);
        return results.isEmpty() ? null : results.get(0);
    }

    public void markRolledBack(Long batchId) {
        jdbc.update("UPDATE sys_batch_log SET status = 'rolled_back', rolled_back_at = ? WHERE batch_id = ?",
                Timestamp.valueOf(LocalDateTime.now()), batchId);
    }

    public List<Map<String, Object>> listRecent(int limit, String status) {
        if (status != null) {
            return jdbc.queryForList(
                    "SELECT * FROM sys_batch_log WHERE status = ? ORDER BY created_at DESC LIMIT ?",
                    status, limit);
        }
        return jdbc.queryForList(
                "SELECT * FROM sys_batch_log ORDER BY created_at DESC LIMIT ?", limit);
    }
}
