package com.iae.service;

import com.iae.database.DatabaseManager;
import com.iae.model.Configuration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationService {
    // adds new configuration
    public void createConfiguration(Configuration configuration) {
        String sql = """
                INSERT INTO configurations (
                    name,
                    language,
                    compile_command,
                    compile_args,
                    run_command,
                    run_args_template,
                    compare_method,
                    source_file_name,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, configuration.getName());
            statement.setString(2, configuration.getLanguage());
            statement.setString(3, configuration.getCompileCommand());
            statement.setString(4, configuration.getCompileArgs());
            statement.setString(5, configuration.getRunCommand());
            statement.setString(6, configuration.getRunArgsTemplate());
            statement.setString(7, configuration.getCompareMethod());
            statement.setString(8, configuration.getSourceFileName());
            statement.setString(9, configuration.getCreatedAt());
            statement.setString(10, configuration.getUpdatedAt());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to create configuration.");
            e.printStackTrace();
        }
    }

    // gets confg. by ID
    public Configuration getConfigurationById(int id) {
        String sql = "SELECT * FROM configurations WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToConfiguration(resultSet);
            }

        } catch (SQLException e) {
            System.err.println("Failed to get configuration by id.");
            e.printStackTrace();
        }

        return null;
    }

    // lists all configurations
    public List<Configuration> getAllConfigurations() {
        List<Configuration> configurations = new ArrayList<>();

        String sql = "SELECT * FROM configurations ORDER BY name ASC";

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Configuration configuration = mapResultSetToConfiguration(resultSet);
                configurations.add(configuration);
            }

        } catch (SQLException e) {
            System.err.println("Failed to get all configurations.");
            e.printStackTrace();
        }

        return configurations;
    }

    // updates a configuration
    public void updateConfiguration(Configuration configuration) {
        String sql = """
                UPDATE configurations
                SET
                    name = ?,
                    language = ?,
                    compile_command = ?,
                    compile_args = ?,
                    run_command = ?,
                    run_args_template = ?,
                    compare_method = ?,
                    source_file_name = ?,
                    updated_at = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, configuration.getName());
            statement.setString(2, configuration.getLanguage());
            statement.setString(3, configuration.getCompileCommand());
            statement.setString(4, configuration.getCompileArgs());
            statement.setString(5, configuration.getRunCommand());
            statement.setString(6, configuration.getRunArgsTemplate());
            statement.setString(7, configuration.getCompareMethod());
            statement.setString(8, configuration.getSourceFileName());
            statement.setString(9, configuration.getUpdatedAt());
            statement.setInt(10, configuration.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to update configuration.");
            e.printStackTrace();
        }
    }

    // deletes a configuration
    public void deleteConfiguration(int id) {
        String sql = "DELETE FROM configurations WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to delete configuration.");
            e.printStackTrace();
        }
    }

    // converts resultset to configuration
    private Configuration mapResultSetToConfiguration(ResultSet resultSet) throws SQLException {
        Configuration configuration = new Configuration();

        configuration.setId(resultSet.getInt("id"));
        configuration.setName(resultSet.getString("name"));
        configuration.setLanguage(resultSet.getString("language"));
        configuration.setCompileCommand(resultSet.getString("compile_command"));
        configuration.setCompileArgs(resultSet.getString("compile_args"));
        configuration.setRunCommand(resultSet.getString("run_command"));
        configuration.setRunArgsTemplate(resultSet.getString("run_args_template"));
        configuration.setCompareMethod(resultSet.getString("compare_method"));
        configuration.setSourceFileName(resultSet.getString("source_file_name"));
        configuration.setCreatedAt(resultSet.getString("created_at"));
        configuration.setUpdatedAt(resultSet.getString("updated_at"));

        return configuration;
    }
}