package com.iae.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    public static final String DB_FILE_NAME = "iae.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void initialize(String dbDirectoryPath) throws SQLException {
        File directory = new File(dbDirectoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String url = "jdbc:sqlite:" + dbDirectoryPath + File.separator + DB_FILE_NAME;
        this.connection = DriverManager.getConnection(url);
        
        try (Statement stmt = this.connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        
        createTables();
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private void createTables() throws SQLException {
        String createConfigurations = "CREATE TABLE IF NOT EXISTS configurations ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL UNIQUE,"
                + "compiler_path TEXT,"
                + "compiler_args TEXT,"
                + "run_command TEXT NOT NULL,"
                + "requires_compilation INTEGER NOT NULL DEFAULT 1,"
                + "source_file_name TEXT NOT NULL,"
                + "output_file_name TEXT,"
                + "file_extension TEXT NOT NULL,"
                + "description TEXT,"
                + "created_at TEXT NOT NULL,"
                + "updated_at TEXT NOT NULL"
                + ");";

        String createProjects = "CREATE TABLE IF NOT EXISTS projects ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "description TEXT,"
                + "configuration_id INTEGER NOT NULL,"
                + "submissions_directory TEXT,"
                + "working_directory TEXT,"
                + "created_at TEXT NOT NULL,"
                + "updated_at TEXT NOT NULL,"
                + "FOREIGN KEY(configuration_id) REFERENCES configurations(id)"
                + ");";

        String createTestCases = "CREATE TABLE IF NOT EXISTS test_cases ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "project_id INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "input_args TEXT,"
                + "expected_output_file TEXT,"
                + "order_index INTEGER NOT NULL DEFAULT 0,"
                + "FOREIGN KEY(project_id) REFERENCES projects(id)"
                + ");";

        String createStudentResults = "CREATE TABLE IF NOT EXISTS student_results ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "project_id INTEGER NOT NULL,"
                + "student_id TEXT NOT NULL,"
                + "zip_file_path TEXT NOT NULL,"
                + "compile_status TEXT NOT NULL DEFAULT 'PENDING',"
                + "compile_output TEXT,"
                + "compile_error TEXT,"
                + "run_status TEXT,"
                + "program_output TEXT,"
                + "error_output TEXT,"
                + "test_status TEXT,"
                + "test_details TEXT,"
                + "processed_at TEXT,"
                + "FOREIGN KEY(project_id) REFERENCES projects(id)"
                + ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createConfigurations);
            stmt.execute(createProjects);
            stmt.execute(createTestCases);
            stmt.execute(createStudentResults);
        }
    }
}
