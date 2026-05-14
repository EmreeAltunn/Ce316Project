package com.iae.service;

import com.iae.db.DatabaseManager;
import com.iae.model.TestCase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestCaseService {

    private final DatabaseManager dbManager;

    public TestCaseService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public TestCase save(TestCase testCase) throws SQLException {
        String sql = "INSERT INTO test_cases (project_id, name, input_args, expected_output_file, order_index) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, testCase.getProjectId());
            stmt.setString(2, testCase.getName());
            stmt.setString(3, testCase.getInputArgs());
            stmt.setString(4, testCase.getExpectedOutputFile());
            stmt.setInt(5, testCase.getOrderIndex());

            stmt.executeUpdate();

            try (Statement sqlStmt = dbManager.getConnection().createStatement();
                 ResultSet rs = sqlStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    testCase.setId(rs.getInt(1));
                }
            }
        }

        return testCase;
    }

    public List<TestCase> findByProjectId(int projectId) throws SQLException {
        List<TestCase> list = new ArrayList<>();

        String sql = "SELECT * FROM test_cases WHERE project_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {

            stmt.setInt(1, projectId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }
        }

        return list;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM test_cases WHERE id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private TestCase map(ResultSet rs) throws SQLException {
        TestCase tc = new TestCase();
        tc.setId(rs.getInt("id"));
        tc.setProjectId(rs.getInt("project_id"));
        tc.setName(rs.getString("name"));
        tc.setInputArgs(rs.getString("input_args"));
        tc.setExpectedOutputFile(rs.getString("expected_output_file"));
        tc.setOrderIndex(rs.getInt("order_index"));
        return tc;
    }
}