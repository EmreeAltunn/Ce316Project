package com.iae.service;

import com.iae.db.DatabaseManager;
import com.iae.model.Configuration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfigurationService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DatabaseManager dbManager;

    public ConfigurationService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<Configuration> findAll() throws SQLException {
        List<Configuration> configurations = new ArrayList<>();
        String sql = "SELECT * FROM configurations";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                configurations.add(mapRowToConfiguration(rs));
            }
        }
        return configurations;
    }

    public Optional<Configuration> findById(int id) throws SQLException {
        String sql = "SELECT * FROM configurations WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToConfiguration(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Configuration> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM configurations WHERE name = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToConfiguration(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Configuration save(Configuration config) throws SQLException {
        String sql = "INSERT INTO configurations(name, compiler_path, compiler_args, run_command, requires_compilation, source_file_name, output_file_name, file_extension, description, created_at, updated_at) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        if (config.getCreatedAt() == null) {
            config.setCreatedAt(LocalDateTime.now());
        }
        if (config.getUpdatedAt() == null) {
            config.setUpdatedAt(LocalDateTime.now());
        }

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, config.getName());
            pstmt.setString(2, config.getCompilerPath());
            pstmt.setString(3, config.getCompilerArgs());
            pstmt.setString(4, config.getRunCommand());
            pstmt.setInt(5, config.isRequiresCompilation() ? 1 : 0);
            pstmt.setString(6, config.getSourceFileName());
            pstmt.setString(7, config.getOutputFileName());
            pstmt.setString(8, config.getFileExtension());
            pstmt.setString(9, config.getDescription());
            pstmt.setString(10, config.getCreatedAt().format(FORMATTER));
            pstmt.setString(11, config.getUpdatedAt().format(FORMATTER));
            
            pstmt.executeUpdate();
            
            try (Statement stmt = dbManager.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    config.setId(rs.getInt(1));
                }
            }
        }
        return config;
    }

    public void update(Configuration config) throws SQLException {
        String sql = "UPDATE configurations SET name = ?, compiler_path = ?, compiler_args = ?, run_command = ?, requires_compilation = ?, source_file_name = ?, output_file_name = ?, file_extension = ?, description = ?, updated_at = ? WHERE id = ?";
        
        config.setUpdatedAt(LocalDateTime.now());

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            
            pstmt.setString(1, config.getName());
            pstmt.setString(2, config.getCompilerPath());
            pstmt.setString(3, config.getCompilerArgs());
            pstmt.setString(4, config.getRunCommand());
            pstmt.setInt(5, config.isRequiresCompilation() ? 1 : 0);
            pstmt.setString(6, config.getSourceFileName());
            pstmt.setString(7, config.getOutputFileName());
            pstmt.setString(8, config.getFileExtension());
            pstmt.setString(9, config.getDescription());
            pstmt.setString(10, config.getUpdatedAt().format(FORMATTER));
            pstmt.setInt(11, config.getId());
            
            pstmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM configurations WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    private Configuration mapRowToConfiguration(ResultSet rs) throws SQLException {
        Configuration config = new Configuration();
        config.setId(rs.getInt("id"));
        config.setName(rs.getString("name"));
        config.setCompilerPath(rs.getString("compiler_path"));
        config.setCompilerArgs(rs.getString("compiler_args"));
        config.setRunCommand(rs.getString("run_command"));
        config.setRequiresCompilation(rs.getInt("requires_compilation") == 1);
        config.setSourceFileName(rs.getString("source_file_name"));
        config.setOutputFileName(rs.getString("output_file_name"));
        config.setFileExtension(rs.getString("file_extension"));
        config.setDescription(rs.getString("description"));
        config.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), FORMATTER));
        config.setUpdatedAt(LocalDateTime.parse(rs.getString("updated_at"), FORMATTER));
        return config;
    }
}