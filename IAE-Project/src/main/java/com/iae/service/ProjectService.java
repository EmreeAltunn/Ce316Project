package com.iae.service;

import com.iae.db.DatabaseManager;
import com.iae.model.Project;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DatabaseManager dbManager;

    public ProjectService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<Project> findAll() throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM projects";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                projects.add(mapRowToProject(rs));
            }
        }
        return projects;
    }

    public Optional<Project> findById(int id) throws SQLException {
        String sql = "SELECT * FROM projects WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToProject(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Project save(Project project) throws SQLException {
        String sql = "INSERT INTO projects(name, description, configuration_id, submissions_directory, working_directory, created_at, updated_at) VALUES(?, ?, ?, ?, ?, ?, ?)";
        
        if (project.getCreatedAt() == null) {
            project.setCreatedAt(LocalDateTime.now());
        }
        if (project.getUpdatedAt() == null) {
            project.setUpdatedAt(LocalDateTime.now());
        }

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, project.getName());
            pstmt.setString(2, project.getDescription());
            pstmt.setInt(3, project.getConfigurationId());
            pstmt.setString(4, project.getSubmissionsDirectory());
            pstmt.setString(5, project.getWorkingDirectory());
            pstmt.setString(6, project.getCreatedAt().format(FORMATTER));
            pstmt.setString(7, project.getUpdatedAt().format(FORMATTER));
            
            pstmt.executeUpdate();
            
            try (Statement stmt = dbManager.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    project.setId(rs.getInt(1));
                }
            }
        }
        return project;
    }

    public void update(Project project) throws SQLException {
        String sql = "UPDATE projects SET name = ?, description = ?, configuration_id = ?, submissions_directory = ?, working_directory = ?, updated_at = ? WHERE id = ?";
        
        project.setUpdatedAt(LocalDateTime.now());

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            
            pstmt.setString(1, project.getName());
            pstmt.setString(2, project.getDescription());
            pstmt.setInt(3, project.getConfigurationId());
            pstmt.setString(4, project.getSubmissionsDirectory());
            pstmt.setString(5, project.getWorkingDirectory());
            pstmt.setString(6, project.getUpdatedAt().format(FORMATTER));
            pstmt.setInt(7, project.getId());
            
            pstmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM projects WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    private Project mapRowToProject(ResultSet rs) throws SQLException {
        Project project = new Project();
        project.setId(rs.getInt("id"));
        project.setName(rs.getString("name"));
        project.setDescription(rs.getString("description"));
        project.setConfigurationId(rs.getInt("configuration_id"));
        project.setSubmissionsDirectory(rs.getString("submissions_directory"));
        project.setWorkingDirectory(rs.getString("working_directory"));
        project.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), FORMATTER));
        project.setUpdatedAt(LocalDateTime.parse(rs.getString("updated_at"), FORMATTER));
        return project;
    }
}
