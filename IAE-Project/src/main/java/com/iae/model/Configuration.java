package com.iae.model;

import java.time.LocalDateTime;

public class Configuration {
    private int id;
    private String name;
    private String compilerPath;
    private String compilerArgs;
    private String runCommand;
    private boolean requiresCompilation;
    private String sourceFileName;
    private String outputFileName;
    private String fileExtension;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Configuration() {
    }

    public Configuration(int id, String name, String compilerPath, String compilerArgs, String runCommand, boolean requiresCompilation, String sourceFileName, String outputFileName, String fileExtension, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.compilerPath = compilerPath;
        this.compilerArgs = compilerArgs;
        this.runCommand = runCommand;
        this.requiresCompilation = requiresCompilation;
        this.sourceFileName = sourceFileName;
        this.outputFileName = outputFileName;
        this.fileExtension = fileExtension;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCompilerPath() { return compilerPath; }
    public void setCompilerPath(String compilerPath) { this.compilerPath = compilerPath; }

    public String getCompilerArgs() { return compilerArgs; }
    public void setCompilerArgs(String compilerArgs) { this.compilerArgs = compilerArgs; }

    public String getRunCommand() { return runCommand; }
    public void setRunCommand(String runCommand) { this.runCommand = runCommand; }

    public boolean isRequiresCompilation() { return requiresCompilation; }
    public void setRequiresCompilation(boolean requiresCompilation) { this.requiresCompilation = requiresCompilation; }

    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
