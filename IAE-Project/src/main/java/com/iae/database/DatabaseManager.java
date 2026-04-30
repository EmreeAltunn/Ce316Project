package com.iae.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DATABASE_URL = "jdbc:sqlite:iae.db";

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DATABASE_URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }

        return connection;
    }

    public static void initializeDatabase() {
        try (Connection connection = getConnection();
                Statement statement = connection.createStatement()) {

            statement.execute("""
                        CREATE TABLE IF NOT EXISTS schema_version (
                            version INTEGER NOT NULL
                        );
                    """);

            statement.execute("""
                        CREATE TABLE IF NOT EXISTS configurations (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL UNIQUE,
                            language TEXT NOT NULL,
                            compile_command TEXT,
                            compile_args TEXT,
                            run_command TEXT NOT NULL,
                            run_args_template TEXT,
                            compare_method TEXT NOT NULL CHECK (compare_method IN ('EXACT', 'TRIMMED', 'LINE_BY_LINE')),
                            source_file_name TEXT NOT NULL,
                            created_at TEXT NOT NULL,
                            updated_at TEXT NOT NULL
                        );
                    """);

            statement.execute("""
                        CREATE TABLE IF NOT EXISTS projects (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            configuration_id INTEGER NOT NULL,
                            description TEXT,
                            submissions_directory TEXT,
                            working_directory TEXT,
                            report_path TEXT,
                            created_at TEXT NOT NULL,
                            updated_at TEXT NOT NULL,
                            FOREIGN KEY (configuration_id) REFERENCES configurations(id)
                                ON DELETE RESTRICT
                                ON UPDATE CASCADE
                        );
                    """);

            statement.execute("""
                        CREATE TABLE IF NOT EXISTS test_cases (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            project_id INTEGER NOT NULL,
                            name TEXT NOT NULL,
                            input_type TEXT NOT NULL CHECK (input_type IN ('INLINE', 'FILE')),
                            input_data TEXT,
                            input_file_path TEXT,
                            expected_output_type TEXT NOT NULL CHECK (expected_output_type IN ('INLINE', 'FILE')),
                            expected_output_data TEXT,
                            expected_output_file_path TEXT,
                            command_line_args TEXT,
                            order_index INTEGER NOT NULL DEFAULT 0,
                            created_at TEXT NOT NULL,
                            FOREIGN KEY (project_id) REFERENCES projects(id)
                                ON DELETE CASCADE
                                ON UPDATE CASCADE
                        );
                    """);

            statement.execute(
                    """
                                CREATE TABLE IF NOT EXISTS student_results (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    project_id INTEGER NOT NULL,
                                    student_id TEXT NOT NULL,
                                    zip_file_name TEXT,
                                    extracted_folder_path TEXT,
                                    compile_status TEXT NOT NULL CHECK (compile_status IN ('NOT_RUN', 'SUCCESS', 'FAILED', 'ERROR')),
                                    run_status TEXT NOT NULL CHECK (run_status IN ('NOT_RUN', 'SUCCESS', 'FAILED', 'ERROR')),
                                    output_status TEXT NOT NULL CHECK (output_status IN ('NOT_RUN', 'SUCCESS', 'FAILED', 'ERROR')),
                                    actual_output TEXT,
                                    expected_output_snapshot TEXT,
                                    error_message TEXT,
                                    processed_at TEXT NOT NULL,
                                    FOREIGN KEY (project_id) REFERENCES projects(id)
                                        ON DELETE CASCADE
                                        ON UPDATE CASCADE
                                );
                            """);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization failed.");
            e.printStackTrace();
        }
    }
}