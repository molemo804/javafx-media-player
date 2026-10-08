package com.maven.mediaplayer.ui;

import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Animated equalizer: a row of bars that bounce up and down while music plays.
 * Bars settle to a thin line when paused/stopped.
 */
public class MusicBars extends HBox {

    private static final int BAR_COUNT = 24;
    private static final double BAR_WIDTH = 6;
    private static final double BAR_GAP = 4;
    private static final double CELL_HEIGHT = 90;   // max bar height
    private static final double MIN_HEIGHT = 4;

    private final Rectangle[] bars = new Rectangle[BAR_COUNT];
    private final double[] targetHeights = new double[BAR_COUNT];
    private final double[] currentHeights = new double[BAR_COUNT];

    private final Random random = new Random();
    private final AnimationTimer timer;

    private boolean active = false;
    private long lastTargetUpdate = 0;

    public MusicBars() {
        setSpacing(BAR_GAP);
        setAlignment(Pos.BOTTOM_CENTER);
        setPrefHeight(CELL_HEIGHT + 20);
        setMinHeight(CELL_HEIGHT + 20);
        setMaxHeight(CELL_HEIGHT + 20);
        setStyle("-fx-padding: 10;");

        for (int i = 0; i < BAR_COUNT; i++) {
            Rectangle bar = new Rectangle(BAR_WIDTH, MIN_HEIGHT);
            bar.setArcWidth(4);
            bar.setArcHeight(4);
            double hue = 200 + (i * 6);
            bar.setFill(Color.hsb(hue % 360, 0.75, 0.95));

            bars[i] = bar;
            targetHeights[i] = MIN_HEIGHT;
            currentHeights[i] = MIN_HEIGHT;

            // Each bar lives in a fixed-height cell, anchored to the BOTTOM.
            StackPane cell = new StackPane(bar);
            cell.setPrefSize(BAR_WIDTH, CELL_HEIGHT);
            cell.setMinSize(BAR_WIDTH, CELL_HEIGHT);
            cell.setMaxSize(BAR_WIDTH, CELL_HEIGHT);
            StackPane.setAlignment(bar, Pos.BOTTOM_CENTER);

            getChildren().add(cell);
        }

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                tick(now);
            }
        };
        timer.start();
    }

    /** Called from MainView when playing state changes. */
    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            for (int i = 0; i < BAR_COUNT; i++) targetHeights[i] = MIN_HEIGHT;
        }
    }

    private void tick(long now) {
        // Update targets a few times per second while active
        if (active && now - lastTargetUpdate > 90_000_000L) { // ~90 ms
            lastTargetUpdate = now;
            for (int i = 0; i < BAR_COUNT; i++) {
                targetHeights[i] = MIN_HEIGHT + random.nextDouble() * (CELL_HEIGHT - MIN_HEIGHT);
            }
        } else if (!active) {
            for (int i = 0; i < BAR_COUNT; i++) targetHeights[i] = MIN_HEIGHT;
        }

        // Smoothly interpolate current -> target for a fluid look
        for (int i = 0; i < BAR_COUNT; i++) {
            double diff = targetHeights[i] - currentHeights[i];
            currentHeights[i] += diff * 0.25;
            bars[i].setHeight(Math.max(MIN_HEIGHT, currentHeights[i]));
        }
    }

    public void dispose() {
        timer.stop();
    }
}