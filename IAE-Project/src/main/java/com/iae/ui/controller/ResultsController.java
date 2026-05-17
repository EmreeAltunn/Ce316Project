package com.iae.ui.controller;

import com.iae.model.ResultStatus;
import com.iae.model.StudentResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Controller for ResultsView.fxml — ogrenci sonuclarini gosterir ve CSV'ye aktarir.
 */
public class ResultsController {

    @FXML private TableView<StudentResult> resultsTable;
    @FXML private TableColumn<StudentResult, String> studentIdColumn;
    @FXML private TableColumn<StudentResult, String> compileStatusColumn;
    @FXML private TableColumn<StudentResult, String> runStatusColumn;
    @FXML private TableColumn<StudentResult, String> testStatusColumn;
    @FXML private TableColumn<StudentResult, String> errorColumn;
    @FXML private Label summaryLabel;
    @FXML private Label statusLabel;

    private final ObservableList<StudentResult> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        studentIdColumn.setCellValueFactory(
                d -> new SimpleStringProperty(nullSafe(d.getValue().getStudentId())));
        compileStatusColumn.setCellValueFactory(
                d -> new SimpleStringProperty(statusText(d.getValue().getCompileStatus())));
        runStatusColumn.setCellValueFactory(
                d -> new SimpleStringProperty(statusText(d.getValue().getRunStatus())));
        testStatusColumn.setCellValueFactory(
                d -> new SimpleStringProperty(statusText(d.getValue().getTestStatus())));
        errorColumn.setCellValueFactory(
                d -> new SimpleStringProperty(firstLine(detailText(d.getValue()))));
        resultsTable.setItems(items);
    }

    /** MainController.showResultsView() tarafindan cagrilir. */
    public void setResults(List<StudentResult> results) {
        items.setAll(results);

        int pass = 0, fail = 0, error = 0;
        for (StudentResult r : results) {
            ResultStatus s = r.getTestStatus();
            if (s == ResultStatus.PASS || s == ResultStatus.SUCCESS) pass++;
            else if (s == ResultStatus.FAIL || s == ResultStatus.FAILED) fail++;
            else if (s == ResultStatus.ERROR) error++;
        }
        summaryLabel.setText(results.size() + " student(s) | "
                + pass + " PASS | " + fail + " FAIL | " + error + " ERROR");
    }

    @FXML
    private void handleExportCsv() {
        if (items.isEmpty()) {
            showError("No results to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Results as CSV");
        chooser.setInitialFileName("results.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));

        File file = chooser.showSaveDialog(getStage());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.println("Student ID,Compile Status,Test Status,Error");
            for (StudentResult r : items) {
                writer.println(String.join(",",
                        csv(r.getStudentId()),
                        csv(statusText(r.getCompileStatus())),
                        csv(statusText(r.getTestStatus())),
                        csv(firstLine(detailText(r)))));
            }
            showInfo("Exported " + items.size() + " row(s) to " + file.getName());
        } catch (IOException e) {
            showError("Export failed: " + e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String detailText(StudentResult r) {
        if (r.getCompileError() != null && !r.getCompileError().isBlank()) {
            return r.getCompileError();
        }
        if (r.getErrorOutput() != null && !r.getErrorOutput().isBlank()) {
            return r.getErrorOutput();
        }
        return nullSafe(r.getTestDetails());
    }

    private static String statusText(ResultStatus status) {
        return status != null ? status.name() : "";
    }

    private static String firstLine(String text) {
        if (text == null || text.isEmpty()) return "";
        int nl = text.indexOf('\n');
        return nl >= 0 ? text.substring(0, nl) : text;
    }

    /** CSV alanini kacis karakterleriyle guvenli hale getirir. */
    private static String csv(String value) {
        String v = value != null ? value : "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }

    private Stage getStage() {
        return (Stage) resultsTable.getScene().getWindow();
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #C62828;");
    }

    private void showInfo(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #2E7D32;");
    }
}
