package com.iae.service;

import com.iae.db.DatabaseManager;
import com.iae.model.StudentResult;
import com.iae.model.ResultStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StudentResultService {

    private final DatabaseManager dbManager;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StudentResultService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public StudentResult save(StudentResult result) throws SQLException {
        String sql = "INSERT INTO student_results (project_id, student_id, zip_file_path, compile_status, compile_output, compile_error, run_status, program_output, error_output, test_status, test_details, processed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        if (result.getProcessedAt() == null) {
            result.setProcessedAt(LocalDateTime.now());
        }

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, result.getProjectId());
            stmt.setString(2, result.getStudentId());
            stmt.setString(3, result.getZipFilePath());
            stmt.setString(4, result.getCompileStatus() != null ? result.getCompileStatus().name() : ResultStatus.PENDING.name());
            stmt.setString(5, result.getCompileOutput());
            stmt.setString(6, result.getCompileError());
            stmt.setString(7, result.getRunStatus() != null ? result.getRunStatus().name() : null);
            stmt.setString(8, result.getProgramOutput());
            stmt.setString(9, result.getErrorOutput());
            stmt.setString(10, result.getTestStatus() != null ? result.getTestStatus().name() : null);
            stmt.setString(11, result.getTestDetails());
            stmt.setString(12, result.getProcessedAt().format(formatter));

            stmt.executeUpdate();

            try (Statement sqlStmt = dbManager.getConnection().createStatement();
                 ResultSet rs = sqlStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    result.setId(rs.getInt(1));
                }
            }
        }

        return result;
    }

    public List<StudentResult> findByProjectId(int projectId) throws SQLException {
        List<StudentResult> list = new ArrayList<>();

        String sql = "SELECT * FROM student_results WHERE project_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {

            stmt.setInt(1, projectId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }
        }

        return list;
    }

    public void deleteByProjectId(int projectId) throws SQLException {
        String sql = "DELETE FROM student_results WHERE project_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            stmt.executeUpdate();
        }
    }

    private StudentResult map(ResultSet rs) throws SQLException {
        StudentResult sr = new StudentResult();

        sr.setId(rs.getInt("id"));
        sr.setProjectId(rs.getInt("project_id"));
        sr.setStudentId(rs.getString("student_id"));
        sr.setZipFilePath(rs.getString("zip_file_path"));

        sr.setCompileStatus(ResultStatus.valueOf(rs.getString("compile_status")));
        sr.setCompileOutput(rs.getString("compile_output"));
        sr.setCompileError(rs.getString("compile_error"));

        String runStatus = rs.getString("run_status");
        if (runStatus != null) {
            sr.setRunStatus(ResultStatus.valueOf(runStatus));
        }

        sr.setProgramOutput(rs.getString("program_output"));
        sr.setErrorOutput(rs.getString("error_output"));

        String testStatus = rs.getString("test_status");
        if (testStatus != null) {
            sr.setTestStatus(ResultStatus.valueOf(testStatus));
        }

        sr.setTestDetails(rs.getString("test_details"));

        String date = rs.getString("processed_at");
        if (date != null) {
            sr.setProcessedAt(LocalDateTime.parse(date, formatter));
        }

        return sr;
    }
}