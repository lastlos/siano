package com.siano.ui;

import com.siano.utils.MidiReader;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class FallingNotesView extends Pane {

    private static final double PIXELS_PER_TICK = 0.2;
    private static final double HIT_LINE_Y = 500;

    private static final int START_NOTE = PianoKeyboard.START_NOTE;
    private static final int KEY_COUNT = PianoKeyboard.KEY_COUNT;

    private List<List<MidiReader.MidiNote>> allEvents;
    private final List<Rectangle> activeNoteRects = new ArrayList<>();
    private double currentTick = 0;
    private double viewHeight = 600;
    private double viewWidth = 1000;

    public FallingNotesView(double width, double height) {
        this.viewWidth = width;
        this.viewHeight = height;
        setPrefSize(width, height);
        getStyleClass().add("falling-notes-view");

        Line hitLine = new Line(0, height - 100, width, height - 100);
    }

    public void setEvents(List<List<MidiReader.MidiNote>> events) {
        this.allEvents = events;
        update(0);
    }

    public void update(double currentTick) {
        this.currentTick = currentTick;
        getChildren().clear();
        activeNoteRects.clear();

        if (allEvents == null)
            return;

        double whiteKeyCount = 0;
        for (int i = 0; i < KEY_COUNT; i++) {
            if (!isBlackKey(START_NOTE + i))
                whiteKeyCount++;
        }
        double whiteKeyWidth = viewWidth / whiteKeyCount;

        double targetY = viewHeight;

        for (List<MidiReader.MidiNote> event : allEvents) {
            for (MidiReader.MidiNote note : event) {
                double x = 0;
                double w = 0;

                int whiteKeysBefore = 0;
                for (int k = START_NOTE; k < note.key; k++) {
                    if (!isBlackKey(k))
                        whiteKeysBefore++;
                }

                if (!isBlackKey(note.key)) {
                    x = whiteKeysBefore * whiteKeyWidth;
                    w = whiteKeyWidth;
                } else {
                    x = (whiteKeysBefore - 0.5) * whiteKeyWidth + (whiteKeyWidth * 0.2);
                    w = whiteKeyWidth * 0.6;
                }

                double distToStart = (note.startTick - currentTick) * PIXELS_PER_TICK;
                double height = note.durationTicks * PIXELS_PER_TICK;

                double rectBottom = targetY - distToStart;
                double rectTop = rectBottom - height;

                if (rectBottom < 0 || rectTop > viewHeight)
                    continue;

                Rectangle r = new Rectangle(x, rectTop, w, height);
                r.getStyleClass().add("note-rectangle");

                getChildren().add(r);
            }
        }
    }

    private boolean isBlackKey(int midiNote) {
        int noteInOctave = midiNote % 12;
        return noteInOctave == 1 || noteInOctave == 3 || noteInOctave == 6 || noteInOctave == 8 || noteInOctave == 10;
    }
}
