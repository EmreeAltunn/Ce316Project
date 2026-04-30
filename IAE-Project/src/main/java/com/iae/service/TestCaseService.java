package com.iae.service;

import com.iae.database.DatabaseManager;
import com.iae.model.TestCase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// test case service
public class TestCaseService {

    public void createTestCase(TestCase testCase) {
        String sql = """
                INSERT INTO test_cases (
                    project_id,
                    name,
                    input_type,
                    input_data,
                    input_file_path,
                    expected_output_type,
                    expected_output_data,
                    expected_output_file_path,
                    command_line_args,
                    order_index,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, testCase.getProjectId());
            statement.setString(2, testCase.getName());
            statement.setString(3, testCase.getInputType());
            statement.setString(4, testCase.getInputData());
            statement.setString(5, testCase.getInputFilePath());
            statement.setString(6, testCase.getExpectedOutputType());
            statement.setString(7, testCase.getExpectedOutputData());
            statement.setString(8, testCase.getExpectedOutputFilePath());
            statement.setString(9, testCase.getCommandLineArgs());
            statement.setInt(10, testCase.getOrderIndex());
            statement.setString(11, testCase.getCreatedAt());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to create test case.");
            e.printStackTrace();
        }
    }

    // get test cases by project id
    public List<TestCase> getTestCasesByProjectId(int projectId) {
        List<TestCase> testCases = new ArrayList<>();

        String sql = "SELECT * FROM test_cases WHERE project_id = ? ORDER BY order_index ASC";

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                testCases.add(mapResultSetToTestCase(resultSet));
            }

        } catch (SQLException e) {
            System.err.println("Failed to get test cases.");
            e.printStackTrace();
        }

        return testCases;
    }

    // delete test cases by project id
    public void deleteTestCasesByProjectId(int projectId) {
        String sql = "DELETE FROM test_cases WHERE project_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);
            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to delete test cases.");
            e.printStackTrace();
        }
    }

    // map result set to test case
    private TestCase mapResultSetToTestCase(ResultSet rs) throws SQLException {
        TestCase testCase = new TestCase();

        testCase.setId(rs.getInt("id"));
        testCase.setProjectId(rs.getInt("project_id"));
        testCase.setName(rs.getString("name"));
        testCase.setInputType(rs.getString("input_type"));
        testCase.setInputData(rs.getString("input_data"));
        testCase.setInputFilePath(rs.getString("input_file_path"));
        testCase.setExpectedOutputType(rs.getString("expected_output_type"));
        testCase.setExpectedOutputData(rs.getString("expected_output_data"));
        testCase.setExpectedOutputFilePath(rs.getString("expected_output_file_path"));
        testCase.setCommandLineArgs(rs.getString("command_line_args"));
        testCase.setOrderIndex(rs.getInt("order_index"));
        testCase.setCreatedAt(rs.getString("created_at"));

        return testCase;
    }
}
