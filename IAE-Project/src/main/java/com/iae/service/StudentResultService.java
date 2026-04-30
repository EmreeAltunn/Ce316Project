package com.iae.service;

import com.iae.database.DatabaseManager;
import com.iae.model.StudentResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// student result service
public class StudentResultService {

    public void createStudentResult(StudentResult result) {
        String sql = """
                INSERT INTO student_results (
                    project_id,
                    student_id,
                    zip_file_name,
                    extracted_folder_path,
                    compile_status,
                    run_status,
                    output_status,
                    actual_output,
                    expected_output_snapshot,
                    error_message,
                    processed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, result.getProjectId());
            statement.setString(2, result.getStudentId());
            statement.setString(3, result.getZipFileName());
            statement.setString(4, result.getExtractedFolderPath());
            statement.setString(5, result.getCompileStatus());
            statement.setString(6, result.getRunStatus());
            statement.setString(7, result.getOutputStatus());
            statement.setString(8, result.getActualOutput());
            statement.setString(9, result.getExpectedOutputSnapshot());
            statement.setString(10, result.getErrorMessage());
            statement.setString(11, result.getProcessedAt());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to create student result.");
            e.printStackTrace();
        }
    }

    // get student results by project id
    public List<StudentResult> getResultsByProjectId(int projectId) {
        List<StudentResult> results = new ArrayList<>();

        String sql = "SELECT * FROM student_results WHERE project_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToStudentResult(rs));
            }

        } catch (SQLException e) {
            System.err.println("Failed to get student results.");
            e.printStackTrace();
        }

        return results;
    }

    // map result set to student result
    private StudentResult mapResultSetToStudentResult(ResultSet rs) throws SQLException {
        StudentResult result = new StudentResult();

        result.setId(rs.getInt("id"));
        result.setProjectId(rs.getInt("project_id"));
        result.setStudentId(rs.getString("student_id"));
        result.setZipFileName(rs.getString("zip_file_name"));
        result.setExtractedFolderPath(rs.getString("extracted_folder_path"));
        result.setCompileStatus(rs.getString("compile_status"));
        result.setRunStatus(rs.getString("run_status"));
        result.setOutputStatus(rs.getString("output_status"));
        result.setActualOutput(rs.getString("actual_output"));
        result.setExpectedOutputSnapshot(rs.getString("expected_output_snapshot"));
        result.setErrorMessage(rs.getString("error_message"));
        result.setProcessedAt(rs.getString("processed_at"));

        return result;
    }
}