package com.siano.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PianoKeyboard extends Pane {

    public static final int START_NOTE = 21;
    public static final int END_NOTE = 108;
    public static final int KEY_COUNT = END_NOTE - START_NOTE + 1;

    private final Map<Integer, Rectangle> keyRectangles = new HashMap<>();
    private final Set<String> highlightClasses = Set.of("key-target", "key-correct", "key-incorrect", "key-free-play");

    public PianoKeyboard(double width, double height) {
        getStyleClass().add("piano-keyboard");
        setPrefSize(width, height);
        drawKeyboard(width, height);
    }

    private void drawKeyboard(double width, double height) {
        getChildren().clear();
        keyRectangles.clear();

        int whiteKeyCount = 0;
        for (int i = 0; i < KEY_COUNT; i++) {
            if (!isBlackKey(START_NOTE + i)) {
                whiteKeyCount++;
            }
        }

        double whiteKeyWidth = width / whiteKeyCount;
        double blackKeyWidth = whiteKeyWidth * 0.6;
        double blackKeyHeight = height * 0.6;

        int whiteKeyIndex = 0;

        for (int i = 0; i < KEY_COUNT; i++) {
            int midiNote = START_NOTE + i;
            if (!isBlackKey(midiNote)) {
                Rectangle whiteKey = new Rectangle(whiteKeyIndex * whiteKeyWidth, 0, whiteKeyWidth, height);
                whiteKey.getStyleClass().addAll("piano-key", "white-key");

                if (midiNote % 12 == 0) {
                }

                getChildren().add(whiteKey);
                keyRectangles.put(midiNote, whiteKey);
                whiteKeyIndex++;
            }
        }

        whiteKeyIndex = 0;
        for (int i = 0; i < KEY_COUNT; i++) {
            int midiNote = START_NOTE + i;
            if (isBlackKey(midiNote)) {
                double xPos = (whiteKeyIndex - 0.5) * whiteKeyWidth - (blackKeyWidth / 2) + (whiteKeyWidth / 2);

                Rectangle blackKey = new Rectangle(xPos, 0, blackKeyWidth, blackKeyHeight);
                blackKey.getStyleClass().addAll("piano-key", "black-key");
                getChildren().add(blackKey);
                keyRectangles.put(midiNote, blackKey);
            } else {
                whiteKeyIndex++;
            }
        }
    }

    private boolean isBlackKey(int midiNote) {
        int noteInOctave = midiNote % 12;
        return noteInOctave == 1 || noteInOctave == 3 || noteInOctave == 6 || noteInOctave == 8 || noteInOctave == 10;
    }

    public void highlightKey(int midiNote, String styleClass) {
        if (keyRectangles.containsKey(midiNote) && highlightClasses.contains(styleClass)) {
            Rectangle key = keyRectangles.get(midiNote);
            key.getStyleClass().removeAll(highlightClasses);
            key.getStyleClass().add(styleClass);
        }
    }

    public void clearHighlight(int midiNote) {
        if (keyRectangles.containsKey(midiNote)) {
            Rectangle key = keyRectangles.get(midiNote);
            key.getStyleClass().removeAll(highlightClasses);
        }
    }

    public void clearAllHighlights() {
        for (Rectangle key : keyRectangles.values()) {
            key.getStyleClass().removeAll(highlightClasses);
        }
    }

    public void addStyle(int midiNote, String styleClass) {
        if (keyRectangles.containsKey(midiNote)) {
            Rectangle key = keyRectangles.get(midiNote);
            if (!key.getStyleClass().contains(styleClass)) {
                key.getStyleClass().add(styleClass);
                if (isBlackKey(midiNote))
                    key.toFront();
            }
        }
    }

    public void removeStyle(int midiNote, String styleClass) {
        if (keyRectangles.containsKey(midiNote)) {
            keyRectangles.get(midiNote).getStyleClass().remove(styleClass);
        }
    }

    public void animateSuccess(int midiNote, Runnable onFinished) {
        if (!keyRectangles.containsKey(midiNote))
            return;
        Rectangle key = keyRectangles.get(midiNote);

        addStyle(midiNote, "key-correct");

        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        timeline.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.millis(200)));

        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150),
                key);
        fade.setFromValue(1.0);
        fade.setToValue(0.3);
        fade.setCycleCount(4);
        fade.setAutoReverse(true);
        fade.setDelay(javafx.util.Duration.millis(200));

        fade.setOnFinished(e -> {
            key.setOpacity(1.0);
            removeStyle(midiNote, "key-correct");
            if (onFinished != null)
                onFinished.run();
        });

        fade.play();
    }
}
