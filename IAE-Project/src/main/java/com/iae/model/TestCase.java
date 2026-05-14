package com.iae.model;

public class TestCase {
    private int id;
    private int projectId;
    private String name;
    private String inputArgs;
    private String expectedOutputFile;
    private int orderIndex;

    public TestCase() {
    }

    public TestCase(int id, int projectId, String name, String inputArgs, String expectedOutputFile, int orderIndex) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.inputArgs = inputArgs;
        this.expectedOutputFile = expectedOutputFile;
        this.orderIndex = orderIndex;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInputArgs() { return inputArgs; }
    public void setInputArgs(String inputArgs) { this.inputArgs = inputArgs; }

    public String getExpectedOutputFile() { return expectedOutputFile; }
    public void setExpectedOutputFile(String expectedOutputFile) { this.expectedOutputFile = expectedOutputFile; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}