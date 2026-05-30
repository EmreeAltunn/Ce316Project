package com.iae.ui.controller;

import com.iae.db.DatabaseManager;
import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.StudentResult;
import com.iae.model.TestCase;
import com.iae.service.ConfigurationService;
import com.iae.service.ProjectService;
import com.iae.service.StudentResultService;
import com.iae.service.TestCaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for MainWindow.fxml — uygulamanin ana penceresi.
 *
 * <p>Menu eylemlerini yonetir ve {@code contentArea} StackPane'ine
 * ProjectView / RunnerView / ResultsView gibi alt ekranlari yukler.
 */
public class MainController {

    // ─── FXML fields ────────────────────────────────────────────────────────

    @FXML private MenuItem newProjectMenuItem;
    @FXML private MenuItem openProjectMenuItem;
    @FXML private MenuItem saveProjectMenuItem;
    @FXML private MenuItem manageConfigurationsMenuItem;
    @FXML private MenuItem helpManualMenuItem;
    @FXML private MenuItem aboutMenuItem;
    @FXML private MenuItem exitMenuItem;
    @FXML private StackPane contentArea;

    // ─── Services & state ────────────────────────────────────────────────────

    private ConfigurationService configurationService;
    private ProjectService projectService;
    private TestCaseService testCaseService;
    private StudentResultService studentResultService;

    /** Su an contentArea'da gosterilen ProjectController (varsa). */
    private ProjectController activeProjectController;

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        configurationService = new ConfigurationService(dbManager);
        projectService = new ProjectService(dbManager);
        testCaseService = new TestCaseService(dbManager);
        studentResultService = new StudentResultService(dbManager);
    }

    // ─── File menu ───────────────────────────────────────────────────────────

    @FXML
    private void handleNewProject() {
        openProjectView(new Project());
    }

    @FXML
    private void handleOpenProject() {
        try {
            List<Project> projects = projectService.findAll();
            if (projects.isEmpty()) {
                showInfo("No Projects", "There are no saved projects yet.");
                return;
            }
            // ChoiceDialog StringConverter desteklemez — "id - name" etiketleri kullanilir.
            List<String> labels = new java.util.ArrayList<>();
            for (Project p : projects) {
                labels.add(p.getId() + " - " + p.getName());
            }
            ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.get(0), labels);
            dialog.setTitle("Open Project");
            dialog.setHeaderText(null);
            dialog.setContentText("Select a project:");
            dialog.showAndWait().ifPresent(label -> {
                int idx = labels.indexOf(label);
                if (idx >= 0) {
                    openProjectView(projects.get(idx));
                }
            });
        } catch (SQLException e) {
            showError("Open Project failed", e.getMessage());
        }
    }

    @FXML
    private void handleSaveProject() {
        if (activeProjectController == null) {
            showInfo("Save Project", "Open or create a project first.");
            return;
        }
        activeProjectController.saveProject();
    }

    @FXML
    private void handleExit() {
        javafx.application.Platform.exit();
    }

    // ─── Configuration menu ──────────────────────────────────────────────────

    @FXML
    private void handleManageConfigurations() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iae/fxml/ConfigurationsManager.fxml"));
            Parent root = loader.load();

            ConfigurationsManagerController ctrl = loader.getController();
            ctrl.setConfigurationService(configurationService);

            Stage dialog = new Stage();
            dialog.setTitle("Manage Configurations");
            dialog.setScene(new Scene(root));
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(getStage());
            dialog.showAndWait();
            refreshActiveProjectConfigurations();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Cannot open Configurations", e.getMessage());
        }
    }

    // ─── Help menu ───────────────────────────────────────────────────────────

    @FXML
    private void handleHelpManual() {
        try {
            // manual.html JAR icinde gomulu — calistirmak icin gecici dosyaya cikar.
            InputStream in = getClass().getResourceAsStream("/help/manual.html");
            if (in == null) {
                showError("Manual not found", "/help/manual.html bulunamadi.");
                return;
            }
            Path temp = Files.createTempFile("iae-manual", ".html");
            Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            in.close();

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(temp.toUri());
            } else {
                showInfo("Manual", "Manual: " + temp);
            }
        } catch (IOException e) {
            showError("Cannot open manual", e.getMessage());
        }
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("IAE - Integrated Assignment Environment");
        alert.setContentText("Version 1.0.0\nCE316 group project.");
        alert.showAndWait();
    }

    // ─── View switching (alt ekranlar tarafindan cagrilir) ───────────────────

    public void showProjectView(Project project) {
        openProjectView(project);
    }

    public void showRunnerView(Project project, Configuration config, List<TestCase> testCases) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iae/fxml/RunnerView.fxml"));
            Parent root = loader.load();
            RunnerController ctrl = loader.getController();
            setContent(root);
            activeProjectController = null;
            ctrl.startRun(project, config, testCases, this);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Cannot open Runner", e.getMessage());
        }
    }

    public void showResultsView(List<StudentResult> results) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iae/fxml/ResultsView.fxml"));
            Parent root = loader.load();
            ResultsController ctrl = loader.getController();
            setContent(root);
            ctrl.setResults(results);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Cannot open Results", e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void openProjectView(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iae/fxml/ProjectView.fxml"));
            Parent root = loader.load();
            ProjectController ctrl = loader.getController();
            ctrl.init(this, configurationService, projectService, testCaseService);
            ctrl.setProject(project);
            setContent(root);
            activeProjectController = ctrl;
        } catch (IOException e) {
            e.printStackTrace();
            showError("Cannot open Project view", e.getMessage());
        }
    }

    private void setContent(Parent root) {
        contentArea.getChildren().setAll(root);
    }

    private void refreshActiveProjectConfigurations() {
        if (activeProjectController != null) {
            activeProjectController.refreshConfigurations();
        }
    }

    StudentResultService getStudentResultService() {
        return studentResultService;
    }

    ProjectService getProjectService() {
        return projectService;
    }

    private Stage getStage() {
        return (Stage) contentArea.getScene().getWindow();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("IAE");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
