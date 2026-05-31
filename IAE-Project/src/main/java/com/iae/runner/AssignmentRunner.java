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
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class AssignmentRunner {

    private final ZipProcessor zipProcessor;
    private final ProcessExecutor processExecutor;
    private final OutputComparator outputComparator;
    private final ProjectService projectService;
    private volatile boolean cancelRequested;

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

        cancelRequested = false;

        Thread runnerThread = new Thread(() -> {
            List<StudentResult> results = new ArrayList<>();

            try {
                if (project.getId() > 0) {
                    try {
                        StudentResultService srs = getStudentResultService();
                        srs.deleteByProjectId(project.getId());
                    } catch (Exception e) {
                        throw new Exception("Could not clear previous run results: " + e.getMessage());
                    }
                }

                File submissionsDir = new File(project.getSubmissionsDirectory());
                File[] zipFiles = listZipFiles(submissionsDir);
                int total = zipFiles.length;

                for (int i = 0; i < zipFiles.length; i++) {
                    if (cancelRequested) {
                        break;
                    }

                    File zipFile = zipFiles[i];
                    String studentId = zipProcessor.getStudentIdFromZip(zipFile);
                    int current = i + 1;

                    runOnUiThread(() -> callback.onStudentStarted(studentId));

                    try {
                        StudentResult result = processStudent(zipFile, config, testCases, project);
                        String saveError = trySaveStudentResult(result);
                        results.add(result);

                        if (saveError != null) {
                            runOnUiThread(() -> callback.onStudentError(studentId, saveError));
                        }
                        runOnUiThread(() -> callback.onStudentCompleted(result));
                    } catch (Exception e) {
                        StudentResult errorResult = createErrorResult(
                                project,
                                zipFile,
                                studentId,
                                e.getMessage()
                        );

                        String saveError = trySaveStudentResult(errorResult);
                        results.add(errorResult);

                        runOnUiThread(() -> callback.onStudentError(studentId, e.getMessage()));
                        if (saveError != null) {
                            runOnUiThread(() -> callback.onStudentError(studentId, saveError));
                        }
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

    public void cancel() {
        cancelRequested = true;
    }

    public List<StudentResult> runSync(Project project,
                                       Configuration config,
                                       List<TestCase> testCases) throws Exception {

        cancelRequested = false;

        List<StudentResult> results = new ArrayList<>();
        if (project.getId() > 0) {
            try {
                StudentResultService srs = getStudentResultService();
                srs.deleteByProjectId(project.getId());
            } catch (Exception e) {
                throw new Exception("Could not clear previous run results: " + e.getMessage());
            }
        }
        File submissionsDir = new File(project.getSubmissionsDirectory());
        File[] zipFiles = listZipFiles(submissionsDir);

        for (File zipFile : zipFiles) {
            if (cancelRequested) {
                break;
            }

            String studentId = zipProcessor.getStudentIdFromZip(zipFile);
            StudentResult result;

            try {
                result = processStudent(zipFile, config, testCases, project);
            } catch (Exception e) {
                result = createErrorResult(project, zipFile, studentId, e.getMessage());
            }

            trySaveStudentResult(result);
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
            File studentDir;

            try {
                studentDir = zipProcessor.extractSingle(
                        zipFile,
                        new File(project.getWorkingDirectory())
                );
            } catch (Exception e) {
                markExtractionError(result, e);
                return result;
            }

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
                markCompileBlocked(result);
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

            runTestCases(result, testCases, config, executionDir, sourceFile, studentDir);
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
                result.setCompileStatus(ResultStatus.TIMEOUT);
                result.setCompileError(
                        appendLine(result.getCompileError(), "Compilation timed out.")
                );
            } else {
                result.setCompileStatus(
                        processResult.getExitCode() == 0
                                ? ResultStatus.SUCCESS
                                : ResultStatus.COMPILE_ERROR
                );

                if (result.getCompileStatus() == ResultStatus.COMPILE_ERROR
                        && isBlank(result.getCompileError())) {
                    result.setCompileError("Compilation failed with exit code " + processResult.getExitCode() + ".");
                }
            }
        } catch (Exception e) {
            result.setCompileStatus(ResultStatus.COMPILE_ERROR);
            result.setCompileError("Could not start compiler: " + e.getMessage());
        }
    }

    private void runTestCases(StudentResult result,
                              List<TestCase> testCases,
                              Configuration config,
                              File executionDir,
                              File sourceFile,
                              File studentDir) {

        List<TestCase> orderedTestCases = new ArrayList<>(testCases);
        orderedTestCases.sort(Comparator.comparingInt(TestCase::getOrderIndex));

        StringBuilder allProgramOutput = new StringBuilder();
        StringBuilder allErrorOutput = new StringBuilder();
        List<NamedTestOutcome> outcomes = new ArrayList<>();

        boolean anyRunFailed = false;
        boolean anyTestFailed = false;
        boolean anyTestError = false;
        boolean atLeastOneCompared = false;
        ResultStatus aggregateRunStatus = ResultStatus.SUCCESS;
        File testRunsRoot;

        try {
            testRunsRoot = prepareTestRunsRoot(studentDir);
        } catch (IOException e) {
            result.setRunStatus(ResultStatus.RUNTIME_ERROR);
            result.setTestStatus(ResultStatus.ERROR);
            result.setTestDetails("Could not prepare isolated test directories: " + safeMessage(e));
            result.setErrorOutput(safeMessage(e));
            return;
        }

        for (int i = 0; i < orderedTestCases.size(); i++) {
            TestCase testCase = orderedTestCases.get(i);
            SingleTestOutcome outcome;

            try {
                File testExecutionDir = prepareTestExecutionDir(executionDir, testRunsRoot, testCase, i);
                File testSourceFile = new File(testExecutionDir, sourceFile.getName());
                outcome = runSingleTestCase(testCase, config, testExecutionDir, testSourceFile);
            } catch (IOException e) {
                outcome = createPreparationErrorOutcome(e);
            }

            String testName = displayTestName(testCase);
            outcomes.add(new NamedTestOutcome(testName, outcome));

            if (!isBlank(outcome.stdout)) {
                allProgramOutput
                        .append("--- ")
                        .append(testName)
                        .append(" stdout ---\n")
                        .append(outcome.stdout)
                        .append('\n');
            }

            if (!isBlank(outcome.stderr)) {
                allErrorOutput
                        .append("--- ")
                        .append(testName)
                        .append(" stderr ---\n")
                        .append(outcome.stderr)
                        .append('\n');
            }

            if (outcome.runStatus != ResultStatus.SUCCESS) {
                anyRunFailed = true;
                if (aggregateRunStatus == ResultStatus.SUCCESS
                        || outcome.runStatus == ResultStatus.TIMEOUT) {
                    aggregateRunStatus = outcome.runStatus;
                }
            }

            if (outcome.testStatus == ResultStatus.ERROR) {
                anyTestError = true;
            }

            if (outcome.testStatus == ResultStatus.FAIL) {
                anyTestFailed = true;
            }

            if (outcome.testStatus == ResultStatus.PASS
                    || outcome.testStatus == ResultStatus.FAIL) {
                atLeastOneCompared = true;
            }
        }

        result.setProgramOutput(allProgramOutput.toString().trim());
        result.setErrorOutput(allErrorOutput.toString().trim());

        result.setRunStatus(anyRunFailed ? aggregateRunStatus : ResultStatus.SUCCESS);

        if (anyRunFailed || anyTestError) {
            result.setTestStatus(ResultStatus.ERROR);
        } else if (!atLeastOneCompared) {
            result.setTestStatus(ResultStatus.SKIPPED);
        } else if (anyTestFailed) {
            result.setTestStatus(ResultStatus.FAIL);
        } else {
            result.setTestStatus(ResultStatus.PASS);
        }

        result.setTestDetails(formatAggregatedDetails(outcomes));
    }

    private SingleTestOutcome runSingleTestCase(TestCase testCase,
                                                Configuration config,
                                                File executionDir,
                                                File sourceFile) {

        SingleTestOutcome outcome = new SingleTestOutcome();

        try {
            if (isBlank(config.getRunCommand())) {
                outcome.runStatus = ResultStatus.RUNTIME_ERROR;
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
                outcome.runStatus = ResultStatus.RUNTIME_ERROR;
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
            outcome.hasExitCode = true;

            if (processResult.isTimedOut()) {
                outcome.runStatus = ResultStatus.TIMEOUT;
                outcome.testStatus = ResultStatus.ERROR;
                outcome.details = appendCapturedOutput(
                        withExitCode("Execution timed out.", processResult.getExitCode()),
                        outcome.stdout,
                        outcome.stderr
                );
                return outcome;
            }

            outcome.runStatus = processResult.getExitCode() == 0
                    ? ResultStatus.SUCCESS
                    : ResultStatus.RUNTIME_ERROR;

            if (outcome.runStatus == ResultStatus.RUNTIME_ERROR) {
                outcome.testStatus = ResultStatus.ERROR;
                outcome.details = appendCapturedOutput(
                        withExitCode(
                                "Program exited with code " + processResult.getExitCode() + ".",
                                processResult.getExitCode()
                        ),
                        outcome.stdout,
                        outcome.stderr
                );
                return outcome;
            }

            if (!isBlank(testCase.getExpectedOutputFile())) {
                if (processResult.getStdout() == null) {
                    outcome.testStatus = ResultStatus.ERROR;
                    outcome.details = withExitCode(
                            "Program output was unavailable; comparison was not run.",
                            processResult.getExitCode()
                    );
                    return outcome;
                }

                File expectedOutputFile = new File(testCase.getExpectedOutputFile());

                if (!expectedOutputFile.exists() || !expectedOutputFile.isFile()) {
                    outcome.testStatus = ResultStatus.ERROR;
                    outcome.details = "Expected output file not found: " + expectedOutputFile.getAbsolutePath();
                    return outcome;
                }

                ComparisonResult comparisonResult;

                try {
                    String expectedOutput = Files.readString(expectedOutputFile.toPath(), StandardCharsets.UTF_8);
                    comparisonResult = outputComparator.compare(processResult.getStdout(), expectedOutput);
                    outcome.expectedOutput = expectedOutput;
                    outcome.actualOutput = processResult.getStdout();
                } catch (IOException e) {
                    outcome.testStatus = ResultStatus.ERROR;
                    outcome.details = "Could not read expected output file: " + e.getMessage();
                    return outcome;
                }

                outcome.testStatus = comparisonResult.isMatch()
                        ? ResultStatus.PASS
                        : ResultStatus.FAIL;

                if (!comparisonResult.isMatch()) {
                    outcome.details = comparisonResult.getDifferences().isEmpty()
                            ? "Output differs from expected output."
                            : String.join("\n", comparisonResult.getDifferences());
                }
            } else {
                outcome.testStatus = ResultStatus.SKIPPED;
                outcome.details = "Expected output file was not provided.";
            }
        } catch (Exception e) {
            outcome.runStatus = ResultStatus.RUNTIME_ERROR;
            outcome.testStatus = ResultStatus.ERROR;
            outcome.stderr = appendLine(outcome.stderr, e.getMessage());
            outcome.details = "Could not run program: " + e.getMessage();
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

        result.setCompileStatus(ResultStatus.MISSING_SOURCE);
        result.setCompileError(message);
        result.setRunStatus(ResultStatus.SKIPPED);
        result.setErrorOutput(message);
        result.setTestStatus(ResultStatus.ERROR);
        result.setTestDetails(message);
        result.setProcessedAt(LocalDateTime.now());
    }

    private void markExtractionError(StudentResult result, Exception exception) {
        String message = "ZIP extraction failed: " + safeMessage(exception);

        result.setCompileStatus(ResultStatus.EXTRACTION_ERROR);
        result.setCompileError(message);
        result.setRunStatus(ResultStatus.SKIPPED);
        result.setErrorOutput(message);
        result.setTestStatus(ResultStatus.ERROR);
        result.setTestDetails(message);
        result.setProcessedAt(LocalDateTime.now());
    }

    private void markCompileBlocked(StudentResult result) {
        result.setRunStatus(ResultStatus.SKIPPED);
        result.setTestStatus(ResultStatus.ERROR);

        String detail = firstNonBlank(
                result.getCompileError(),
                result.getCompileOutput(),
                "Compilation did not complete successfully."
        );
        result.setTestDetails(appendSection(String.valueOf(result.getCompileStatus()), detail.trim()));
    }

    private SingleTestOutcome createPreparationErrorOutcome(Exception exception) {
        SingleTestOutcome outcome = new SingleTestOutcome();
        outcome.runStatus = ResultStatus.RUNTIME_ERROR;
        outcome.testStatus = ResultStatus.ERROR;
        outcome.stderr = safeMessage(exception);
        outcome.details = "Could not prepare isolated test directory: " + safeMessage(exception);
        return outcome;
    }

    private File prepareTestRunsRoot(File studentDir) throws IOException {
        File parent = studentDir.getParentFile() != null
                ? studentDir.getParentFile()
                : studentDir;
        File testRunsRoot = new File(parent, studentDir.getName() + "-test-runs");

        clearDirectory(testRunsRoot);
        createDirectory(testRunsRoot);
        return testRunsRoot;
    }

    private File prepareTestExecutionDir(File executionDir,
                                         File testRunsRoot,
                                         TestCase testCase,
                                         int index) throws IOException {

        String directoryName = String.format(
                Locale.ROOT,
                "%02d-%s",
                index + 1,
                safeFileName(displayTestName(testCase))
        );
        File testExecutionDir = new File(testRunsRoot, directoryName);
        copyDirectory(executionDir.toPath(), testExecutionDir.toPath());
        return testExecutionDir;
    }

    private void copyDirectory(Path sourceRoot, Path targetRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            paths.forEach(source -> {
                Path relative = sourceRoot.relativize(source);
                Path target = targetRoot.resolve(relative);

                try {
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(
                                source,
                                target,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES
                        );
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private void clearDirectory(File directory) throws IOException {
        if (directory == null || !directory.exists()) {
            return;
        }

        Path root = directory.toPath();

        try (Stream<Path> paths = Files.walk(root)) {
            paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private void createDirectory(File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Could not create directory: " + directory.getAbsolutePath());
        }
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
                (dir, name) -> new File(dir, name).isFile()
                        && name.toLowerCase(Locale.ROOT).endsWith(".zip")
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

    private String trySaveStudentResult(StudentResult result) {
        try {
            saveStudentResult(result);
            return null;
        } catch (Exception e) {
            String message = "Could not save result: " + safeMessage(e);
            result.setTestDetails(appendLine(result.getTestDetails(), message));
            return message;
        }
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

    private String displayTestName(TestCase testCase) {
        if (testCase == null || isBlank(testCase.getName())) {
            return "Test case";
        }

        return testCase.getName();
    }

    private String safeFileName(String value) {
        String safe = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "test" : safe;
    }

    private String formatAggregatedDetails(List<NamedTestOutcome> outcomes) {
        String details = formatOutcomeSummary(outcomes);
        details = appendSection(details, formatFailedComparisons(outcomes));
        details = appendSection(details, formatRuntimeErrorGroups(outcomes));
        details = appendSection(details, formatExecutionErrorGroups(outcomes));
        return details.trim();
    }

    private String formatOutcomeSummary(List<NamedTestOutcome> outcomes) {
        List<String> summary = new ArrayList<>();

        for (NamedTestOutcome namedOutcome : outcomes) {
            summary.add(namedOutcome.testName + ": " + namedOutcome.outcome.testStatus);
        }

        return String.join("; ", summary);
    }

    private String formatFailedComparisons(List<NamedTestOutcome> outcomes) {
        StringBuilder failures = new StringBuilder();

        for (NamedTestOutcome namedOutcome : outcomes) {
            SingleTestOutcome outcome = namedOutcome.outcome;

            if (outcome.testStatus != ResultStatus.FAIL) {
                continue;
            }

            if (failures.length() == 0) {
                failures.append("Failed comparisons:");
            }

            failures
                    .append(System.lineSeparator())
                    .append("- ")
                    .append(namedOutcome.testName)
                    .append(": expected \"")
                    .append(compactOutput(outcome.expectedOutput))
                    .append("\", got \"")
                    .append(compactOutput(outcome.actualOutput))
                    .append("\"");
        }

        return failures.toString();
    }

    private String formatRuntimeErrorGroups(List<NamedTestOutcome> outcomes) {
        LinkedHashMap<String, DetailGroup> groups = new LinkedHashMap<>();

        for (NamedTestOutcome namedOutcome : outcomes) {
            SingleTestOutcome outcome = namedOutcome.outcome;

            if (outcome.testStatus != ResultStatus.ERROR
                    || (outcome.runStatus != ResultStatus.RUNTIME_ERROR
                    && outcome.runStatus != ResultStatus.TIMEOUT)) {
                continue;
            }

            String diagnostic = runtimeDiagnostic(outcome);
            String key = outcome.runStatus + "|" + outcome.exitCode + "|" + diagnostic;
            groups
                    .computeIfAbsent(key, ignored -> new DetailGroup(outcome.runStatus, outcome, diagnostic))
                    .testNames
                    .add(namedOutcome.testName);
        }

        return formatDetailGroups(groups);
    }

    private String formatExecutionErrorGroups(List<NamedTestOutcome> outcomes) {
        LinkedHashMap<String, DetailGroup> groups = new LinkedHashMap<>();

        for (NamedTestOutcome namedOutcome : outcomes) {
            SingleTestOutcome outcome = namedOutcome.outcome;

            if (outcome.testStatus != ResultStatus.ERROR
                    || outcome.runStatus == ResultStatus.RUNTIME_ERROR
                    || outcome.runStatus == ResultStatus.TIMEOUT) {
                continue;
            }

            String diagnostic = firstNonBlank(outcome.details, outcome.stderr, outcome.stdout, "Execution failed.");
            String key = "EXECUTION|" + diagnostic;
            groups
                    .computeIfAbsent(key, ignored -> new DetailGroup(ResultStatus.ERROR, outcome, diagnostic.trim()))
                    .testNames
                    .add(namedOutcome.testName);
        }

        return formatDetailGroups(groups);
    }

    private String formatDetailGroups(LinkedHashMap<String, DetailGroup> groups) {
        StringBuilder formatted = new StringBuilder();

        for (DetailGroup group : groups.values()) {
            if (formatted.length() > 0) {
                formatted.append(System.lineSeparator()).append(System.lineSeparator());
            }

            formatted
                    .append(groupTitle(group.status))
                    .append(" in ")
                    .append(group.testNames.size())
                    .append(" test(s): ")
                    .append(String.join(", ", group.testNames));

            if ((group.status == ResultStatus.RUNTIME_ERROR || group.status == ResultStatus.TIMEOUT)
                    && group.outcome.hasExitCode) {
                formatted
                        .append(System.lineSeparator())
                        .append("Exit code: ")
                        .append(group.outcome.exitCode);
            }

            if (!isBlank(group.details)) {
                formatted
                        .append(System.lineSeparator())
                        .append(group.details.trim());
            }
        }

        return formatted.toString();
    }

    private String groupTitle(ResultStatus status) {
        if (status == ResultStatus.TIMEOUT) {
            return "Timeout";
        }

        if (status == ResultStatus.RUNTIME_ERROR) {
            return "Runtime error";
        }

        return "Execution error";
    }

    private String runtimeDiagnostic(SingleTestOutcome outcome) {
        if (!isBlank(outcome.stderr)) {
            return outcome.stderr.trim();
        }

        String detail = stripRuntimeBoilerplate(outcome.details);
        if (!isBlank(detail)) {
            return detail.trim();
        }

        if (!isBlank(outcome.stdout)) {
            return "stdout: " + compactOutput(outcome.stdout);
        }

        return "Program did not complete successfully.";
    }

    private String stripRuntimeBoilerplate(String details) {
        if (isBlank(details)) {
            return "";
        }

        StringBuilder stripped = new StringBuilder();
        String[] lines = details.replace("\r\n", "\n").replace("\r", "\n").split("\n");

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("Exit code:")
                    || trimmed.matches("Program exited with code -?\\d+\\.")) {
                continue;
            }

            if (stripped.length() > 0) {
                stripped.append(System.lineSeparator());
            }
            stripped.append(line);
        }

        return stripped.toString();
    }

    private String compactOutput(String output) {
        String normalized = valueOrEmpty(output)
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();

        if (normalized.isEmpty()) {
            return "<empty>";
        }

        return normalized
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\t", "\\t")
                .replace("\n", "\\n");
    }

    private StudentResultService getStudentResultService() throws Exception {
        Field dbManagerField = projectService
                .getClass()
                .getDeclaredField("dbManager");
        dbManagerField.setAccessible(true);
        DatabaseManager dbManager = (DatabaseManager) dbManagerField.get(projectService);
        return new StudentResultService(dbManager);
    }

    private String withExitCode(String details, int exitCode) {
        return appendLine("Exit code: " + exitCode, details);
    }

    private String appendCapturedOutput(String details, String stdout, String stderr) {
        String updated = details;

        if (!isBlank(stderr)) {
            updated = appendLine(updated, "stderr:");
            updated = appendLine(updated, stderr.trim());
        }

        if (!isBlank(stdout)) {
            updated = appendLine(updated, "stdout:");
            updated = appendLine(updated, stdout.trim());
        }

        return updated;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }

        return "";
    }

    private String safeMessage(Exception exception) {
        if (exception == null) {
            return "Unknown error.";
        }

        return isBlank(exception.getMessage())
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private String appendLine(String current, String line) {
        if (isBlank(current)) {
            return line;
        }

        return current + System.lineSeparator() + line;
    }

    private String appendSection(String current, String section) {
        if (isBlank(section)) {
            return current;
        }

        if (isBlank(current)) {
            return section;
        }

        return current + System.lineSeparator() + System.lineSeparator() + section;
    }

    private static class SingleTestOutcome {
        private ResultStatus runStatus = ResultStatus.PENDING;
        private ResultStatus testStatus = ResultStatus.PENDING;
        private int exitCode = 0;
        private boolean hasExitCode = false;
        private String stdout = "";
        private String stderr = "";
        private String details = "";
        private String expectedOutput = "";
        private String actualOutput = "";
    }

    private static class NamedTestOutcome {
        private final String testName;
        private final SingleTestOutcome outcome;

        private NamedTestOutcome(String testName, SingleTestOutcome outcome) {
            this.testName = testName;
            this.outcome = outcome;
        }
    }

    private static class DetailGroup {
        private final ResultStatus status;
        private final SingleTestOutcome outcome;
        private final String details;
        private final List<String> testNames = new ArrayList<>();

        private DetailGroup(ResultStatus status, SingleTestOutcome outcome, String details) {
            this.status = status;
            this.outcome = outcome;
            this.details = details;
        }
    }
}
