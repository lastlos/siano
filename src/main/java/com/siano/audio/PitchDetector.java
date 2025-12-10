package com.siano.audio;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import javax.sound.sampled.*;
import java.util.function.Consumer;

public class PitchDetector {

    private AudioDispatcher dispatcher;
    private Thread audioThread;
    private static final String[] NOTE_NAMES = { "Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La",
            "La#", "Si" };

    public void start(Consumer<Integer> onNoteDetected, Consumer<Exception> onError) {
        if (dispatcher != null) {
            stop();
        }

        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("The audio format is not supported.");
            }

            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            AudioInputStream stream = new AudioInputStream(line);

            JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
            dispatcher = new AudioDispatcher(audioStream, 2048, 0);

            PitchDetectionHandler pdh = (result, e) -> {
                float pitchInHz = result.getPitch();
                if (pitchInHz != -1) {
                    int midiKey = frequencyToMidi(pitchInHz);
                    if (midiKey > 0) {
                        onNoteDetected.accept(midiKey);
                    }
                }
            };

            dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.YIN, 44100, 2048, pdh));

            audioThread = new Thread(dispatcher, "Audio Dispatcher");
            audioThread.start();

        } catch (LineUnavailableException e) {
            onError.accept(e);
        }
    }

    public void stop() {
        if (dispatcher != null && !dispatcher.isStopped()) {
            dispatcher.stop();
        }
        if (audioThread != null) {
            try {
                audioThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        dispatcher = null;
        audioThread = null;
    }

    private int frequencyToMidi(float frequency) {
        if (frequency <= 0) {
            return -1;
        }
        return (int) Math.round(12 * (Math.log(frequency / 440.0) / Math.log(2)) + 69);
    }
}
