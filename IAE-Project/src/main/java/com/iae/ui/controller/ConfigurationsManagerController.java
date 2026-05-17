package com.iae.ui.controller;

import com.iae.config.ConfigurationExporter;
import com.iae.config.ConfigurationImporter;
import com.iae.model.Configuration;
import com.iae.service.ConfigurationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for ConfigurationsManager.fxml.
 *
 * <p>This screen is opened as a modal dialog by MainController.handleManageConfigurations().
 * It covers Requirements 4 (create/edit/remove) and 5 (import/export).
 *
 * <p>Usage:
 * <pre>
 *   FXMLLoader loader = new FXMLLoader(...ConfigurationsManager.fxml...);
 *   Parent root = loader.load();
 *   ConfigurationsManagerController ctrl = loader.getController();
 *   ctrl.setConfigurationService(service);
 *   Stage stage = new Stage();
 *   stage.initModality(Modality.APPLICATION_MODAL);
 *   stage.setScene(new Scene(root));
 *   stage.showAndWait();
 * </pre>
 */
public class ConfigurationsManagerController {

    // ─── FXML fields ────────────────────────────────────────────────────────

    @FXML private TableView<Configuration>             configurationsTable;
    @FXML private TableColumn<Configuration, String>   nameColumn;
    @FXML private TableColumn<Configuration, String>   typeColumn;
    @FXML private TableColumn<Configuration, String>   compilerColumn;
    @FXML private TableColumn<Configuration, String>   sourceColumn;
    @FXML private TableColumn<Configuration, String>   extensionColumn;

    @FXML private Button newButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;
    @FXML private Button exportAllButton;
    @FXML private Button closeButton;

    @FXML private Label statusLabel;

    // ─── State ──────────────────────────────────────────────────────────────

    private ConfigurationService configurationService;

    private final ConfigurationExporter exporter = new ConfigurationExporter();
    private final ConfigurationImporter importer = new ConfigurationImporter();
    private final ObservableList<Configuration> items  = FXCollections.observableArrayList();

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Wire up table columns
        nameColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getName()));
        typeColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().isRequiresCompilation() ? "Compiled" : "Interpreted"));
        compilerColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        nullSafe(data.getValue().getCompilerPath())));
        sourceColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        nullSafe(data.getValue().getSourceFileName())));
        extensionColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        nullSafe(data.getValue().getFileExtension())));

        configurationsTable.setItems(items);
        configurationsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Double-click to edit
        configurationsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && configurationsTable.getSelectionModel().getSelectedItem() != null) {
                handleEdit();
            }
        });
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Must be called immediately after loading before the dialog is shown.
     */
    public void setConfigurationService(ConfigurationService service) {
        this.configurationService = service;
        refreshTable();
    }

    // ─── FXML handlers ───────────────────────────────────────────────────────

    @FXML
    private void handleNew() {
        openEditorDialog(null);
    }

    @FXML
    private void handleEdit() {
        Configuration selected = getSelectedConfig();
        if (selected == null) {
            showError("Select a configuration to edit.");
            return;
        }
        openEditorDialog(selected);
    }

    @FXML
    private void handleDelete() {
        Configuration selected = getSelectedConfig();
        if (selected == null) {
            showError("Select a configuration to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete '" + selected.getName() + "'?\nThis cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    configurationService.delete(selected.getId());
                    refreshTable();
                    showInfo("Deleted: " + selected.getName());
                } catch (SQLException e) {
                    showError("Delete failed: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Configuration");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "IAE Configuration (*.iaeconfig)", "*.iaeconfig"));

        File file = chooser.showOpenDialog(getStage());
        if (file == null) return;

        try {
            Configuration config = importer.importFromFile(file);

            // Avoid duplicate names — append "(imported)" if needed
            try {
                if (configurationService.findByName(config.getName()).isPresent()) {
                    config.setName(config.getName() + " (imported)");
                }
            } catch (SQLException ignored) {}

            configurationService.save(config);
            refreshTable();
            showInfo("Imported: " + config.getName());
        } catch (IOException | SQLException e) {
            showError("Import failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleExport() {
        Configuration selected = getSelectedConfig();
        if (selected == null) {
            showError("Select a configuration to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Configuration");
        chooser.setInitialFileName(
                selected.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".iaeconfig");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "IAE Configuration (*.iaeconfig)", "*.iaeconfig"));

        File file = chooser.showSaveDialog(getStage());
        if (file == null) return;

        try {
            exporter.exportToFile(selected, file);
            showInfo("Exported to: " + file.getName());
        } catch (IOException e) {
            showError("Export failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportAll() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Export Directory");
        File dir = chooser.showDialog(getStage());
        if (dir == null) return;

        try {
            List<Configuration> all = configurationService.findAll();
            if (all.isEmpty()) {
                showError("No configurations to export.");
                return;
            }
            exporter.exportAllToDirectory(all, dir);
            showInfo("Exported " + all.size() + " configuration(s) to " + dir.getAbsolutePath());
        } catch (IOException | SQLException e) {
            showError("Export all failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        getStage().close();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void openEditorDialog(Configuration config) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iae/fxml/ConfigurationEditor.fxml"));
            Parent root = loader.load();

            ConfigurationEditorController ctrl = loader.getController();
            ctrl.setConfigurationService(configurationService);
            if (config != null) {
                ctrl.setConfiguration(config);
            }

            Stage dialog = new Stage();
            dialog.setTitle(config == null ? "New Configuration" : "Edit Configuration");
            dialog.setResizable(true);
            dialog.setScene(new Scene(root));
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(getStage());
            dialog.showAndWait();

            if (ctrl.isSaved()) {
                refreshTable();
                showInfo("Configuration saved.");
            }
        } catch (IOException e) {
            showError("Failed to open editor: " + e.getMessage());
        }
    }

    private void refreshTable() {
        try {
            items.setAll(configurationService.findAll());
        } catch (SQLException e) {
            showError("Error loading configurations: " + e.getMessage());
        }
    }

    private Configuration getSelectedConfig() {
        return configurationsTable.getSelectionModel().getSelectedItem();
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #C62828;");
    }

    private void showInfo(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #2E7D32;");
    }

    private Stage getStage() {
        return (Stage) configurationsTable.getScene().getWindow();
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
