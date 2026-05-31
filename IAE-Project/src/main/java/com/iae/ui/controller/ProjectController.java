package com.iae.ui.controller;

import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.TestCase;
import com.iae.model.StudentResult;
import com.iae.service.ConfigurationService;
import com.iae.service.ProjectService;
import com.iae.service.TestCaseService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller for ProjectView.fxml — proje olusturma/duzenleme formu.
 */
public class ProjectController {

    // ─── FXML fields ────────────────────────────────────────────────────────

    @FXML private TextField projectNameField;
    @FXML private TextArea projectDescriptionArea;
    @FXML private ComboBox<Configuration> configurationComboBox;
    @FXML private TextField submissionsDirectoryField;
    @FXML private Button browseSubmissionsButton;
    @FXML private TextField workingDirectoryField;
    @FXML private Button browseWorkingDirButton;
    @FXML private TableView<TestCase> testCasesTable;
    @FXML private TableColumn<TestCase, String> testNameColumn;
    @FXML private TableColumn<TestCase, String> testArgsColumn;
    @FXML private TableColumn<TestCase, String> testExpectedColumn;
    @FXML private Button addTestCaseButton;
    @FXML private Button removeTestCaseButton;
    @FXML private Button saveButton;
    @FXML private Button runButton;
    @FXML private Button viewResultsButton;
    @FXML private Button deleteButton;
    @FXML private Label statusLabel;

    // ─── State ──────────────────────────────────────────────────────────────

    private MainController mainController;
    private ConfigurationService configurationService;
    private ProjectService projectService;
    private TestCaseService testCaseService;

    private Project project = new Project();
    private final ObservableList<TestCase> testCases = FXCollections.observableArrayList();

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        testNameColumn.setCellValueFactory(
                d -> new SimpleStringProperty(nullSafe(d.getValue().getName())));
        testArgsColumn.setCellValueFactory(
                d -> new SimpleStringProperty(nullSafe(d.getValue().getInputArgs())));
        testExpectedColumn.setCellValueFactory(
                d -> new SimpleStringProperty(nullSafe(d.getValue().getExpectedOutputFile())));
        testCasesTable.setItems(testCases);

        configurationComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Configuration c) {
                return c == null ? "" : c.getName();
            }
            @Override public Configuration fromString(String s) {
                return null;
            }
        });
    }

    /** MainController tarafindan FXML yuklendikten hemen sonra cagrilir. */
    public void init(MainController mainController,
                     ConfigurationService configurationService,
                     ProjectService projectService,
                     TestCaseService testCaseService) {
        this.mainController = mainController;
        this.configurationService = configurationService;
        this.projectService = projectService;
        this.testCaseService = testCaseService;
        loadConfigurations();
    }

    /** Formu verilen proje ile doldurur (yeni proje icin bos Project verilir). */
    public void setProject(Project project) {
        this.project = project;
        projectNameField.setText(nullSafe(project.getName()));
        projectDescriptionArea.setText(nullSafe(project.getDescription()));
        submissionsDirectoryField.setText(nullSafe(project.getSubmissionsDirectory()));
        workingDirectoryField.setText(nullSafe(project.getWorkingDirectory()));
        selectConfiguration(project.getConfigurationId());

        testCases.clear();
        if (project.getId() > 0) {
            try {
                testCases.addAll(testCaseService.findByProjectId(project.getId()));
            } catch (SQLException e) {
                showError("Could not load test cases: " + e.getMessage());
            }
            viewResultsButton.setDisable(false);
        } else {
            viewResultsButton.setDisable(true);
        }
    }

    public void refreshConfigurations() {
        int selectedId = 0;
        Configuration selected = configurationComboBox.getValue();

        if (selected != null) {
            selectedId = selected.getId();
        } else if (project != null) {
            selectedId = project.getConfigurationId();
        }

        loadConfigurations();

        if (selectedId > 0) {
            selectConfiguration(selectedId);
        } else {
            configurationComboBox.setValue(null);
        }
    }

    // ─── FXML handlers ───────────────────────────────────────────────────────

    @FXML
    private void handleBrowseSubmissions() {
        File dir = chooseDirectory("Select Submissions Directory");
        if (dir != null) submissionsDirectoryField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void handleBrowseWorkingDir() {
        File dir = chooseDirectory("Select Working Directory");
        if (dir != null) workingDirectoryField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void handleAddTestCase() {
        String name = prompt("Add Test Case", "Test case name:", "Test " + (testCases.size() + 1));
        if (name == null || name.isBlank()) return;
        String args = prompt("Add Test Case", "Input arguments (optional):", "");

        TestCase tc = new TestCase();
        tc.setName(name.trim());
        tc.setInputArgs(args == null ? "" : args.trim());
        tc.setOrderIndex(testCases.size());

        FileChooser fc = new FileChooser();
        fc.setTitle("Select Expected Output File (optional — Cancel to skip)");
        File expected = fc.showOpenDialog(getStage());
        tc.setExpectedOutputFile(expected != null ? expected.getAbsolutePath() : null);

        testCases.add(tc);
    }

    @FXML
    private void handleRemoveTestCase() {
        TestCase selected = testCasesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a test case to remove.");
            return;
        }
        testCases.remove(selected);
    }

    @FXML
    private void handleSave() {
        saveProject();
    }

    @FXML
    private void handleRun() {
        if (!saveProject()) return;

        Configuration config = configurationComboBox.getValue();
        if (config == null) {
            showError("Select a configuration before running.");
            return;
        }
        if (isBlank(project.getSubmissionsDirectory()) || isBlank(project.getWorkingDirectory())) {
            showError("Submissions and working directories are required to run.");
            return;
        }
        mainController.showRunnerView(project, config, List.copyOf(testCases));
    }

    @FXML
    private void handleViewSavedResults() {
        if (project == null || project.getId() <= 0) {
            showError("Please save or open a project first.");
            return;
        }
        try {
            List<StudentResult> results = mainController.getStudentResultService().findByProjectId(project.getId());
            if (results.isEmpty()) {
                showInfo("No saved results exist for this project yet. Run the assignment first.");
            } else {
                mainController.showResultsView(results);
            }
        } catch (SQLException e) {
            showError("Could not retrieve saved results: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (project == null || project.getId() <= 0) {
            showError("Save or open a project first; nothing to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Project");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete project \"" + nullSafe(project.getName())
                + "\" and all its test cases and saved results? This cannot be undone.");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        try {
            // FK kisitlari ON DELETE CASCADE icermez — once cocuk kayitlar silinmeli.
            mainController.getStudentResultService().deleteByProjectId(project.getId());
            for (TestCase existing : testCaseService.findByProjectId(project.getId())) {
                testCaseService.delete(existing.getId());
            }
            projectService.delete(project.getId());
            mainController.showWelcome();
        } catch (SQLException e) {
            showError("Delete failed: " + e.getMessage());
        }
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    /**
     * Formu dogrular ve projeyi (+ test case'lerini) veritabanina kaydeder.
     *
     * @return basarili ise true
     */
    public boolean saveProject() {
        String name = projectNameField.getText();
        if (isBlank(name)) {
            showError("Project name cannot be empty.");
            return false;
        }
        Configuration config = configurationComboBox.getValue();
        if (config == null) {
            showError("A configuration must be selected.");
            return false;
        }

        project.setName(name.trim());
        project.setDescription(projectDescriptionArea.getText());
        project.setConfigurationId(config.getId());
        project.setSubmissionsDirectory(emptyToNull(submissionsDirectoryField.getText()));
        project.setWorkingDirectory(emptyToNull(workingDirectoryField.getText()));
        project.setUpdatedAt(LocalDateTime.now());

        try {
            if (project.getId() > 0) {
                projectService.update(project);
            } else {
                project.setCreatedAt(LocalDateTime.now());
                project = projectService.save(project);
            }
            persistTestCases();
            viewResultsButton.setDisable(false);
            showInfo("Project saved (#" + project.getId() + ").");
            return true;
        } catch (SQLException e) {
            showError("Save failed: " + e.getMessage());
            return false;
        }
    }

    /** Mevcut test case'leri silip tablodakileri yeniden yazar. */
    private void persistTestCases() throws SQLException {
        for (TestCase existing : testCaseService.findByProjectId(project.getId())) {
            testCaseService.delete(existing.getId());
        }
        int index = 0;
        for (TestCase tc : testCases) {
            tc.setProjectId(project.getId());
            tc.setOrderIndex(index++);
            testCaseService.save(tc);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void loadConfigurations() {
        try {
            configurationComboBox.getItems().setAll(configurationService.findAll());
        } catch (SQLException e) {
            showError("Could not load configurations: " + e.getMessage());
        }
    }

    private boolean selectConfiguration(int configurationId) {
        for (Configuration c : configurationComboBox.getItems()) {
            if (c.getId() == configurationId) {
                configurationComboBox.setValue(c);
                return true;
            }
        }

        if (configurationId > 0) {
            configurationComboBox.setValue(null);
        }

        return false;
    }

    private File chooseDirectory(String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        return chooser.showDialog(getStage());
    }

    private String prompt(String title, String content, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(content);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private Stage getStage() {
        return (Stage) projectNameField.getScene().getWindow();
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #C62828;");
    }

    private void showInfo(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #2E7D32;");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
