package com.iae;

import com.iae.db.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

/**
 * IAE - Integrated Assignment Environment.
 *
 * <p>JavaFX uygulama giris noktasi. Veritabanini baslatir ve ana pencereyi yukler.
 *
 * <p>Not: fat JAR'dan calistirmak icin {@link Launcher} sinifi kullanilir
 * (JavaFX {@code Application} alt sinifi dogrudan fat JAR Main-Class olamaz).
 */
public class Main extends Application {

    /** Uygulama veri dizini: ~/.iae (veritabani burada tutulur). */
    private static final String APP_DIR =
            System.getProperty("user.home") + File.separator + ".iae";

    @Override
    public void start(Stage primaryStage) throws Exception {
        DatabaseManager.getInstance().initialize(APP_DIR);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/iae/fxml/MainWindow.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("IAE - Integrated Assignment Environment");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        DatabaseManager.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
