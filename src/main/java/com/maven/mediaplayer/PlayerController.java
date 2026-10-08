package com.maven.mediaplayer;

import javafx.scene.media.*;
import javafx.util.Duration;

import java.util.List;

public class PlayerController {

    private MediaPlayer player;
    private final MediaView mediaView = new MediaView();

    private List<MediaFile> currentPlaylist;
    private int currentIndex = -1;
    private Runnable onTrackChanged;
    private Runnable onStateChanged;

    /** True when the current track is an audio file. */
    private boolean audioMode = false;

    private double volume = 0.5;
    private double previousVolume = 0.5;
    private boolean muted = false;

    public MediaView getMediaView() { return mediaView; }

    public void setOnTrackChanged(Runnable r) { this.onTrackChanged = r; }
    public void setOnStateChanged(Runnable r) { this.onStateChanged = r; }

    public boolean isAudioMode() { return audioMode; }
    public boolean isPlaying() {
        return player != null && player.getStatus() == MediaPlayer.Status.PLAYING;
    }
    public boolean isMuted() { return muted; }
    public double getVolume() { return volume; }

    public void play(MediaFile file, List<MediaFile> playlist) {
        stopAndDispose();
        this.currentPlaylist = playlist;
        this.currentIndex = playlist.indexOf(file);
        this.audioMode = (file.getType() == MediaFile.Type.AUDIO);

        try {
            Media media = new Media(file.getPath().toUri().toString());
            player = new MediaPlayer(media);
            mediaView.setMediaPlayer(player);

            player.setVolume(volume);

            player.setOnReady(() -> {
                if (onTrackChanged != null) onTrackChanged.run();
                if (onStateChanged != null) onStateChanged.run();
            });
            player.setOnPlaying(() -> {
                if (onStateChanged != null) onStateChanged.run();
            });
            player.setOnPaused(() -> {
                if (onStateChanged != null) onStateChanged.run();
            });
            player.setOnStopped(() -> {
                if (onStateChanged != null) onStateChanged.run();
            });
            player.setOnEndOfMedia(this::playNext);

            mediaView.setVisible(!audioMode); // hide view for audio

            player.play();
        } catch (Exception ex) {
            System.err.println("Failed to play " + file.getPath() + ": " + ex.getMessage());
        }
    }

    public void pauseOrResume() {
        if (player == null) return;
        MediaPlayer.Status status = player.getStatus();
        if (status == MediaPlayer.Status.PLAYING) {
            player.pause();
        } else if (status == MediaPlayer.Status.PAUSED
                || status == MediaPlayer.Status.STOPPED
                || status == MediaPlayer.Status.READY) {
            player.play();
        }
    }

    public void stop() {
        if (player != null) {
            player.stop();
            if (onStateChanged != null) onStateChanged.run();
        }
    }

    public void playNext() {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;
        int next = (currentIndex + 1) % currentPlaylist.size();
        play(currentPlaylist.get(next), currentPlaylist);
    }

    public void playPrevious() {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;
        int prev = (currentIndex - 1 + currentPlaylist.size()) % currentPlaylist.size();
        play(currentPlaylist.get(prev), currentPlaylist);
    }

    public void setVolume(double v) {
        this.volume = clamp(v);
        if (player != null && !muted) player.setVolume(this.volume);
        if (!muted) this.previousVolume = this.volume;
    }

    public void changeVolume(double delta) {
        setVolume(volume + delta);
    }

    public void toggleMute() {
        if (player == null) {
            muted = !muted;
            return;
        }
        if (!muted) {
            previousVolume = player.getVolume();
            player.setVolume(0);
            muted = true;
        } else {
            player.setVolume(previousVolume);
            volume = previousVolume;
            muted = false;
        }
        if (onStateChanged != null) onStateChanged.run();
    }

    public Duration getCurrentTime() {
        return player != null ? player.getCurrentTime() : Duration.ZERO;
    }

    public Duration getTotalDuration() {
        return player != null ? player.getTotalDuration() : Duration.ZERO;
    }

    public MediaPlayer getPlayer() { return player; }

    public void stopAndDispose() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
        mediaView.setMediaPlayer(null);
    }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
}
