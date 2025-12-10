package com.siano.utils;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MidiReader {

    public static class MidiNote {
        public final int key;
        public final String noteName;
        public final int velocity;
        public final long startTick;
        public final long endTick;
        public final long durationTicks;
        private static final String[] NOTE_NAMES = { "Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La",
                "La#", "Si" };

        public MidiNote(int key, int velocity, long startTick, long endTick) {
            this.key = key;
            this.velocity = velocity;
            this.startTick = startTick;
            this.endTick = endTick;
            this.durationTicks = endTick - startTick;
            this.noteName = NOTE_NAMES[key % 12] + (key / 12 - 1);
        }

        @Override
        public String toString() {
            return "Note: " + noteName + " (Key: " + key + ")";
        }
    }

    public List<List<MidiNote>> readNoteEvents(String filePath) throws InvalidMidiDataException, IOException {
        Sequence sequence = MidiSystem.getSequence(new File(filePath));

        List<MidiNote> allNotes = new ArrayList<>();
        for (Track track : sequence.getTracks()) {
            Map<Integer, MidiNote> activeNotes = new java.util.HashMap<>();
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    int key = sm.getData1();
                    int velocity = sm.getData2();
                    long tick = event.getTick();

                    if (sm.getCommand() == 0x90 && velocity > 0) {
                        activeNotes.put(key, new MidiNote(key, velocity, tick, -1));
                    } else if (sm.getCommand() == 0x80 || (sm.getCommand() == 0x90 && velocity == 0)) {
                        if (activeNotes.containsKey(key)) {
                            MidiNote note = activeNotes.remove(key);
                            allNotes.add(new MidiNote(key, note.velocity, note.startTick, tick));
                        }
                    }
                }
            }
        }

        allNotes.sort(Comparator.comparingLong(n -> n.startTick));

        Map<Long, List<MidiNote>> groupedByTick = allNotes.stream()
                .collect(Collectors.groupingBy(n -> n.startTick));

        List<List<MidiNote>> noteEvents = new ArrayList<>(groupedByTick.values());
        if (!noteEvents.isEmpty()) {
            noteEvents.sort(Comparator.comparingLong(event -> event.get(0).startTick));
        }

        System.out.println("Successfully read " + noteEvents.size() + " note events from MIDI file: " + filePath);
        return noteEvents;
    }
}
