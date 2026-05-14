package com.iae.model;

import java.time.LocalDateTime;

public class Project {
    private int id;
    private String name;
    private String description;
    private int configurationId;
    private String submissionsDirectory;
    private String workingDirectory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Project() {
    }

    public Project(int id, String name, String description, int configurationId, String submissionsDirectory, String workingDirectory, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.configurationId = configurationId;
        this.submissionsDirectory = submissionsDirectory;
        this.workingDirectory = workingDirectory;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getConfigurationId() { return configurationId; }
    public void setConfigurationId(int configurationId) { this.configurationId = configurationId; }

    public String getSubmissionsDirectory() { return submissionsDirectory; }
    public void setSubmissionsDirectory(String submissionsDirectory) { this.submissionsDirectory = submissionsDirectory; }

    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
