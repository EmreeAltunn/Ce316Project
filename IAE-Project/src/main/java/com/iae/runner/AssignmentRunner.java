package com.iae.runner;

import com.iae.db.DatabaseManager;
import com.iae.model.ComparisonResult;
import com.iae.model.Configuration;
import com.iae.model.ProcessResult;
import com.iae.model.Project;
import com.iae.model.ResultStatus;
import com.iae.model.StudentResult;
import com.iae.model.TestCase;
import com.iae.process.ProcessExecutor;
import com.iae.process.ZipProcessor;
import com.iae.service.ProjectService;
import com.iae.service.StudentResultService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AssignmentRunner {

    private final ZipProcessor zipProcessor;
    private final ProcessExecutor processExecutor;
    private final OutputComparator outputComparator;
    private final ProjectService projectService;

    public AssignmentRunner(ZipProcessor zipProcessor,
                            ProcessExecutor processExecutor,
                            OutputComparator outputComparator,
                            ProjectService projectService) {
        this.zipProcessor = zipProcessor;
        this.processExecutor = processExecutor;
        this.outputComparator = outputComparator;
        this.projectService = projectService;
    }

    public void runAsync(Project project,
                         Configuration config,
                         List<TestCase> testCases,
                         RunnerCallback callback) {

        Thread runnerThread = new Thread(() -> {
            List<StudentResult> results = new ArrayList<>();

            try {
                File submissionsDir = new File(project.getSubmissionsDirectory());
                File[] zipFiles = listZipFiles(submissionsDir);
                int total = zipFiles.length;

                for (int i = 0; i < zipFiles.length; i++) {
                    File zipFile = zipFiles[i];
                    String studentId = zipProcessor.getStudentIdFromZip(zipFile);
                    int current = i + 1;

                    runOnUiThread(() -> callback.onStudentStarted(studentId));

                    try {
                        StudentResult result = processStudent(zipFile, config, testCases, project);
                        saveStudentResult(result);
                        results.add(result);

                        runOnUiThread(() -> callback.onStudentCompleted(result));
                    } catch (Exception e) {
                        StudentResult errorResult = createErrorResult(
                                project,
                                zipFile,
                                studentId,
                                e.getMessage()
                        );

                        saveStudentResult(errorResult);
                        results.add(errorResult);

                        runOnUiThread(() -> callback.onStudentError(studentId, e.getMessage()));
                    }

                    runOnUiThread(() -> callback.onProgress(current, total));
                }

                runOnUiThread(() -> callback.onAllCompleted(results));
            } catch (Exception e) {
                runOnUiThread(() -> callback.onStudentError("GENERAL", e.getMessage()));
                runOnUiThread(() -> callback.onAllCompleted(results));
            }
        });

        runnerThread.setName("IAE-AssignmentRunner");
        runnerThread.setDaemon(true);
        runnerThread.start();
    }

    public List<StudentResult> runSync(Project project,
                                       Configuration config,
                                       List<TestCase> testCases) throws Exception {

        List<StudentResult> results = new ArrayList<>();
        File submissionsDir = new File(project.getSubmissionsDirectory());
        File[] zipFiles = listZipFiles(submissionsDir);

        for (File zipFile : zipFiles) {
            String studentId = zipProcessor.getStudentIdFromZip(zipFile);
            StudentResult result;

            try {
                result = processStudent(zipFile, config, testCases, project);
            } catch (Exception e) {
                result = createErrorResult(project, zipFile, studentId, e.getMessage());
            }

            saveStudentResult(result);
            results.add(result);
        }

        return results;
    }

    private StudentResult processStudent(File zipFile,
                                         Configuration config,
                                         List<TestCase> testCases,
                                         Project project) {

        String studentId = zipProcessor.getStudentIdFromZip(zipFile);

        StudentResult result = new StudentResult();
        result.setProjectId(project.getId());
        result.setStudentId(studentId);
        result.setZipFilePath(zipFile.getAbsolutePath());
        result.setCompileStatus(ResultStatus.PENDING);
        result.setRunStatus(ResultStatus.PENDING);
        result.setTestStatus(ResultStatus.PENDING);
        result.setProcessedAt(LocalDateTime.now());

        try {
            File studentDir = zipProcessor.extractSingle(
                    zipFile,
                    new File(project.getWorkingDirectory())
            );

            if (config.isRequiresCompilation()) {
                compile(result, config, studentDir);
            } else {
                result.setCompileStatus(ResultStatus.SKIPPED);
                result.setCompileOutput("Compilation skipped for interpreted language.");
            }

            if (result.getCompileStatus() == ResultStatus.FAILED) {
                result.setRunStatus(ResultStatus.SKIPPED);
                result.setTestStatus(ResultStatus.SKIPPED);
                result.setProcessedAt(LocalDateTime.now());
                return result;
            }

            if (testCases == null || testCases.isEmpty()) {
                result.setRunStatus(ResultStatus.SKIPPED);
                result.setTestStatus(ResultStatus.SKIPPED);
                result.setTestDetails("No test cases were provided.");
                result.setProcessedAt(LocalDateTime.now());
                return result;
            }

            runTestCases(result, testCases, config, studentDir);
        } catch (Exception e) {
            if (result.getCompileStatus() == ResultStatus.PENDING) {
                result.setCompileStatus(ResultStatus.ERROR);
            }

            result.setRunStatus(ResultStatus.ERROR);
            result.setTestStatus(ResultStatus.ERROR);
            result.setErrorOutput(e.getMessage());
            result.setTestDetails("Processing error: " + e.getMessage());
        }

        result.setProcessedAt(LocalDateTime.now());
        return result;
    }

    private void compile(StudentResult result,
                         Configuration config,
                         File studentDir) {

        try {
            if (isBlank(config.getCompilerPath())) {
                result.setCompileStatus(ResultStatus.FAILED);
                result.setCompileError("Compiler path is empty.");
                return;
            }

            String outputFileName = !isBlank(config.getOutputFileName())
                    ? config.getOutputFileName()
                    : "output";

            String compilerArgs = config.getCompilerArgs() == null
                    ? ""
                    : config.getCompilerArgs();

            compilerArgs = compilerArgs
                    .replace("{source}", config.getSourceFileName())
                    .replace("{output}", outputFileName)
                    .replace("{executable}", outputFileName)
                    .replace("{args}", "");

            List<String> commandParts = new ArrayList<>();
            commandParts.add(config.getCompilerPath());
            commandParts.addAll(splitCommand(compilerArgs));

            ProcessResult processResult = processExecutor.execute(commandParts, studentDir);

            result.setCompileOutput(processResult.getStdout());
            result.setCompileError(processResult.getStderr());

            if (processResult.isTimedOut()) {
                result.setCompileStatus(ResultStatus.FAILED);
                result.setCompileError(
                        appendLine(result.getCompileError(), "Compilation timed out.")
                );
            } else {
                result.setCompileStatus(
                        processResult.getExitCode() == 0
                                ? ResultStatus.SUCCESS
                                : ResultStatus.FAILED
                );
            }
        } catch (IOException e) {
            result.setCompileStatus(ResultStatus.FAILED);
            result.setCompileError(e.getMessage());
        }
    }

    private void runTestCases(StudentResult result,
                              List<TestCase> testCases,
                              Configuration config,
                              File studentDir) {

        List<TestCase> orderedTestCases = new ArrayList<>(testCases);
        orderedTestCases.sort(Comparator.comparingInt(TestCase::getOrderIndex));

        StringBuilder allProgramOutput = new StringBuilder();
        StringBuilder allErrorOutput = new StringBuilder();
        StringBuilder details = new StringBuilder();

        boolean anyRunFailed = false;
        boolean anyTestFailed = false;
        boolean atLeastOneCompared = false;

        for (TestCase testCase : orderedTestCases) {
            SingleTestOutcome outcome = runSingleTestCase(testCase, config, studentDir);

            if (!isBlank(outcome.stdout)) {
                allProgramOutput
                        .append("--- ")
                        .append(testCase.getName())
                        .append(" stdout ---\n")
                        .append(outcome.stdout)
                        .append('\n');
            }

            if (!isBlank(outcome.stderr)) {
                allErrorOutput
                        .append("--- ")
                        .append(testCase.getName())
                        .append(" stderr ---\n")
                        .append(outcome.stderr)
                        .append('\n');
            }

            if (outcome.runStatus != ResultStatus.SUCCESS) {
                anyRunFailed = true;
            }

            if (outcome.testStatus == ResultStatus.FAIL
                    || outcome.testStatus == ResultStatus.ERROR) {
                anyTestFailed = true;
            }

            if (outcome.testStatus == ResultStatus.PASS
                    || outcome.testStatus == ResultStatus.FAIL) {
                atLeastOneCompared = true;
            }

            details
                    .append(testCase.getName())
                    .append(": ")
                    .append(outcome.testStatus);

            if (!isBlank(outcome.details)) {
                details
                        .append("\n")
                        .append(outcome.details);
            }

            details.append("\n");
        }

        result.setProgramOutput(allProgramOutput.toString().trim());
        result.setErrorOutput(allErrorOutput.toString().trim());

        result.setRunStatus(anyRunFailed ? ResultStatus.FAILED : ResultStatus.SUCCESS);

        if (anyRunFailed) {
            result.setTestStatus(ResultStatus.ERROR);
        } else if (!atLeastOneCompared) {
            result.setTestStatus(ResultStatus.SKIPPED);
        } else if (anyTestFailed) {
            result.setTestStatus(ResultStatus.FAIL);
        } else {
            result.setTestStatus(ResultStatus.PASS);
        }

        result.setTestDetails(details.toString().trim());
    }

    private SingleTestOutcome runSingleTestCase(TestCase testCase,
                                                Configuration config,
                                                File studentDir) {

        SingleTestOutcome outcome = new SingleTestOutcome();

        try {
            String executable = !isBlank(config.getOutputFileName())
                    ? config.getOutputFileName()
                    : config.getSourceFileName();

            String inputArgs = testCase.getInputArgs() != null
                    ? testCase.getInputArgs()
                    : "";

            String command = config.getRunCommand()
                    .replace("{executable}", executable)
                    .replace("{args}", inputArgs)
                    .replace("{source}", config.getSourceFileName())
                    .replace("{output}", executable);

            ProcessResult processResult = processExecutor.execute(
                    splitCommand(command),
                    studentDir
            );

            outcome.stdout = processResult.getStdout();
            outcome.stderr = processResult.getStderr();

            if (processResult.isTimedOut()) {
                outcome.runStatus = ResultStatus.FAILED;
                outcome.testStatus = ResultStatus.ERROR;
                outcome.details = "Execution timed out.";
                return outcome;
            }

            outcome.runStatus = processResult.getExitCode() == 0
                    ? ResultStatus.SUCCESS
                    : ResultStatus.FAILED;

            if (outcome.runStatus == ResultStatus.FAILED) {
                outcome.testStatus = ResultStatus.ERROR;
                outcome.details = "Program exited with code " + processResult.getExitCode();
                return outcome;
            }

            if (!isBlank(testCase.getExpectedOutputFile())) {
                ComparisonResult comparisonResult = outputComparator.compareWithFile(
                        processResult.getStdout(),
                        new File(testCase.getExpectedOutputFile())
                );

                outcome.testStatus = comparisonResult.isMatch()
                        ? ResultStatus.PASS
                        : ResultStatus.FAIL;

                outcome.details = comparisonResult.getDifferences().isEmpty()
                        ? "Output matches expected output."
                        : String.join("\n", comparisonResult.getDifferences());
            } else {
                outcome.testStatus = ResultStatus.SKIPPED;
                outcome.details = "Expected output file was not provided.";
            }
        } catch (Exception e) {
            outcome.runStatus = ResultStatus.FAILED;
            outcome.testStatus = ResultStatus.ERROR;
            outcome.stderr = appendLine(outcome.stderr, e.getMessage());
            outcome.details = e.getMessage();
        }

        return outcome;
    }

    private File[] listZipFiles(File submissionsDir) throws IOException {
        if (submissionsDir == null
                || !submissionsDir.exists()
                || !submissionsDir.isDirectory()) {

            throw new IOException(
                    "Submissions directory not found: "
                            + (submissionsDir == null
                            ? "null"
                            : submissionsDir.getAbsolutePath())
            );
        }

        File[] zipFiles = submissionsDir.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".zip")
        );

        if (zipFiles == null) {
            return new File[0];
        }

        Arrays.sort(zipFiles, Comparator.comparing(File::getName));
        return zipFiles;
    }

    private void saveStudentResult(StudentResult result) throws Exception {
        try {
            Method saveMethod = projectService
                    .getClass()
                    .getMethod("saveStudentResult", StudentResult.class);

            saveMethod.invoke(projectService, result);
            return;
        } catch (NoSuchMethodException ignored) {
            // Some versions of the project use StudentResultService separately.
        }

        Field dbManagerField = projectService
                .getClass()
                .getDeclaredField("dbManager");

        dbManagerField.setAccessible(true);

        DatabaseManager dbManager = (DatabaseManager) dbManagerField.get(projectService);
        StudentResultService studentResultService = new StudentResultService(dbManager);

        studentResultService.save(result);
    }

    private StudentResult createErrorResult(Project project,
                                            File zipFile,
                                            String studentId,
                                            String errorMessage) {

        StudentResult result = new StudentResult();

        result.setProjectId(project.getId());
        result.setStudentId(studentId);
        result.setZipFilePath(zipFile != null ? zipFile.getAbsolutePath() : "");
        result.setCompileStatus(ResultStatus.ERROR);
        result.setCompileError(errorMessage);
        result.setRunStatus(ResultStatus.ERROR);
        result.setErrorOutput(errorMessage);
        result.setTestStatus(ResultStatus.ERROR);
        result.setTestDetails(errorMessage);
        result.setProcessedAt(LocalDateTime.now());

        return result;
    }

    private void runOnUiThread(Runnable action) {
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            Method runLaterMethod = platformClass.getMethod("runLater", Runnable.class);
            runLaterMethod.invoke(null, action);
        } catch (Exception ignored) {
            action.run();
        }
    }

    private List<String> splitCommand(String command) {
        List<String> parts = new ArrayList<>();

        if (command == null) {
            return parts;
        }

        String trimmed = command.trim();

        if (trimmed.isEmpty()) {
            return parts;
        }

        parts.addAll(Arrays.asList(trimmed.split("\\s+")));
        return parts;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String appendLine(String current, String line) {
        if (isBlank(current)) {
            return line;
        }

        return current + System.lineSeparator() + line;
    }

    private static class SingleTestOutcome {
        private ResultStatus runStatus = ResultStatus.PENDING;
        private ResultStatus testStatus = ResultStatus.PENDING;
        private String stdout = "";
        private String stderr = "";
        private String details = "";
    }
}