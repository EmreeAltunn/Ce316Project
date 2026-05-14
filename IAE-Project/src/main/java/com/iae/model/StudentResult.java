package com.iae.model;

import java.time.LocalDateTime;

public class StudentResult {
    private int id;
    private int projectId;
    private String studentId;
    private String zipFilePath;
    private ResultStatus compileStatus;
    private String compileOutput;
    private String compileError;
    private ResultStatus runStatus;
    private String programOutput;
    private String errorOutput;
    private ResultStatus testStatus;
    private String testDetails;
    private LocalDateTime processedAt;

    public StudentResult() {
    }

    public StudentResult(int id, int projectId, String studentId, String zipFilePath, ResultStatus compileStatus, String compileOutput, String compileError, ResultStatus runStatus, String programOutput, String errorOutput, ResultStatus testStatus, String testDetails, LocalDateTime processedAt) {
        this.id = id;
        this.projectId = projectId;
        this.studentId = studentId;
        this.zipFilePath = zipFilePath;
        this.compileStatus = compileStatus;
        this.compileOutput = compileOutput;
        this.compileError = compileError;
        this.runStatus = runStatus;
        this.programOutput = programOutput;
        this.errorOutput = errorOutput;
        this.testStatus = testStatus;
        this.testDetails = testDetails;
        this.processedAt = processedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getZipFilePath() { return zipFilePath; }
    public void setZipFilePath(String zipFilePath) { this.zipFilePath = zipFilePath; }

    public ResultStatus getCompileStatus() { return compileStatus; }
    public void setCompileStatus(ResultStatus compileStatus) { this.compileStatus = compileStatus; }

    public String getCompileOutput() { return compileOutput; }
    public void setCompileOutput(String compileOutput) { this.compileOutput = compileOutput; }

    public String getCompileError() { return compileError; }
    public void setCompileError(String compileError) { this.compileError = compileError; }

    public ResultStatus getRunStatus() { return runStatus; }
    public void setRunStatus(ResultStatus runStatus) { this.runStatus = runStatus; }

    public String getProgramOutput() { return programOutput; }
    public void setProgramOutput(String programOutput) { this.programOutput = programOutput; }

    public String getErrorOutput() { return errorOutput; }
    public void setErrorOutput(String errorOutput) { this.errorOutput = errorOutput; }

    public ResultStatus getTestStatus() { return testStatus; }
    public void setTestStatus(ResultStatus testStatus) { this.testStatus = testStatus; }

    public String getTestDetails() { return testDetails; }
    public void setTestDetails(String testDetails) { this.testDetails = testDetails; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
