package com.iae.ui.controller;

import com.iae.model.Project;
import javafx.fxml.FXML;

/**
 * Controller for WelcomeView.fxml — uygulamanin acilis/karsilama ekrani.
 *
 * <p>Bos contentArea yerine kullanilabilir bir baslangic ekrani sunar ve
 * ana eylemleri ({@link MainController}'a delege ederek) baslatir.
 */
public class WelcomeController {

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void handleNewProject() {
        mainController.showProjectView(new Project());
    }

    @FXML
    private void handleOpenProject() {
        mainController.openProjectChooser();
    }

    @FXML
    private void handleManageConfigurations() {
        mainController.manageConfigurations();
    }
}
