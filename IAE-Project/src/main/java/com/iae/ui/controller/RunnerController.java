package com.iae.ui.controller;

import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.StudentResult;
import com.iae.model.TestCase;
import com.iae.process.ProcessExecutor;
import com.iae.process.ZipProcessor;
import com.iae.runner.AssignmentRunner;
import com.iae.runner.OutputComparator;
import com.iae.runner.RunnerCallback;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for RunnerView.fxml — calismayi baslatir ve canli ilerleme gosterir.
 *
 * <p>{@link RunnerCallback} implementasyonudur. {@link AssignmentRunner} callback'leri
 * zaten UI thread'e tasir; yine de guvenlik icin {@code Platform.runLater} kullanilir.
 */
public class RunnerController implements RunnerCallback {

    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private TextArea logTextArea;
    @FXML private Button cancelButton;
    @FXML private Button viewResultsButton;
    @FXML private Button backButton;

    private MainController mainController;
    private AssignmentRunner currentRunner;
    private final List<StudentResult> results = new ArrayList<>();

    /** MainController.showRunnerView() tarafindan cagrilir. */
    public void startRun(Project project,
                         Configuration config,
                         List<TestCase> testCases,
                         MainController mainController) {
        this.mainController = mainController;

        currentRunner = new AssignmentRunner(
                new ZipProcessor(),
                new ProcessExecutor(),
                new OutputComparator(),
                mainController.getProjectService());

        log("Starting assignment run for project: " + project.getName());
        log("Configuration: " + config.getName());
        log("Submissions: " + project.getSubmissionsDirectory());
        log("─────────────────────────────────────────");

        currentRunner.runAsync(project, config, testCases, this);
    }

    // ─── RunnerCallback ──────────────────────────────────────────────────────

    @Override
    public void onStudentStarted(String studentId) {
        Platform.runLater(() -> log("▶ " + studentId + " — started"));
    }

    @Override
    public void onStudentCompleted(StudentResult result) {
        Platform.runLater(() -> log("✔ " + result.getStudentId()
                + " — compile=" + result.getCompileStatus()
                + ", test=" + result.getTestStatus()));
    }

    @Override
    public void onStudentError(String studentId, String errorMessage) {
        Platform.runLater(() -> log("✗ " + studentId + " — ERROR: " + errorMessage));
    }

    @Override
    public void onAllCompleted(List<StudentResult> finalResults) {
        Platform.runLater(() -> {
            results.clear();
            results.addAll(finalResults);
            log("─────────────────────────────────────────");
            log("Done. " + finalResults.size() + " student(s) processed.");
            cancelButton.setDisable(true);
            viewResultsButton.setDisable(false);
            backButton.setDisable(false);
            currentRunner = null;
        });
    }

    @Override
    public void onProgress(int current, int total) {
        Platform.runLater(() -> {
            double fraction = total > 0 ? (double) current / total : 0.0;
            progressBar.setProgress(fraction);
            progressLabel.setText(current + " / " + total + " students processed");
        });
    }

    // ─── FXML handlers ───────────────────────────────────────────────────────

    @FXML
    private void handleCancel() {
        if (currentRunner != null) {
            currentRunner.cancel();
        }

        cancelButton.setDisable(true);
        log("Cancellation requested — the run will stop after the current student.");
    }

    @FXML
    private void handleViewResults() {
        mainController.showResultsView(new ArrayList<>(results));
    }

    @FXML
    private void handleBack() {
        mainController.showWelcome();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void log(String line) {
        logTextArea.appendText(line + "\n");
    }
}
