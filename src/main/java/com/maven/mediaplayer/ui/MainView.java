package com.maven.mediaplayer.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.maven.mediaplayer.MediaFile;
import com.maven.mediaplayer.MediaScanner;
import com.maven.mediaplayer.PlayerController;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainView extends BorderPane {

    private final PlayerController controller = new PlayerController();
    private final Stage stage;

    // Playlists
    private final ListView<MediaFile> audioList = new ListView<>();
    private final ListView<MediaFile> videoList = new ListView<>();
    private final List<MediaFile> allAudio = new ArrayList<>();
    private final List<MediaFile> allVideo = new ArrayList<>();

    // UI components
    private final Label nowPlaying = new Label("Nothing playing");
    private final Slider progress = new Slider(0, 1, 0);
    private final Slider volumeSlider = new Slider(0, 1, 0.5);
    private final MusicBars musicBars = new MusicBars();
    private final StackPane centerStage = new StackPane();
    private final Button playPauseBtn = new Button("▶  Play");
    private final Button muteBtn = new Button("🔊");

    private Timeline progressUpdater;
    private boolean updatingProgress = false;

    public MainView(Stage stage, Path mediaFolder) {
        this.stage = stage;
        loadMedia(mediaFolder);
        buildLayout();
        wireEvents();
        setupKeyboard();
        startProgressUpdater();

        controller.setOnTrackChanged(this::refreshNowPlaying);
        controller.setOnStateChanged(this::refreshState);
    }

    // =========================================================
    // Media loading
    // =========================================================
    private void loadMedia(Path folder) {
        try {
            List<MediaFile> all = MediaScanner.scan(folder);
            allAudio.clear();
            allVideo.clear();
            allAudio.addAll(all.stream()
                    .filter(m -> m.getType() == MediaFile.Type.AUDIO)
                    .collect(Collectors.toList()));
            allVideo.addAll(all.stream()
                    .filter(m -> m.getType() == MediaFile.Type.VIDEO)
                    .collect(Collectors.toList()));

            audioList.getItems().setAll(allAudio);
            videoList.getItems().setAll(allVideo);

            System.out.println("Loaded " + allAudio.size() + " audio, "
                    + allVideo.size() + " video files from " + folder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // Layout
    // =========================================================
    private void buildLayout() {
        VBox sidebar = buildSidebar();
        VBox center = buildCenter();

        setLeft(sidebar);
        setCenter(center);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #1e1e1e;");
    }

    private VBox buildSidebar() {
        Label audioLabel = new Label("🎵 Audio");
        audioLabel.getStyleClass().add("sidebar-title");
        Label videoLabel = new Label("🎬 Video");
        videoLabel.getStyleClass().add("sidebar-title");

        VBox audioBox = new VBox(5, audioLabel, audioList);
        VBox videoBox = new VBox(5, videoLabel, videoList);
        VBox.setVgrow(audioList, Priority.ALWAYS);
        VBox.setVgrow(videoList, Priority.ALWAYS);
        VBox.setVgrow(audioBox, Priority.ALWAYS);
        VBox.setVgrow(videoBox, Priority.ALWAYS);

        Button addBtn = new Button("➕ Add File");
        Button removeBtn = new Button("➖ Remove");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addBtn, Priority.ALWAYS);
        HBox.setHgrow(removeBtn, Priority.ALWAYS);

        addBtn.setOnAction(e -> addFiles());
        removeBtn.setOnAction(e -> removeSelected());

        HBox buttons = new HBox(6, addBtn, removeBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox sidebar = new VBox(15, audioBox, videoBox, buttons);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(320);
        sidebar.setPadding(new Insets(12));
        return sidebar;
    }

    private VBox buildCenter() {
        // ----- Video / visualiser stage -----
        MediaView view = controller.getMediaView();
        view.setPreserveRatio(true);
        // Bind to stage size once the stage has a real size
        view.fitWidthProperty().bind(centerStage.widthProperty().subtract(20));
        view.fitHeightProperty().bind(centerStage.heightProperty().subtract(20));

        centerStage.getChildren().addAll(view, musicBars);
        centerStage.getStyleClass().add("video-pane");
        centerStage.setMinHeight(280);
        VBox.setVgrow(centerStage, Priority.ALWAYS);

        // ----- Now playing -----
        nowPlaying.getStyleClass().add("now-playing");
        nowPlaying.setMaxWidth(Double.MAX_VALUE);
        nowPlaying.setAlignment(Pos.CENTER);

        // ----- Progress slider -----
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.valueProperty().addListener((obs, oldV, newV) -> {
            if (updatingProgress) return;
            if (controller.getPlayer() != null
                    && controller.getTotalDuration() != null
                    && controller.getTotalDuration().greaterThan(Duration.ZERO)) {
                Duration target = controller.getTotalDuration().multiply(newV.doubleValue());
                controller.getPlayer().seek(target);
            }
        });

        // ----- Transport controls -----
        Button prevBtn = new Button("⏮");
        Button stopBtn = new Button("⏹");
        Button nextBtn = new Button("⏭");
        prevBtn.getStyleClass().add("icon-button");
        stopBtn.getStyleClass().add("icon-button");
        nextBtn.getStyleClass().add("icon-button");
        playPauseBtn.getStyleClass().add("play-pause-button");
        muteBtn.getStyleClass().add("icon-button");

        prevBtn.setOnAction(e -> controller.playPrevious());
        playPauseBtn.setOnAction(e -> controller.pauseOrResume());
        stopBtn.setOnAction(e -> controller.stop());
        nextBtn.setOnAction(e -> controller.playNext());
        muteBtn.setOnAction(e -> controller.toggleMute());

        HBox controls = new HBox(10,
                prevBtn, playPauseBtn, stopBtn, nextBtn, muteBtn);
        controls.setAlignment(Pos.CENTER);

        // ----- Volume -----
        Label volLabel = new Label("🔉");
        volLabel.getStyleClass().add("now-playing");
        volumeSlider.setPrefWidth(140);
        volumeSlider.valueProperty().addListener((obs, oldV, newV) ->
                controller.setVolume(newV.doubleValue()));

        HBox volumeBox = new HBox(8, volLabel, volumeSlider);
        volumeBox.setAlignment(Pos.CENTER);

        HBox bottomRow = new HBox(20, controls, volumeBox);
        bottomRow.setAlignment(Pos.CENTER);

        VBox bottomBar = new VBox(8, nowPlaying, progress, bottomRow);
        bottomBar.getStyleClass().add("bottom-bar");
        bottomBar.setAlignment(Pos.CENTER);

        VBox center = new VBox(10, centerStage, bottomBar);
        center.setPadding(new Insets(0, 0, 0, 10));
        return center;
    }

    // =========================================================
    // Event wiring
    // =========================================================
    private void wireEvents() {
        audioList.setOnMouseClicked(e -> {
            MediaFile sel = audioList.getSelectionModel().getSelectedItem();
            if (sel != null) controller.play(sel, allAudio);
        });
        videoList.setOnMouseClicked(e -> {
            MediaFile sel = videoList.getSelectionModel().getSelectedItem();
            if (sel != null) controller.play(sel, allVideo);
        });
        audioList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                MediaFile sel = audioList.getSelectionModel().getSelectedItem();
                if (sel != null) controller.play(sel, allAudio);
            }
        });
        videoList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                MediaFile sel = videoList.getSelectionModel().getSelectedItem();
                if (sel != null) controller.play(sel, allVideo);
            }
        });
    }

    private void setupKeyboard() {
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
        setFocusTraversable(true);
    }

    private void handleKey(KeyEvent e) {
        // Let list views handle Up/Down when they have focus
        if ((e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN)
                && (audioList.isFocused() || videoList.isFocused())) {
            return;
        }

        switch (e.getCode()) {
            case SPACE -> { controller.pauseOrResume(); e.consume(); }
            case S     -> { controller.stop(); e.consume(); }
            case N     -> { controller.playNext(); e.consume(); }
            case P     -> { controller.playPrevious(); e.consume(); }
            case UP    -> { controller.changeVolume(+0.05); syncVolumeSlider(); e.consume(); }
            case DOWN  -> { controller.changeVolume(-0.05); syncVolumeSlider(); e.consume(); }
            case M     -> { controller.toggleMute(); syncVolumeSlider(); e.consume(); }
            default -> { }
        }
    }

    private void syncVolumeSlider() {
        volumeSlider.setValue(controller.getVolume());
    }

    // =========================================================
    // Add / Remove files
    // =========================================================
    private void addFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Add media files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files",
                        "*.mp3", "*.wav", "*.aac", "*.m4a", "*.flac", "*.ogg",
                        "*.mp4", "*.mkv", "*.avi", "*.mov", "*.flv", "*.webm"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null) return;

        for (File f : files) {
            MediaFile mf = MediaScanner.classifySingle(f.toPath());
            if (mf == null) continue;
            if (mf.getType() == MediaFile.Type.AUDIO) {
                allAudio.add(mf);
                audioList.getItems().add(mf);
            } else {
                allVideo.add(mf);
                videoList.getItems().add(mf);
            }
        }
    }

    private void removeSelected() {
        MediaFile a = audioList.getSelectionModel().getSelectedItem();
        MediaFile v = videoList.getSelectionModel().getSelectedItem();

        if (a != null) {
            allAudio.remove(a);
            audioList.getItems().remove(a);
        }
        if (v != null) {
            allVideo.remove(v);
            videoList.getItems().remove(v);
        }
    }

    // =========================================================
    // UI refreshers
    // =========================================================
    private void refreshNowPlaying() {
        MediaFile current = controller.isAudioMode()
                ? audioList.getSelectionModel().getSelectedItem()
                : videoList.getSelectionModel().getSelectedItem();

        if (current != null) {
            nowPlaying.setText("Now Playing: " + current);
        }
        updatePlayPauseLabel();
    }

    private void refreshState() {
        updatePlayPauseLabel();
        muteBtn.setText(controller.isMuted() ? "🔇" : "🔊");
        syncVolumeSlider();

        boolean playingAudio = controller.isAudioMode();
        // Bars animate only while audio is playing
        musicBars.setActive(playingAudio && controller.isPlaying());
        // Bars are visible ONLY when the current track is audio
        musicBars.setVisible(playingAudio);
        musicBars.setManaged(playingAudio);
   }

    private void updatePlayPauseLabel() {
        playPauseBtn.setText(controller.isPlaying() ? "⏸  Pause" : "▶  Play");
    }

    // =========================================================
    // Progress bar updater
    // =========================================================
    private void startProgressUpdater() {
        progressUpdater = new Timeline(new KeyFrame(Duration.millis(250), e -> {
            if (controller.getPlayer() == null) return;
            Duration total = controller.getTotalDuration();
            if (total == null || total.isUnknown() || total.lessThanOrEqualTo(Duration.ZERO)) return;
            updatingProgress = true;
            double ratio = controller.getCurrentTime().toMillis() / total.toMillis();
            progress.setValue(Math.max(0, Math.min(1, ratio)));
            updatingProgress = false;
        }));
        progressUpdater.setCycleCount(Timeline.INDEFINITE);
        progressUpdater.play();
    }

    // =========================================================
    // Called from Main on shutdown
    // =========================================================
    public void shutdown() {
        if (progressUpdater != null) progressUpdater.stop();
        musicBars.dispose();
        controller.stopAndDispose();
    }
}