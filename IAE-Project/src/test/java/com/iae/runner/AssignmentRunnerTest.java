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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        String wrongDetails = byStudent.get("wrong").getTestDetails();
        assertTrue(wrongDetails.startsWith("case1: FAIL"));
        assertTrue(wrongDetails.contains("Failed comparisons:"));
        assertTrue(wrongDetails.contains("- case1: expected \"expected\", got \"wrong\""));
        assertFalse(wrongDetails.contains("Exit code: 0"));
    }

    @Test
    void runSyncPassesWhenAllTestCasesPassAndDetailsOnlyShowSummary() throws Exception {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));
        Path expectedEmre = writeExpected("Hello Emre\n");
        Path expectedAli = writeExpected("Hello Ali\n");
        Path expectedMissingArg = writeExpected("Missing argument\n");

        createZip(submissions.resolve("100001.zip"), "Main.txt", "source");

        AssignmentRunner runner = newRunner(Map.of(
                "100001|emre", new ProcessResult(0, "Hello Emre\n", "", false, 5),
                "100001|ali", new ProcessResult(0, "Hello Ali\n", "", false, 5),
                "100001|missing", new ProcessResult(0, "Missing argument\n", "", false, 5)
        ));

        StudentResult result = runner.runSync(
                savedProject(submissions, working),
                interpretedConfiguration(),
                List.of(
                        testCase("hello-emre", "emre", expectedEmre),
                        testCase("hello-ali", "ali", expectedAli),
                        testCase("missing-arg", "missing", expectedMissingArg)
                )
        ).get(0);

        assertEquals(ResultStatus.SUCCESS, result.getRunStatus());
        assertEquals(ResultStatus.PASS, result.getTestStatus());
        assertEquals(
                "hello-emre: PASS; hello-ali: PASS; missing-arg: PASS",
                result.getTestDetails()
        );
    }

    @Test
    void runSyncAggregatesPartialFailAcrossMultipleTestCases() throws Exception {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));
        Path expectedEmre = writeExpected("Hello Emre\n");
        Path expectedAli = writeExpected("Hello Ali\n");
        Path expectedMissingArg = writeExpected("Missing argument\n");

        createZip(submissions.resolve("100002.zip"), "Main.txt", "source");

        AssignmentRunner runner = newRunner(Map.of(
                "100002|emre", new ProcessResult(0, "Hello Emre\n", "", false, 5),
                "100002|ali", new ProcessResult(0, "Wrong Ali\n", "", false, 5),
                "100002|missing", new ProcessResult(0, "Missing argument\n", "", false, 5)
        ));

        StudentResult result = runner.runSync(
                savedProject(submissions, working),
                interpretedConfiguration(),
                List.of(
                        testCase("hello-emre", "emre", expectedEmre),
                        testCase("hello-ali", "ali", expectedAli),
                        testCase("missing-arg", "missing", expectedMissingArg)
                )
        ).get(0);

        assertEquals(ResultStatus.SUCCESS, result.getRunStatus());
        assertEquals(ResultStatus.FAIL, result.getTestStatus());
        assertTrue(result.getTestDetails().startsWith(
                "hello-emre: PASS; hello-ali: FAIL; missing-arg: PASS"
        ));
        assertTrue(result.getTestDetails().contains("Failed comparisons:"));
        assertTrue(result.getTestDetails().contains("- hello-ali: expected \"Hello Ali\", got \"Wrong Ali\""));
        assertFalse(result.getTestDetails().contains("Exit code: 0"));
    }

    @Test
    void runSyncIsolatesWorkingDirectoryBetweenTestCases() throws Exception {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));
        Path expected = writeExpected("clean\n");

        createZip(submissions.resolve("isolated.zip"), "Main.txt", "source");

        AssignmentRunner runner = new AssignmentRunner(
                new ZipProcessor(),
                new StateWritingProcessExecutor(),
                new OutputComparator(),
                projectService
        );

        StudentResult result = runner.runSync(
                savedProject(submissions, working),
                interpretedConfiguration(),
                List.of(
                        testCase("first", "one", expected),
                        testCase("second", "two", expected)
                )
        ).get(0);

        assertEquals(ResultStatus.SUCCESS, result.getRunStatus());
        assertEquals(ResultStatus.PASS, result.getTestStatus());
        assertEquals("first: PASS; second: PASS", result.getTestDetails());
    }

    @Test
    void runSyncShowsCompilerOutputWhenCompilationFails() throws Exception {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));
        Path expected = writeExpected("expected\n");

        createZip(submissions.resolve("compile.zip"), "Main.txt", "source");

        AssignmentRunner runner = newRunner(Map.of(
                "compile", new ProcessResult(1, "", "compiler exploded\nline 4\n", false, 5)
        ));

        StudentResult result = runner.runSync(
                savedProject(submissions, working),
                compiledConfiguration(),
                List.of(testCase(expected))
        ).get(0);

        assertEquals(ResultStatus.COMPILE_ERROR, result.getCompileStatus());
        assertEquals(ResultStatus.SKIPPED, result.getRunStatus());
        assertEquals(ResultStatus.ERROR, result.getTestStatus());
        assertTrue(result.getTestDetails().startsWith("COMPILE_ERROR"));
        assertTrue(result.getTestDetails().contains("compiler exploded"));
    }

    @Test
    void runSyncGroupsRepeatedRuntimeErrorsAcrossTestCases() throws Exception {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));
        Path expected = writeExpected("expected\n");

        createZip(submissions.resolve("runtime.zip"), "Main.txt", "source");

        ProcessResult runtimeError = new ProcessResult(1, "", "Intentional runtime error\n", false, 5);
        AssignmentRunner runner = newRunner(Map.of(
                "runtime|emre", runtimeError,
                "runtime|ali", runtimeError,
                "runtime|missing", runtimeError
        ));

        StudentResult result = runner.runSync(
                savedProject(submissions, working),
                interpretedConfiguration(),
                List.of(
                        testCase("hello-emre", "emre", expected),
                        testCase("hello-ali", "ali", expected),
                        testCase("missing-arg", "missing", expected)
                )
        ).get(0);

        assertEquals(ResultStatus.RUNTIME_ERROR, result.getRunStatus());
        assertEquals(ResultStatus.ERROR, result.getTestStatus());
        assertTrue(result.getTestDetails().startsWith(
                "hello-emre: ERROR; hello-ali: ERROR; missing-arg: ERROR"
        ));
        assertTrue(result.getTestDetails().contains(
                "Runtime error in 3 test(s): hello-emre, hello-ali, missing-arg"
        ));
        assertEquals(1, countOccurrences(result.getTestDetails(), "Intentional runtime error"));
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
        assertTrue(result.getTestDetails().contains("Runtime error in 1 test(s): case1"));
        assertTrue(result.getTestDetails().contains("Exit code: 7"));
        assertTrue(result.getTestDetails().contains("boom"));
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

    private Configuration compiledConfiguration() {
        Configuration config = interpretedConfiguration();
        config.setRequiresCompilation(true);
        config.setCompilerPath("stub-compiler");
        config.setCompilerArgs("{source}");
        config.setOutputFileName("Main");
        return config;
    }

    private TestCase testCase(Path expectedOutputFile) {
        return testCase("case1", "", expectedOutputFile);
    }

    private TestCase testCase(String name, String inputArgs, Path expectedOutputFile) {
        TestCase testCase = new TestCase();
        testCase.setName(name);
        testCase.setInputArgs(inputArgs);
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

    private int countOccurrences(String text, String value) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(value, index)) >= 0) {
            count++;
            index += value.length();
        }

        return count;
    }

    private static class StubProcessExecutor extends ProcessExecutor {
        private final Map<String, ProcessResult> processResults;

        private StubProcessExecutor(Map<String, ProcessResult> processResults) {
            this.processResults = processResults;
        }

        @Override
        public ProcessResult execute(List<String> commandParts, File workingDir) throws IOException {
            String normalizedPath = workingDir.getAbsolutePath().replace('\\', '/');
            String command = String.join(" ", commandParts);

            for (Map.Entry<String, ProcessResult> entry : processResults.entrySet()) {
                String key = entry.getKey();
                String studentId = key.contains("|") ? key.substring(0, key.indexOf('|')) : key;
                String inputArg = key.contains("|") ? key.substring(key.indexOf('|') + 1) : "";
                String marker = "/" + studentId + "/";
                String testRunsMarker = "/" + studentId + "-test-runs/";

                if ((normalizedPath.contains(marker)
                        || normalizedPath.endsWith("/" + studentId)
                        || normalizedPath.contains(testRunsMarker))
                        && (inputArg.isEmpty() || command.contains(inputArg))) {
                    return entry.getValue();
                }
            }

            throw new IOException("No stubbed process result for " + workingDir.getAbsolutePath());
        }
    }

    private static class StateWritingProcessExecutor extends ProcessExecutor {
        @Override
        public ProcessResult execute(List<String> commandParts, File workingDir) throws IOException {
            Path state = workingDir.toPath().resolve("state.txt");

            if (Files.exists(state)) {
                return new ProcessResult(0, "dirty\n", "", false, 5);
            }

            Files.writeString(state, "created", StandardCharsets.UTF_8);
            return new ProcessResult(0, "clean\n", "", false, 5);
        }
    }
}
