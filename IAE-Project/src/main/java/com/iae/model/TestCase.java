package com.iae.model;

public class TestCase {

    private int id;
    private int projectId;
    private String name;
    private String inputType;
    private String inputData;
    private String inputFilePath;
    private String expectedOutputType;
    private String expectedOutputData;
    private String expectedOutputFilePath;
    private String commandLineArgs;
    private int orderIndex;
    private String createdAt;

    public TestCase() {
    }

    public TestCase(int id, int projectId, String name,
            String inputType, String inputData, String inputFilePath,
            String expectedOutputType, String expectedOutputData,
            String expectedOutputFilePath, String commandLineArgs,
            int orderIndex, String createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.inputType = inputType;
        this.inputData = inputData;
        this.inputFilePath = inputFilePath;
        this.expectedOutputType = expectedOutputType;
        this.expectedOutputData = expectedOutputData;
        this.expectedOutputFilePath = expectedOutputFilePath;
        this.commandLineArgs = commandLineArgs;
        this.orderIndex = orderIndex;
        this.createdAt = createdAt;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getInputFilePath() {
        return inputFilePath;
    }

    public void setInputFilePath(String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

    public String getExpectedOutputType() {
        return expectedOutputType;
    }

    public void setExpectedOutputType(String expectedOutputType) {
        this.expectedOutputType = expectedOutputType;
    }

    public String getExpectedOutputData() {
        return expectedOutputData;
    }

    public void setExpectedOutputData(String expectedOutputData) {
        this.expectedOutputData = expectedOutputData;
    }

    public String getExpectedOutputFilePath() {
        return expectedOutputFilePath;
    }

    public void setExpectedOutputFilePath(String expectedOutputFilePath) {
        this.expectedOutputFilePath = expectedOutputFilePath;
    }

    public String getCommandLineArgs() {
        return commandLineArgs;
    }

    public void setCommandLineArgs(String commandLineArgs) {
        this.commandLineArgs = commandLineArgs;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}