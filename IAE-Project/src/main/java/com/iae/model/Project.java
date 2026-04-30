package com.iae.model;

public class Project {

    private int id;
    private String name;
    private int configurationId;
    private String description;
    private String submissionsDirectory;
    private String workingDirectory;
    private String reportPath;
    private String createdAt;
    private String updatedAt;

    public Project() {
    }

    public Project(int id, String name, int configurationId, String description,
            String submissionsDirectory, String workingDirectory,
            String reportPath, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.configurationId = configurationId;
        this.description = description;
        this.submissionsDirectory = submissionsDirectory;
        this.workingDirectory = workingDirectory;
        this.reportPath = reportPath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(int configurationId) {
        this.configurationId = configurationId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubmissionsDirectory() {
        return submissionsDirectory;
    }

    public void setSubmissionsDirectory(String submissionsDirectory) {
        this.submissionsDirectory = submissionsDirectory;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
