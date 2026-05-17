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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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

            File sourceFile = locateSourceFile(studentDir, config.getSourceFileName());

            if (sourceFile == null) {
                markMissingSource(result, config, studentDir);
                return result;
            }

            File executionDir = sourceFile.getParentFile() != null
                    ? sourceFile.getParentFile()
                    : studentDir;

            if (shouldCompile(config)) {
                compile(result, config, executionDir, sourceFile);
            } else {
                result.setCompileStatus(ResultStatus.SKIPPED);
                result.setCompileOutput(getCompileSkipMessage(config));
            }

            if (isCompileBlocking(result.getCompileStatus())) {
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

            runTestCases(result, testCases, config, executionDir, sourceFile);
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
                         File executionDir,
                         File sourceFile) {

        try {
            if (isBlank(config.getCompilerPath())) {
                result.setCompileStatus(ResultStatus.SKIPPED);
                result.setCompileOutput(getCompileSkipMessage(config));
                return;
            }

            String compilerArgs = config.getCompilerArgs() == null
                    ? ""
                    : config.getCompilerArgs();

            compilerArgs = applyCommandPlaceholders(
                    compilerArgs,
                    config,
                    sourceFile,
                    ""
            );

            List<String> commandParts = new ArrayList<>();
            commandParts.add(config.getCompilerPath());
            commandParts.addAll(splitCommand(compilerArgs));

            ProcessResult processResult = processExecutor.execute(commandParts, executionDir);

            result.setCompileOutput(appendLine(
                    processResult.getStdout(),
                    "Exit code: " + processResult.getExitCode()
            ));
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

                if (result.getCompileStatus() == ResultStatus.FAILED
                        && isBlank(result.getCompileError())) {
                    result.setCompileError("Compilation failed with exit code " + processResult.getExitCode() + ".");
                }
            }
        } catch (Exception e) {
            result.setCompileStatus(ResultStatus.FAILED);
            result.setCompileError("Could not start compiler: " + e.getMessage());
        }
    }

    private void runTestCases(StudentResult result,
                              List<TestCase> testCases,
                              Configuration config,
                              File executionDir,
                              File sourceFile) {

        List<TestCase> orderedTestCases = new ArrayList<>(testCases);
        orderedTestCases.sort(Comparator.comparingInt(TestCase::getOrderIndex));

        StringBuilder allProgramOutput = new StringBuilder();
        StringBuilder allErrorOutput = new StringBuilder();
        StringBuilder details = new StringBuilder();

        boolean anyRunFailed = false;
        boolean anyTestFailed = false;
        boolean atLeastOneCompared = false;

        for (TestCase testCase : orderedTestCases) {
            SingleTestOutcome outcome = runSingleTestCase(testCase, config, executionDir, sourceFile);

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
                                                File executionDir,
                                                File sourceFile) {

        SingleTestOutcome outcome = new SingleTestOutcome();

        try {
            if (isBlank(config.getRunCommand())) {
                outcome.runStatus = ResultStatus.FAILED;
                outcome.testStatus = ResultStatus.ERROR;
                outcome.details = "Run command is empty.";
                return outcome;
            }

            String inputArgs = testCase.getInputArgs() != null
                    ? testCase.getInputArgs()
                    : "";

            String command = applyCommandPlaceholders(
                    config.getRunCommand(),
                    config,
                    sourceFile,
                    inputArgs
            );

            List<String> commandParts = splitCommand(command);

            if (commandParts.isEmpty()) {
                outcome.runStatus = ResultStatus.FAILED;
                outcome.testStatus = ResultStatus.ERROR;
                outcome.details = "Run command is empty after applying arguments.";
                return outcome;
            }

            ProcessResult processResult = processExecutor.execute(
                    commandParts,
                    executionDir
            );

            outcome.stdout = processResult.getStdout();
            outcome.stderr = processResult.getStderr();
            outcome.exitCode = processResult.getExitCode();

            if (processResult.isTimedOut()) {
                outcome.runStatus = ResultStatus.FAILED;
                outcome.testStatus = ResultStatus.ERROR;
                outcome.details = withExitCode("Execution timed out.", processResult.getExitCode());
                return outcome;
            }

            outcome.runStatus = processResult.getExitCode() == 0
                    ? ResultStatus.SUCCESS
                    : ResultStatus.FAILED;

            if (outcome.runStatus == ResultStatus.FAILED) {
                outcome.testStatus = ResultStatus.ERROR;
                outcome.details = withExitCode(
                        "Program exited with code " + processResult.getExitCode() + ".",
                        processResult.getExitCode()
                );
                return outcome;
            }

            if (!isBlank(testCase.getExpectedOutputFile())) {
                File expectedOutputFile = new File(testCase.getExpectedOutputFile());

                if (!expectedOutputFile.exists() || !expectedOutputFile.isFile()) {
                    outcome.testStatus = ResultStatus.ERROR;
                    outcome.details = withExitCode(
                            "Expected output file not found: " + expectedOutputFile.getAbsolutePath(),
                            processResult.getExitCode()
                    );
                    return outcome;
                }

                ComparisonResult comparisonResult = outputComparator.compareWithFile(
                        processResult.getStdout(),
                        expectedOutputFile
                );

                outcome.testStatus = comparisonResult.isMatch()
                        ? ResultStatus.PASS
                        : ResultStatus.FAIL;

                String comparisonDetails = comparisonResult.getDifferences().isEmpty()
                        ? "Output matches expected output."
                        : String.join("\n", comparisonResult.getDifferences());

                outcome.details = withExitCode(comparisonDetails, processResult.getExitCode());
            } else {
                outcome.testStatus = ResultStatus.SKIPPED;
                outcome.details = withExitCode(
                        "Expected output file was not provided.",
                        processResult.getExitCode()
                );
            }
        } catch (Exception e) {
            outcome.runStatus = ResultStatus.FAILED;
            outcome.testStatus = ResultStatus.ERROR;
            outcome.stderr = appendLine(outcome.stderr, e.getMessage());
            outcome.details = e.getMessage();
        }

        return outcome;
    }

    private File locateSourceFile(File studentDir, String sourceFileName) throws IOException {
        if (studentDir == null || isBlank(sourceFileName)) {
            return null;
        }

        File directMatch = new File(studentDir, sourceFileName);
        if (directMatch.exists() && directMatch.isFile()) {
            return directMatch;
        }

        String requiredFileName = new File(sourceFileName).getName();
        List<File> matches = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(studentDir.toPath())) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(requiredFileName))
                    .map(Path::toFile)
                    .forEach(matches::add);
        }

        if (matches.isEmpty()) {
            return null;
        }

        matches.sort(Comparator.comparing(File::getAbsolutePath));
        return matches.get(0);
    }

    private void markMissingSource(StudentResult result,
                                   Configuration config,
                                   File studentDir) {

        String message = "Required source file not found: "
                + config.getSourceFileName()
                + " under "
                + studentDir.getAbsolutePath();

        if (shouldCompile(config)) {
            result.setCompileStatus(ResultStatus.FAILED);
            result.setCompileError(message);
        } else {
            result.setCompileStatus(ResultStatus.SKIPPED);
            result.setCompileOutput(getCompileSkipMessage(config));
        }

        result.setRunStatus(ResultStatus.SKIPPED);
        result.setErrorOutput(message);
        result.setTestStatus(ResultStatus.ERROR);
        result.setTestDetails(message);
        result.setProcessedAt(LocalDateTime.now());
    }

    private boolean shouldCompile(Configuration config) {
        return config != null
                && config.isRequiresCompilation()
                && !isBlank(config.getCompilerPath());
    }

    private boolean isCompileBlocking(ResultStatus compileStatus) {
        return compileStatus != ResultStatus.SUCCESS
                && compileStatus != ResultStatus.SKIPPED;
    }

    private String getCompileSkipMessage(Configuration config) {
        if (config != null && config.isRequiresCompilation()) {
            return "Compilation skipped because no compiler command is configured.";
        }

        return "Compilation skipped for interpreted language.";
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

        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);

            if (inQuote) {
                if (ch == quoteChar) {
                    inQuote = false;
                } else {
                    current.append(ch);
                }
                continue;
            }

            if (ch == '"' || ch == '\'') {
                inQuote = true;
                quoteChar = ch;
            } else if (Character.isWhitespace(ch)) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts;
    }

    private String applyCommandPlaceholders(String command,
                                            Configuration config,
                                            File sourceFile,
                                            String args) {

        if (command == null) {
            return "";
        }

        String sourceName = sourceFile != null
                ? sourceFile.getName()
                : valueOrEmpty(config.getSourceFileName());
        String sourcePath = sourceFile != null
                ? sourceFile.getPath()
                : sourceName;
        String sourceAbsolutePath = sourceFile != null
                ? sourceFile.getAbsolutePath()
                : sourcePath;
        String sourceBaseName = removeExtension(sourceName);
        String executable = getExecutableName(config, sourceFile);
        String safeArgs = args == null ? "" : args;

        return command
                .replace("{source}", sourceName)
                .replace("{sourcePath}", sourcePath)
                .replace("{sourceAbsolute}", sourceAbsolutePath)
                .replace("{sourceBase}", sourceBaseName)
                .replace("{output}", executable)
                .replace("{executable}", executable)
                .replace("{args}", safeArgs);
    }

    private String getExecutableName(Configuration config, File sourceFile) {
        if (config != null && !isBlank(config.getOutputFileName())) {
            return config.getOutputFileName();
        }

        if (shouldCompile(config)) {
            return "output";
        }

        return sourceFile != null
                ? sourceFile.getName()
                : valueOrEmpty(config.getSourceFileName());
    }

    private String removeExtension(String fileName) {
        if (isBlank(fileName)) {
            return "";
        }

        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart <= 0) {
            return fileName;
        }

        return fileName.substring(0, extensionStart);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String withExitCode(String details, int exitCode) {
        return appendLine("Exit code: " + exitCode, details);
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
        private int exitCode = 0;
        private String stdout = "";
        private String stderr = "";
        private String details = "";
    }
}
