package com.maven.mediaplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class App extends Application {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private ListView<String> playlistView;
    private final List<File> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private Slider volumeSlider;
    private Label nowPlayingLabel;
    private double previousVolume = 0.5;
    private boolean isMuted = false;

    @Override
    public void start(Stage primaryStage) {
        // --- Media View ---
        mediaView = new MediaView();
        mediaView.setFitWidth(640);
        mediaView.setFitHeight(360);

        StackPane mediaPane = new StackPane(mediaView);
        mediaPane.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #333; -fx-border-width: 2;");
        mediaPane.setPrefSize(640, 360);

        // --- Playlist ---
        playlistView = new ListView<>();
        playlistView.setPrefWidth(250);
        playlistView.setStyle("-fx-control-inner-background: #2a2a2a; -fx-text-fill: white;");
        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int idx = playlistView.getSelectionModel().getSelectedIndex();
                if (idx >= 0) playIndex(idx);
            }
        });

        // --- Buttons ---
        Button addBtn = new Button("Add File");
        Button removeBtn = new Button("Remove");
        Button playBtn = new Button("Play");
        Button pauseBtn = new Button("Pause");
        Button stopBtn = new Button("Stop");
        Button prevBtn = new Button("◀ Prev");
        Button nextBtn = new Button("Next ▶");
        Button muteBtn = new Button("Mute");

        addBtn.setOnAction(e -> addFiles(primaryStage));
        removeBtn.setOnAction(e -> removeSelected());
        playBtn.setOnAction(e -> playCurrent());
        pauseBtn.setOnAction(e -> { if (mediaPlayer != null) mediaPlayer.pause(); });
        stopBtn.setOnAction(e -> stopPlayback());
        prevBtn.setOnAction(e -> playPrevious());
        nextBtn.setOnAction(e -> playNext());
        muteBtn.setOnAction(e -> toggleMute());

        HBox transportControls = new HBox(8, prevBtn, playBtn, pauseBtn, stopBtn, nextBtn, muteBtn);
        transportControls.setAlignment(Pos.CENTER);

        HBox playlistControls = new HBox(8, addBtn, removeBtn);
        playlistControls.setAlignment(Pos.CENTER);

        // --- Volume ---
        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setPrefWidth(200);
        volumeSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (mediaPlayer != null && !isMuted) {
                mediaPlayer.setVolume(newV.doubleValue() / 100.0);
            }
        });

        HBox volumeBox = new HBox(8, new Label("Volume:"), volumeSlider);
        volumeBox.setAlignment(Pos.CENTER);

        // --- Now Playing ---
        nowPlayingLabel = new Label("Now Playing: —");
        nowPlayingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        // --- Layout ---
        VBox rightPanel = new VBox(10,
                new Label("Playlist"),
                playlistView,
                playlistControls);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setStyle("-fx-background-color: #222;");
        rightPanel.setPrefWidth(270);
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        VBox centerPanel = new VBox(10, mediaPane, nowPlayingLabel, transportControls, volumeBox);
        centerPanel.setPadding(new Insets(10));
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setStyle("-fx-background-color: #111;");

        HBox root = new HBox(centerPanel, rightPanel);
        HBox.setHgrow(centerPanel, Priority.ALWAYS);

        // --- Scene + Keyboard ---
        Scene scene = new Scene(root, 950, 520);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        primaryStage.setTitle("JavaFX Media Player");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case SPACE  -> { togglePlayPause(); event.consume(); }
            case S      -> { stopPlayback();    event.consume(); }
            case N      -> { playNext();        event.consume(); }
            case P      -> { playPrevious();    event.consume(); }
            case UP     -> { adjustVolume(+5);  event.consume(); }
            case DOWN   -> { adjustVolume(-5);  event.consume(); }
            case M      -> { toggleMute();      event.consume(); }
            default -> { }
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) mediaPlayer.pause();
        else mediaPlayer.play();
    }

    private void adjustVolume(double delta) {
        double v = Math.max(0, Math.min(100, volumeSlider.getValue() + delta));
        volumeSlider.setValue(v);
        if (mediaPlayer != null && !isMuted) mediaPlayer.setVolume(v / 100.0);
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;
        if (!isMuted) {
            previousVolume = mediaPlayer.getVolume();
            mediaPlayer.setVolume(0);
            isMuted = true;
        } else {
            mediaPlayer.setVolume(previousVolume);
            volumeSlider.setValue(previousVolume * 100);
            isMuted = false;
        }
    }

    private void addFiles(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Media Files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files",
                        "*.mp3", "*.mp4", "*.wav", "*.m4a", "*.aac", "*.flv", "*.mov"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files != null) {
            for (File f : files) {
                playlist.add(f);
                playlistView.getItems().add(f.getName());
            }
            if (currentIndex == -1 && !playlist.isEmpty()) playIndex(0);
        }
    }

    private void removeSelected() {
        int idx = playlistView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        playlist.remove(idx);
        playlistView.getItems().remove(idx);
        if (idx == currentIndex) { stopPlayback(); currentIndex = -1; }
        else if (idx < currentIndex) currentIndex--;
    }

    private void playCurrent() {
        if (mediaPlayer == null && !playlist.isEmpty()) playIndex(currentIndex < 0 ? 0 : currentIndex);
        else if (mediaPlayer != null) mediaPlayer.play();
    }

    private void playIndex(int index) {
        if (index < 0 || index >= playlist.size()) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        File file = playlist.get(index);
        currentIndex = index;

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

            mediaPlayer.setOnReady(() -> {
                nowPlayingLabel.setText("Now Playing: " + file.getName());
                mediaPlayer.play();
            });
            mediaPlayer.setOnError(() ->
                    showAlert("Playback Error", mediaPlayer.getError().getMessage()));
            mediaPlayer.setOnEndOfMedia(this::playNext);

        } catch (Exception ex) {
            showAlert("Error loading media", ex.getMessage());
        }
        playlistView.getSelectionModel().select(index);
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            nowPlayingLabel.setText("Now Playing: —");
        }
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        playIndex((currentIndex + 1) % playlist.size());
    }

    private void playPrevious() {
        if (playlist.isEmpty()) return;
        playIndex((currentIndex - 1 + playlist.size()) % playlist.size());
    }

    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
