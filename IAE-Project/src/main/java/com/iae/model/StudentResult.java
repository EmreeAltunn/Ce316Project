package com.iae.model;

public class StudentResult {

    private int id;
    private int projectId;
    private String studentId;
    private String zipFileName;
    private String extractedFolderPath;
    private String compileStatus;
    private String runStatus;
    private String outputStatus;
    private String actualOutput;
    private String expectedOutputSnapshot;
    private String errorMessage;
    private String processedAt;

    public StudentResult() {
    }

    public StudentResult(int id, int projectId, String studentId,
            String zipFileName, String extractedFolderPath,
            String compileStatus, String runStatus,
            String outputStatus, String actualOutput,
            String expectedOutputSnapshot, String errorMessage,
            String processedAt) {
        this.id = id;
        this.projectId = projectId;
        this.studentId = studentId;
        this.zipFileName = zipFileName;
        this.extractedFolderPath = extractedFolderPath;
        this.compileStatus = compileStatus;
        this.runStatus = runStatus;
        this.outputStatus = outputStatus;
        this.actualOutput = actualOutput;
        this.expectedOutputSnapshot = expectedOutputSnapshot;
        this.errorMessage = errorMessage;
        this.processedAt = processedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public String getExtractedFolderPath() {
        return extractedFolderPath;
    }

    public void setExtractedFolderPath(String extractedFolderPath) {
        this.extractedFolderPath = extractedFolderPath;
    }

    public String getCompileStatus() {
        return compileStatus;
    }

    public void setCompileStatus(String compileStatus) {
        this.compileStatus = compileStatus;
    }

    public String getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(String runStatus) {
        this.runStatus = runStatus;
    }

    public String getOutputStatus() {
        return outputStatus;
    }

    public void setOutputStatus(String outputStatus) {
        this.outputStatus = outputStatus;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(String actualOutput) {
        this.actualOutput = actualOutput;
    }

    public String getExpectedOutputSnapshot() {
        return expectedOutputSnapshot;
    }

    public void setExpectedOutputSnapshot(String expectedOutputSnapshot) {
        this.expectedOutputSnapshot = expectedOutputSnapshot;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
}
