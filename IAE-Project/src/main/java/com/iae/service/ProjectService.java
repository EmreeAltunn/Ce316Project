package com.iae.service;

import com.iae.database.DatabaseManager;
import com.iae.model.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// adds new project to db
public class ProjectService {

    public void createProject(Project project) {
        String sql = """
                INSERT INTO projects (
                    name,
                    configuration_id,
                    description,
                    submissions_directory,
                    working_directory,
                    report_path,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, project.getName());
            statement.setInt(2, project.getConfigurationId());
            statement.setString(3, project.getDescription());
            statement.setString(4, project.getSubmissionsDirectory());
            statement.setString(5, project.getWorkingDirectory());
            statement.setString(6, project.getReportPath());
            statement.setString(7, project.getCreatedAt());
            statement.setString(8, project.getUpdatedAt());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to create project.");
            e.printStackTrace();
        }
    }

    // gets project by id
    public Project getProjectById(int id) {
        String sql = "SELECT * FROM projects WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToProject(resultSet);
            }

        } catch (SQLException e) {
            System.err.println("Failed to get project by id.");
            e.printStackTrace();
        }

        return null;
    }

    // lists all projects
    public List<Project> getAllProjects() {
        List<Project> projects = new ArrayList<>();

        String sql = "SELECT * FROM projects ORDER BY created_at DESC";

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                projects.add(mapResultSetToProject(resultSet));
            }

        } catch (SQLException e) {
            System.err.println("Failed to get all projects.");
            e.printStackTrace();
        }

        return projects;
    }

    // lists projects by configuration id
    public List<Project> getProjectsByConfigurationId(int configurationId) {
        List<Project> projects = new ArrayList<>();

        String sql = "SELECT * FROM projects WHERE configuration_id = ? ORDER BY created_at DESC";

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, configurationId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                projects.add(mapResultSetToProject(resultSet));
            }

        } catch (SQLException e) {
            System.err.println("Failed to get projects by configuration id.");
            e.printStackTrace();
        }

        return projects;
    }

    // updates a project
    public void updateProject(Project project) {
        String sql = """
                UPDATE projects
                SET
                    name = ?,
                    configuration_id = ?,
                    description = ?,
                    submissions_directory = ?,
                    working_directory = ?,
                    report_path = ?,
                    updated_at = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, project.getName());
            statement.setInt(2, project.getConfigurationId());
            statement.setString(3, project.getDescription());
            statement.setString(4, project.getSubmissionsDirectory());
            statement.setString(5, project.getWorkingDirectory());
            statement.setString(6, project.getReportPath());
            statement.setString(7, project.getUpdatedAt());
            statement.setInt(8, project.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to update project.");
            e.printStackTrace();
        }
    }

    // deletes a project
    public void deleteProject(int id) {
        String sql = "DELETE FROM projects WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to delete project.");
            e.printStackTrace();
        }
    }

    // converts resultset to project
    private Project mapResultSetToProject(ResultSet resultSet) throws SQLException {
        Project project = new Project();

        project.setId(resultSet.getInt("id"));
        project.setName(resultSet.getString("name"));
        project.setConfigurationId(resultSet.getInt("configuration_id"));
        project.setDescription(resultSet.getString("description"));
        project.setSubmissionsDirectory(resultSet.getString("submissions_directory"));
        project.setWorkingDirectory(resultSet.getString("working_directory"));
        project.setReportPath(resultSet.getString("report_path"));
        project.setCreatedAt(resultSet.getString("created_at"));
        project.setUpdatedAt(resultSet.getString("updated_at"));

        return project;
    }
}
