package com.siano;

import com.siano.utils.MidiReader;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class GameEngine {

    public enum State {
        PLAYING,
        FROZEN,
        FINISHED
    }

    private List<List<MidiReader.MidiNote>> noteEvents;
    private int currentEventIndex = 0;
    private double currentTick = 0;
    private State currentState = State.FINISHED;

    public State getState() {
        return currentState;
    }

    private final Set<Integer> targetNotes = new HashSet<>();
    private final Set<Integer> pressedNotes = new HashSet<>();

    private AnimationTimer timer;
    private long lastTime = 0;
    private static final double TEMPO_MULTIPLIER = 1.0;
    private double ticksPerSecond = 200.0;

    private Consumer<Double> onTickUpdate;
    private Consumer<Set<Integer>> onTargetNotesChanged;
    private Runnable onGameFinished;
    private Consumer<NoteFeedback> onNoteFeedback;

    public record NoteFeedback(int key, boolean isCorrect) {
    }

    public void setOnNoteFeedback(Consumer<NoteFeedback> callback) {
        this.onNoteFeedback = callback;
    }

    public void setNoteEvents(List<List<MidiReader.MidiNote>> events) {
        this.noteEvents = events;
        this.currentEventIndex = 0;
        this.currentTick = 0;
        this.currentState = State.PLAYING;
        findNextTarget();
    }

    public void setOnTickUpdate(Consumer<Double> callback) {
        this.onTickUpdate = callback;
    }

    public void setOnTargetNotesChanged(Consumer<Set<Integer>> callback) {
        this.onTargetNotesChanged = callback;
    }

    public void setOnGameFinished(Runnable callback) {
        this.onGameFinished = callback;
    }

    public void start() {
        if (timer != null)
            timer.stop();
        lastTime = System.nanoTime();
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update((now - lastTime) / 1_000_000_000.0);
                lastTime = now;
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null)
            timer.stop();
    }

    private void update(double deltaTime) {
        if (currentState == State.FINISHED)
            return;

        if (currentState == State.PLAYING) {
            currentTick += ticksPerSecond * deltaTime;
            if (onTickUpdate != null)
                onTickUpdate.accept(currentTick);

            checkIfNeedToFreeze();
        } else if (currentState == State.FROZEN) {
            checkInput();
        }
    }

    private void checkIfNeedToFreeze() {
        if (currentEventIndex >= noteEvents.size()) {
            currentState = State.FINISHED;
            if (onGameFinished != null)
                onGameFinished.run();
            return;
        }

        List<MidiReader.MidiNote> nextEvent = noteEvents.get(currentEventIndex);
        if (nextEvent.isEmpty())
            return;

        long targetTick = nextEvent.get(0).startTick;

        if (currentTick >= targetTick) {
            currentTick = targetTick;
            currentState = State.FROZEN;
            targetNotes.clear();
            for (MidiReader.MidiNote note : nextEvent) {
                targetNotes.add(note.key);
            }
            if (onTargetNotesChanged != null)
                onTargetNotesChanged.accept(targetNotes);
        }
    }

    public void handleInputNote(String noteName, Integer midiKey) {
        if (midiKey == null)
            return;

        boolean isCorrect = false;

        if (currentState == State.FROZEN) {
            if (targetNotes.contains(midiKey)) {
                targetNotes.remove(midiKey);
                isCorrect = true;
                if (onTargetNotesChanged != null)
                    onTargetNotesChanged.accept(targetNotes);

                if (targetNotes.isEmpty()) {
                    currentState = State.PLAYING;
                    currentEventIndex++;
                }
            } else {
                isCorrect = false;
            }
        } else {
            isCorrect = false;
        }

        if (onNoteFeedback != null) {
            onNoteFeedback.accept(new NoteFeedback(midiKey, isCorrect));
        }
    }

    private void checkInput() {
    }

    private void findNextTarget() {
    }
}
