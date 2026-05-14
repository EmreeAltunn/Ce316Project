package com.iae;

import com.iae.db.DatabaseManager;
import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.ResultStatus;
import com.iae.model.StudentResult;
import com.iae.model.TestCase;
import com.iae.service.ConfigurationService;
import com.iae.service.ProjectService;
import com.iae.service.StudentResultService;
import com.iae.service.TestCaseService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            dbManager.initialize(".");

            ConfigurationService configurationService = new ConfigurationService(dbManager);
            ProjectService projectService = new ProjectService(dbManager);
            TestCaseService testCaseService = new TestCaseService(dbManager);
            StudentResultService studentResultService = new StudentResultService(dbManager);

            Optional<Configuration> savedConfigOpt = configurationService.findByName("C Configuration Test");
            Configuration savedConfig;
            
            if (savedConfigOpt.isEmpty()) {
                Configuration config = new Configuration();
                config.setName("C Configuration Test");
                config.setCompilerPath("gcc");
                config.setCompilerArgs("main.c -o main");
                config.setRunCommand("./main");
                config.setRequiresCompilation(true);
                config.setSourceFileName("main.c");
                config.setOutputFileName("main");
                config.setFileExtension(".c");
                config.setDescription("A test C config");
                config.setCreatedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());

                savedConfig = configurationService.save(config);
            } else {
                savedConfig = savedConfigOpt.get();
            }

            Project project = new Project();
            project.setName("Sorting Assignment");
            project.setDescription("Implement quicksort");
            project.setConfigurationId(savedConfig.getId());
            project.setSubmissionsDirectory("./submissions");
            project.setWorkingDirectory("./workspace");
            project.setCreatedAt(LocalDateTime.now());
            project.setUpdatedAt(LocalDateTime.now());

            Project savedProject = projectService.save(project);

            TestCase testCase = new TestCase();
            testCase.setProjectId(savedProject.getId());
            testCase.setName("Test 1");
            testCase.setInputArgs("5 4 3 2 1");
            testCase.setExpectedOutputFile("output1.txt");
            testCase.setOrderIndex(1);

            testCaseService.save(testCase);

            StudentResult result = new StudentResult();
            result.setProjectId(savedProject.getId());
            result.setStudentId("2020123456");
            result.setZipFilePath("./submissions/2020123456.zip");
            result.setCompileStatus(ResultStatus.SUCCESS);
            result.setCompileOutput("Compiled successfully.");
            result.setCompileError("");
            result.setRunStatus(ResultStatus.SUCCESS);
            result.setProgramOutput("1 2 3 4 5");
            result.setErrorOutput("");
            result.setTestStatus(ResultStatus.PASS);
            result.setTestDetails("Passed Test 1.");
            result.setProcessedAt(LocalDateTime.now());

            studentResultService.save(result);

            System.out.println("Test completed successfully.");
            System.out.println("Configurations: " + configurationService.findAll().size());
            System.out.println("Projects: " + projectService.findAll().size());
            System.out.println("Test cases: " + testCaseService.findByProjectId(savedProject.getId()).size());
            System.out.println("Student results: " + studentResultService.findByProjectId(savedProject.getId()).size());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}