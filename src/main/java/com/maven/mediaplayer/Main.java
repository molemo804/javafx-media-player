package com.maven.mediaplayer;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import com.maven.mediaplayer.ui.MainView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static final Path MEDIA_FOLDER =
            Path.of(System.getProperty("user.home"), "MediaPlayerSamples");

    private static final String STYLESHEET = "/styles.css";

    private static final double WINDOW_WIDTH = 1000;
    private static final double WINDOW_HEIGHT = 650;
    private static final double MIN_WIDTH = 720;
    private static final double MIN_HEIGHT = 480;

    private static final String APP_TITLE = "JavaFX Media Player";

    private MainView root;

    @Override
    public void start(Stage stage) {
        ensureMediaFolderExists();

        root = new MainView(stage, MEDIA_FOLDER);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        applyStylesheet(scene);

        stage.setTitle(APP_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() {
        if (root != null) root.shutdown();
        Platform.exit();
    }

    private void applyStylesheet(Scene scene) {
        URL css = getClass().getResource(STYLESHEET);
        if (css == null) {
            System.err.println("⚠ styles.css not found at " + STYLESHEET);
            return;
        }
        scene.getStylesheets().add(css.toExternalForm());
        System.out.println("✔ Loaded stylesheet: " + css);
    }

    private void ensureMediaFolderExists() {
        try {
            if (!Files.exists(MEDIA_FOLDER)) {
                Files.createDirectories(MEDIA_FOLDER);
                System.out.println("Created media folder: " + MEDIA_FOLDER);
            }
            System.out.println("Media folder: " + MEDIA_FOLDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}