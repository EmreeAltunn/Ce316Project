package com.iae.ui.controller;

import com.iae.config.ConfigurationValidator;
import com.iae.model.Configuration;
import com.iae.model.ValidationResult;
import com.iae.service.ConfigurationService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Controller for ConfigurationEditor.fxml.
 *
 * <p>Usage:
 * <pre>
 *   FXMLLoader loader = new FXMLLoader(...ConfigurationEditor.fxml...);
 *   Parent root = loader.load();
 *   ConfigurationEditorController ctrl = loader.getController();
 *   ctrl.setConfigurationService(service);   // required for Save to persist
 *   ctrl.setConfiguration(existing);         // only when editing
 *   stage.showAndWait();
 *   if (ctrl.isSaved()) { ... }
 * </pre>
 */
public class ConfigurationEditorController {

    // ─── FXML fields ────────────────────────────────────────────────────────

    @FXML private TextField nameField;
    @FXML private TextField compilerPathField;
    @FXML private TextField compilerArgsField;
    @FXML private TextField runCommandField;
    @FXML private CheckBox requiresCompilationCheckBox;
    @FXML private TextField sourceFileNameField;
    @FXML private TextField outputFileNameField;
    @FXML private TextField fileExtensionField;
    @FXML private TextArea  descriptionArea;
    @FXML private Button    saveButton;
    @FXML private Button    cancelButton;
    @FXML private Button    testCompilerButton;
    @FXML private Label     statusLabel;

    // ─── State ──────────────────────────────────────────────────────────────

    private ConfigurationService  configurationService;
    private final ConfigurationValidator validator = new ConfigurationValidator();

    /** The configuration being edited; null when creating a new one. */
    private Configuration editingConfiguration;

    /** Set to true after a successful save so callers can detect it. */
    private boolean saved = false;

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Reflect the initial state of the checkbox immediately
        updateCompilerFieldsEnabled(requiresCompilationCheckBox.isSelected());
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Injects the service used when saving.
     * Must be called before the user presses Save.
     */
    public void setConfigurationService(ConfigurationService service) {
        this.configurationService = service;
    }

    /**
     * Populates the form with an existing configuration (edit mode).
     * If not called the form starts empty (create mode).
     */
    public void setConfiguration(Configuration config) {
        this.editingConfiguration = config;

        nameField.setText(nullSafe(config.getName()));
        compilerPathField.setText(nullSafe(config.getCompilerPath()));
        compilerArgsField.setText(nullSafe(config.getCompilerArgs()));
        runCommandField.setText(nullSafe(config.getRunCommand()));
        requiresCompilationCheckBox.setSelected(config.isRequiresCompilation());
        sourceFileNameField.setText(nullSafe(config.getSourceFileName()));
        outputFileNameField.setText(nullSafe(config.getOutputFileName()));
        fileExtensionField.setText(nullSafe(config.getFileExtension()));
        descriptionArea.setText(nullSafe(config.getDescription()));

        updateCompilerFieldsEnabled(config.isRequiresCompilation());
    }

    /**
     * Builds and returns a Configuration from the current form values.
     * Does NOT save to the database.
     */
    public Configuration getConfiguration() {
        Configuration config = (editingConfiguration != null)
                ? editingConfiguration
                : new Configuration();

        config.setName(nameField.getText().trim());
        config.setCompilerPath(emptyToNull(compilerPathField.getText()));
        config.setCompilerArgs(emptyToNull(compilerArgsField.getText()));
        config.setRunCommand(runCommandField.getText().trim());
        config.setRequiresCompilation(requiresCompilationCheckBox.isSelected());
        config.setSourceFileName(sourceFileNameField.getText().trim());
        config.setOutputFileName(emptyToNull(outputFileNameField.getText()));
        config.setFileExtension(fileExtensionField.getText().trim());
        config.setDescription(emptyToNull(descriptionArea.getText()));
        return config;
    }

    /** Returns true if the user saved (committed) the configuration. */
    public boolean isSaved() {
        return saved;
    }

    // ─── FXML handlers ───────────────────────────────────────────────────────

    /** Called when the requiresCompilation checkbox is toggled via FXML onAction. */
    @FXML
    private void handleCompilationToggle() {
        updateCompilerFieldsEnabled(requiresCompilationCheckBox.isSelected());
    }

    @FXML
    private void handleSave() {
        clearStatus();
        Configuration config = getConfiguration();
        ValidationResult vr = validator.validate(config);

        if (!vr.isValid()) {
            showError(String.join("\n", vr.getErrors()));
            return;
        }

        if (configurationService != null) {
            try {
                if (editingConfiguration != null && editingConfiguration.getId() > 0) {
                    // Editing an existing record
                    configurationService.update(config);
                } else {
                    // Creating a new record
                    LocalDateTime now = LocalDateTime.now();
                    config.setCreatedAt(now);
                    config.setUpdatedAt(now);
                    configurationService.save(config);
                }
                saved = true;
                closeStage();
            } catch (SQLException e) {
                showError("Could not save: " + e.getMessage());
            }
        } else {
            // No service injected – caller will retrieve the config via getConfiguration().
            saved = true;
            closeStage();
        }
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    @FXML
    private void handleTestCompiler() {
        String path = compilerPathField.getText().trim();
        if (path.isEmpty()) {
            showError("Enter a compiler path first.");
            return;
        }
        showInfo("Testing...");
        // Run on a background thread so the UI stays responsive
        Thread t = new Thread(() -> {
            boolean ok = validator.isCompilerAccessible(path);
            javafx.application.Platform.runLater(() -> {
                if (ok) {
                    showInfo("✔ Compiler is accessible: " + path);
                } else {
                    showError("✘ Compiler not found or not accessible: " + path);
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void updateCompilerFieldsEnabled(boolean enabled) {
        compilerPathField.setDisable(!enabled);
        compilerArgsField.setDisable(!enabled);
        testCompilerButton.setDisable(!enabled);
        if (!enabled) {
            compilerPathField.clear();
            compilerArgsField.clear();
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #D32F2F;");
    }

    private void showInfo(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #2E7D32;");
    }

    private void clearStatus() {
        statusLabel.setText("");
    }

    private void closeStage() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
