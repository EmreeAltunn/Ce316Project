package com.iae.model;

public class Configuration {

    private int id;
    private String name;
    private String language;
    private String compileCommand;
    private String compileArgs;
    private String runCommand;
    private String runArgsTemplate;
    private String compareMethod;
    private String sourceFileName;
    private String createdAt;
    private String updatedAt;

    public Configuration() {
    }

    public Configuration(int id, String name, String language,
            String compileCommand, String compileArgs,
            String runCommand, String runArgsTemplate,
            String compareMethod, String sourceFileName,
            String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.language = language;
        this.compileCommand = compileCommand;
        this.compileArgs = compileArgs;
        this.runCommand = runCommand;
        this.runArgsTemplate = runArgsTemplate;
        this.compareMethod = compareMethod;
        this.sourceFileName = sourceFileName;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCompileCommand() {
        return compileCommand;
    }

    public void setCompileCommand(String compileCommand) {
        this.compileCommand = compileCommand;
    }

    public String getCompileArgs() {
        return compileArgs;
    }

    public void setCompileArgs(String compileArgs) {
        this.compileArgs = compileArgs;
    }

    public String getRunCommand() {
        return runCommand;
    }

    public void setRunCommand(String runCommand) {
        this.runCommand = runCommand;
    }

    public String getRunArgsTemplate() {
        return runArgsTemplate;
    }

    public void setRunArgsTemplate(String runArgsTemplate) {
        this.runArgsTemplate = runArgsTemplate;
    }

    public String getCompareMethod() {
        return compareMethod;
    }

    public void setCompareMethod(String compareMethod) {
        this.compareMethod = compareMethod;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
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
