package com.iae.runner;

import com.iae.db.DatabaseManager;
import com.iae.model.Configuration;
import com.iae.model.ProcessResult;
import com.iae.model.Project;
import com.iae.model.ResultStatus;
import com.iae.model.StudentResult;
import com.iae.model.TestCase;
import com.iae.process.ProcessExecutor;
import com.iae.process.ZipProcessor;
import com.iae.service.ConfigurationService;
import com.iae.service.ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentRunnerTest {

    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private ConfigurationService configurationService;
    private ProjectService projectService;

    @BeforeEach
    void setUp() throws SQLException {
        dbManager = DatabaseManager.getInstance();
        dbManager.initialize(tempDir.resolve("db").toString());
        configurationService = new ConfigurationService(dbManager);
        projectService = new ProjectService(dbManager);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    void runSyncContinuesBatchAndReportsPassFailAndMissingSource() throws Exception {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));
        Path expected = writeExpected("expected\n");

        createZip(submissions.resolve("pass.zip"), "submission/src/Main.txt", "source");
        createZip(submissions.resolve("wrong.zip"), "submission/src/Main.txt", "source");
        createZip(submissions.resolve("missing.zip"), "submission/readme.txt", "no source");

        AssignmentRunner runner = newRunner(Map.of(
                "pass", new ProcessResult(0, "expected\n", "", false, 5),
                "wrong", new ProcessResult(0, "wrong\n", "", false, 5)
        ));

        List<StudentResult> results = runner.runSync(
                savedProject(submissions, working),
                interpretedConfiguration(),
                List.of(testCase(expected))
        );

        Map<String, StudentResult> byStudent = resultsByStudent(results);

        assertEquals(3, results.size());
        assertEquals(ResultStatus.PASS, byStudent.get("pass").getTestStatus());
        assertEquals(ResultStatus.FAIL, byStudent.get("wrong").getTestStatus());
        assertEquals(ResultStatus.MISSING_SOURCE, byStudent.get("missing").getCompileStatus());
        assertEquals(ResultStatus.ERROR, byStudent.get("missing").getTestStatus());
    }

    @Test
    void runSyncMarksMissingExpectedOutputFileAsError() throws Exception {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));
        Path missingExpected = tempDir.resolve("missing-expected.txt");

        createZip(submissions.resolve("pass.zip"), "Main.txt", "source");

        AssignmentRunner runner = newRunner(Map.of(
                "pass", new ProcessResult(0, "expected\n", "", false, 5)
        ));

        StudentResult result = runner.runSync(
                savedProject(submissions, working),
                interpretedConfiguration(),
                List.of(testCase(missingExpected))
        ).get(0);

        assertEquals(ResultStatus.SUCCESS, result.getRunStatus());
        assertEquals(ResultStatus.ERROR, result.getTestStatus());
        assertTrue(result.getTestDetails().contains("Expected output file not found"));
    }

    @Test
    void runSyncMarksRuntimeErrorAndKeepsStderr() throws Exception {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));
        Path expected = writeExpected("expected\n");

        createZip(submissions.resolve("runtime.zip"), "Main.txt", "source");

        AssignmentRunner runner = newRunner(Map.of(
                "runtime", new ProcessResult(7, "partial\n", "boom\n", false, 5)
        ));

        StudentResult result = runner.runSync(
                savedProject(submissions, working),
                interpretedConfiguration(),
                List.of(testCase(expected))
        ).get(0);

        assertEquals(ResultStatus.RUNTIME_ERROR, result.getRunStatus());
        assertEquals(ResultStatus.ERROR, result.getTestStatus());
        assertTrue(result.getErrorOutput().contains("boom"));
        assertTrue(result.getTestDetails().contains("Program exited with code 7"));
    }

    @Test
    void runSyncMarksTimeoutAsErrorStatus() throws Exception {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));
        Path expected = writeExpected("expected\n");

        createZip(submissions.resolve("timeout.zip"), "Main.txt", "source");

        AssignmentRunner runner = newRunner(Map.of(
                "timeout", new ProcessResult(-1, "partial\n", "late\n", true, 1000)
        ));

        StudentResult result = runner.runSync(
                savedProject(submissions, working),
                interpretedConfiguration(),
                List.of(testCase(expected))
        ).get(0);

        assertEquals(ResultStatus.TIMEOUT, result.getRunStatus());
        assertEquals(ResultStatus.ERROR, result.getTestStatus());
        assertTrue(result.getErrorOutput().contains("late"));
    }

    private AssignmentRunner newRunner(Map<String, ProcessResult> processResults) {
        return new AssignmentRunner(
                new ZipProcessor(),
                new StubProcessExecutor(processResults),
                new OutputComparator(),
                projectService
        );
    }

    private Project savedProject(Path submissions, Path working) throws SQLException {
        Configuration savedConfig = configurationService.save(interpretedConfiguration());

        Project project = new Project();
        project.setName("Execution Test Project");
        project.setConfigurationId(savedConfig.getId());
        project.setSubmissionsDirectory(submissions.toString());
        project.setWorkingDirectory(working.toString());
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        return projectService.save(project);
    }

    private Configuration interpretedConfiguration() {
        Configuration config = new Configuration();
        config.setName("Interpreted Test Config " + System.nanoTime());
        config.setRequiresCompilation(false);
        config.setSourceFileName("Main.txt");
        config.setRunCommand("stub {source} {args}");
        config.setFileExtension(".txt");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }

    private TestCase testCase(Path expectedOutputFile) {
        TestCase testCase = new TestCase();
        testCase.setName("case1");
        testCase.setExpectedOutputFile(expectedOutputFile.toString());
        testCase.setOrderIndex(0);
        return testCase;
    }

    private Path writeExpected(String content) throws IOException {
        Path expected = tempDir.resolve("expected-" + System.nanoTime() + ".txt");
        Files.writeString(expected, content, StandardCharsets.UTF_8);
        return expected;
    }

    private Map<String, StudentResult> resultsByStudent(List<StudentResult> results) {
        return results.stream()
                .collect(Collectors.toMap(StudentResult::getStudentId, Function.identity()));
    }

    private void createZip(Path zipPath, String entryName, String content) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    private static class StubProcessExecutor extends ProcessExecutor {
        private final Map<String, ProcessResult> processResults;

        private StubProcessExecutor(Map<String, ProcessResult> processResults) {
            this.processResults = processResults;
        }

        @Override
        public ProcessResult execute(List<String> commandParts, File workingDir) throws IOException {
            String normalizedPath = workingDir.getAbsolutePath().replace('\\', '/');

            for (Map.Entry<String, ProcessResult> entry : processResults.entrySet()) {
                String marker = "/" + entry.getKey() + "/";
                if (normalizedPath.contains(marker) || normalizedPath.endsWith("/" + entry.getKey())) {
                    return entry.getValue();
                }
            }

            throw new IOException("No stubbed process result for " + workingDir.getAbsolutePath());
        }
    }
}
