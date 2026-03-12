package com.foggysource.student.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 统一的数据录入 Repository，覆盖所有维度表和事实表的读写。
 */
@Repository
public class InputDataRepository {

    private final JdbcTemplate jdbc;

    public InputDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ========== dim_student ==========

    public Map<String, Object> findStudentByNo(String studentNo) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT s.*, c.class_name, g.grade_name FROM dim_student s " +
                        "JOIN dim_class c ON s.class_id = c.class_id " +
                        "JOIN dim_grade g ON c.grade_id = g.grade_id " +
                        "WHERE s.student_no = ?", studentNo);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Map<String, Object>> findStudentsByName(String studentName) {
        return jdbc.queryForList(
                "SELECT s.*, c.class_name, g.grade_name FROM dim_student s " +
                        "JOIN dim_class c ON s.class_id = c.class_id " +
                        "JOIN dim_grade g ON c.grade_id = g.grade_id " +
                        "WHERE s.student_name = ?", studentName);
    }

    public List<Map<String, Object>> findStudentsByNos(List<String> studentNos) {
        String placeholders = String.join(",", studentNos.stream().map(s -> "?").toList());
        return jdbc.queryForList(
                "SELECT s.*, c.class_name FROM dim_student s " +
                        "JOIN dim_class c ON s.class_id = c.class_id " +
                        "WHERE s.student_no IN (" + placeholders + ")",
                studentNos.toArray());
    }

    public Long insertStudent(String studentNo, String studentName, String gender,
                              Date birthDate, Long classId, String phone, Long batchId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO dim_student (student_no, student_name, gender, birth_date, class_id, phone, batch_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, studentNo);
            ps.setString(2, studentName);
            ps.setString(3, gender);
            ps.setDate(4, birthDate);
            ps.setLong(5, classId);
            ps.setString(6, phone);
            if (batchId != null) ps.setLong(7, batchId); else ps.setNull(7, java.sql.Types.BIGINT);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public String findMaxStudentNoByClassId(Long classId) {
        List<String> list = jdbc.queryForList(
                "SELECT student_no FROM dim_student WHERE class_id = ? ORDER BY student_no DESC LIMIT 1",
                String.class, classId);
        return list.isEmpty() ? null : list.get(0);
    }

    public void deleteStudentsByBatchId(Long batchId) {
        jdbc.update("DELETE FROM dim_student WHERE batch_id = ?", batchId);
    }

    // ========== dim_subject ==========

    public Map<String, Object> findSubjectByName(String subjectName) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM dim_subject WHERE subject_name = ?", subjectName);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Map<String, Object>> findAllSubjects() {
        return jdbc.queryForList("SELECT subject_name FROM dim_subject ORDER BY sort_order");
    }

    // ========== dim_exam ==========

    public Map<String, Object> findExamByName(String examName) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM dim_exam WHERE exam_name = ?", examName);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Map<String, Object>> findExamsByNameLike(String keyword) {
        return jdbc.queryForList(
                "SELECT exam_name FROM dim_exam WHERE exam_name LIKE ? ORDER BY exam_date DESC LIMIT 5",
                "%" + keyword + "%");
    }

    public Long insertExam(String examName, String examType, Date examDate, Long semesterId, Long batchId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO dim_exam (exam_name, exam_type, exam_date, semester_id, batch_id) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, examName);
            ps.setString(2, examType);
            ps.setDate(3, examDate);
            ps.setLong(4, semesterId);
            if (batchId != null) ps.setLong(5, batchId); else ps.setNull(5, java.sql.Types.BIGINT);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public boolean examHasScores(Long examId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM fact_score WHERE exam_id = ? LIMIT 1", Integer.class, examId);
        return count != null && count > 0;
    }

    public void deleteExamsByBatchId(Long batchId) {
        jdbc.update("DELETE FROM dim_exam WHERE batch_id = ?", batchId);
    }

    // ========== dim_semester ==========

    public Map<String, Object> findSemesterByDate(Date date) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM dim_semester WHERE start_date <= ? AND end_date >= ?", date, date);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findCurrentSemester() {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM dim_semester WHERE is_current = TRUE LIMIT 1");
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findSemesterByName(String name) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM dim_semester WHERE semester_name LIKE ?", "%" + name + "%");
        return list.isEmpty() ? null : list.get(0);
    }

    // ========== dim_class ==========

    public List<Map<String, Object>> findAllClasses() {
        return jdbc.queryForList(
                "SELECT c.*, g.grade_name, g.grade_level FROM dim_class c " +
                        "JOIN dim_grade g ON c.grade_id = g.grade_id ORDER BY g.grade_level, c.class_name");
    }

    public void updateClassStudentCount(Long classId, int delta) {
        jdbc.update("UPDATE dim_class SET student_count = COALESCE(student_count, 0) + ? WHERE class_id = ?",
                delta, classId);
    }

    // ========== dim_date ==========

    public boolean dateExists(String dateId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dim_date WHERE date_id = ?", Integer.class, dateId);
        return count != null && count > 0;
    }

    // ========== fact_score ==========

    public Map<String, Object> findScore(Long studentId, Long subjectId, Long examId) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM fact_score WHERE student_id = ? AND subject_id = ? AND exam_id = ?",
                studentId, subjectId, examId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Map<String, Object>> findScoresByStudentAndSubject(Long studentId, Long subjectId) {
        return jdbc.queryForList(
                "SELECT fs.*, e.exam_name, e.exam_date FROM fact_score fs " +
                        "JOIN dim_exam e ON fs.exam_id = e.exam_id " +
                        "WHERE fs.student_id = ? AND fs.subject_id = ? ORDER BY e.exam_date DESC",
                studentId, subjectId);
    }

    public void insertScore(Long studentId, Long classId, Long subjectId, Long examId,
                            BigDecimal score, String scoreLevel, Long batchId) {
        jdbc.update("INSERT INTO fact_score (student_id, class_id, subject_id, exam_id, score, score_level, batch_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                studentId, classId, subjectId, examId, score, scoreLevel, batchId);
    }

    public void updateScore(Long scoreId, BigDecimal score, String scoreLevel, Long batchId) {
        jdbc.update("UPDATE fact_score SET score = ?, score_level = ?, batch_id = ? WHERE score_id = ?",
                score, scoreLevel, batchId, scoreId);
    }

    public void deleteScoresByBatchId(Long batchId) {
        jdbc.update("DELETE FROM fact_score WHERE batch_id = ?", batchId);
    }

    public int countScoresByBatchId(Long batchId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM fact_score WHERE batch_id = ?", Integer.class, batchId);
        return count != null ? count : 0;
    }

    // ========== fact_attendance ==========

    public Map<String, Object> findAttendance(Long studentId, String dateId, String timeSlot) {
        String sql = timeSlot != null
                ? "SELECT * FROM fact_attendance WHERE student_id = ? AND date_id = ? AND time_slot = ?"
                : "SELECT * FROM fact_attendance WHERE student_id = ? AND date_id = ? AND time_slot IS NULL";
        List<Map<String, Object>> list = timeSlot != null
                ? jdbc.queryForList(sql, studentId, dateId, timeSlot)
                : jdbc.queryForList(sql, studentId, dateId);
        return list.isEmpty() ? null : list.get(0);
    }

    public void insertAttendance(Long studentId, Long classId, String dateId, String status,
                                 String timeSlot, String reason, Long batchId) {
        jdbc.update("INSERT INTO fact_attendance (student_id, class_id, date_id, status, time_slot, reason, batch_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                studentId, classId, dateId, status, timeSlot, reason, batchId);
    }

    public void deleteAttendanceByBatchId(Long batchId) {
        jdbc.update("DELETE FROM fact_attendance WHERE batch_id = ?", batchId);
    }
}
