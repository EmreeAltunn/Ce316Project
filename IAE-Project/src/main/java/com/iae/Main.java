package com.iae;

import com.iae.database.DatabaseManager;
import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.StudentResult;
import com.iae.model.TestCase;
import com.iae.service.ConfigurationService;
import com.iae.service.ProjectService;
import com.iae.service.StudentResultService;
import com.iae.service.TestCaseService;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();

        String now = LocalDateTime.now().toString();

        ConfigurationService configurationService = new ConfigurationService();
        ProjectService projectService = new ProjectService();
        TestCaseService testCaseService = new TestCaseService();
        StudentResultService studentResultService = new StudentResultService();

        Configuration config = new Configuration();
        config.setName("C Configuration");
        config.setLanguage("C");
        config.setCompileCommand("gcc");
        config.setCompileArgs("main.c -o main");
        config.setRunCommand("./main");
        config.setRunArgsTemplate("");
        config.setCompareMethod("TRIMMED");
        config.setSourceFileName("main.c");
        config.setCreatedAt(now);
        config.setUpdatedAt(now);

        configurationService.createConfiguration(config);

        Configuration savedConfig = configurationService.getAllConfigurations()
                .stream()
                .filter(c -> c.getName().equals("C Configuration"))
                .findFirst()
                .orElse(null);

        if (savedConfig == null) {
            System.out.println("Configuration could not be found.");
            return;
        }

        Project project = new Project();
        project.setName("Sorting Assignment");
        project.setConfigurationId(savedConfig.getId());
        project.setDescription("Test project for C sorting assignment");
        project.setSubmissionsDirectory("submissions/");
        project.setWorkingDirectory("workspace/");
        project.setReportPath("reports/report.txt");
        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        projectService.createProject(project);

        Project savedProject = projectService.getAllProjects()
                .stream()
                .filter(p -> p.getName().equals("Sorting Assignment"))
                .findFirst()
                .orElse(null);

        if (savedProject == null) {
            System.out.println("Project could not be found.");
            return;
        }

        TestCase testCase = new TestCase();
        testCase.setProjectId(savedProject.getId());
        testCase.setName("Basic sorting test");
        testCase.setInputType("INLINE");
        testCase.setInputData("banana apple cherry");
        testCase.setExpectedOutputType("INLINE");
        testCase.setExpectedOutputData("apple banana cherry");
        testCase.setCommandLineArgs("banana apple cherry");
        testCase.setOrderIndex(1);
        testCase.setCreatedAt(now);

        testCaseService.createTestCase(testCase);

        StudentResult result = new StudentResult();
        result.setProjectId(savedProject.getId());
        result.setStudentId("220101");
        result.setZipFileName("220101.zip");
        result.setExtractedFolderPath("workspace/220101");
        result.setCompileStatus("SUCCESS");
        result.setRunStatus("SUCCESS");
        result.setOutputStatus("SUCCESS");
        result.setActualOutput("apple banana cherry");
        result.setExpectedOutputSnapshot("apple banana cherry");
        result.setErrorMessage(null);
        result.setProcessedAt(now);

        studentResultService.createStudentResult(result);

        System.out.println("Test completed successfully.");
        System.out.println("Configurations: " + configurationService.getAllConfigurations().size());
        System.out.println("Projects: " + projectService.getAllProjects().size());
        System.out.println("Test cases: " + testCaseService.getTestCasesByProjectId(savedProject.getId()).size());
        System.out
                .println("Student results: " + studentResultService.getResultsByProjectId(savedProject.getId()).size());
    }
}