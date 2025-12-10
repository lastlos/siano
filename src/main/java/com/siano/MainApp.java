package com.siano;

import com.siano.audio.PitchDetector;
import com.siano.ui.FallingNotesView;
import com.siano.ui.PianoKeyboard;
import com.siano.utils.MidiReader;
import com.siano.utils.MidiReader.MidiNote;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainApp extends Application {

    private final MidiReader midiReader = new MidiReader();
    private final PitchDetector pitchDetector = new PitchDetector();
    private final GameEngine gameEngine = new GameEngine();

    private PianoKeyboard pianoKeyboard;
    private FallingNotesView fallingNotesView;
    private ListView<File> midiListView;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Siano Alpha");

        midiListView = new ListView<>();
        midiListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName().replace(".mid", ""));
            }
        });
        midiListView.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null)
                loadMidi(val);
        });
        loadMidiLibrary();

        VBox leftPanel = new VBox(10, new Label("MIDI Kütüphanesi"), midiListView);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(250);

        BorderPane centerPanel = new BorderPane();

        double pianoHeight = 150;
        double viewHeight = 600;

        pianoKeyboard = new PianoKeyboard(1200, pianoHeight);
        fallingNotesView = new FallingNotesView(1200, viewHeight);

        VBox visualContainer = new VBox(fallingNotesView, pianoKeyboard);
        centerPanel.setCenter(visualContainer);

        statusLabel = new Label("Başlamak için bir MIDI dosyası seçin.");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-padding: 10;");
        centerPanel.setTop(statusLabel);

        BorderPane root = new BorderPane();
        root.setLeft(leftPanel);
        root.setCenter(centerPanel);

        Scene scene = new Scene(root, 1500, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            stop();
            Platform.exit();
        });
        primaryStage.show();

        gameEngine.setOnTickUpdate(tick -> fallingNotesView.update(tick));
        gameEngine.setOnTargetNotesChanged(this::updateTargetKeys);
        gameEngine.setOnGameFinished(() -> Platform.runLater(() -> statusLabel.setText("Parça Tamamlandı!")));
        gameEngine.setOnNoteFeedback(feedback -> Platform.runLater(() -> handleNoteFeedback(feedback)));

        startMicrophone();
    }

    private Set<Integer> currentTargetNotes = new java.util.HashSet<>();

    private void handleNoteFeedback(GameEngine.NoteFeedback feedback) {
        if (feedback.isCorrect()) {
            pianoKeyboard.animateSuccess(feedback.key(), null);
        } else {
            String styleClass = (gameEngine.getState() == GameEngine.State.FROZEN) ? "key-incorrect" : "key-free-play";
            pianoKeyboard.addStyle(feedback.key(), styleClass);

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(300));
            pause.setOnFinished(e -> {
                pianoKeyboard.removeStyle(feedback.key(), styleClass);
            });
            pause.play();
        }
    }

    private void loadMidi(File file) {
        try {
            List<List<MidiNote>> events = midiReader.readNoteEvents(file.getAbsolutePath());
            if (events.isEmpty()) {
                statusLabel.setText("Hata: MIDI dosyasında nota bulunamadı.");
                return;
            }

            statusLabel.setText("Çalınıyor: " + file.getName());
            fallingNotesView.setEvents(events);
            gameEngine.setNoteEvents(events);
            gameEngine.start();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("MIDI Hatası: " + e.getMessage());
        }
    }

    private void updateTargetKeys(Set<Integer> targetNotes) {
        for (Integer oldKey : currentTargetNotes) {
            if (!targetNotes.contains(oldKey)) {
                pianoKeyboard.removeStyle(oldKey, "key-target");
            }
        }

        for (Integer newKey : targetNotes) {
            if (!currentTargetNotes.contains(newKey)) {
                pianoKeyboard.addStyle(newKey, "key-target");
            }
        }

        this.currentTargetNotes = new java.util.HashSet<>(targetNotes);

        if (!targetNotes.isEmpty()) {
            statusLabel.setText("Bekleniyor: " + targetNotes.size() + " nota...");
        } else {
            statusLabel.setText("Çalınıyor...");
        }
    }

    private void startMicrophone() {
        pitchDetector.start(
                midiKey -> {
                    Platform.runLater(() -> {
                        gameEngine.handleInputNote(null, midiKey);
                    });
                },
                error -> {
                    Platform.runLater(() -> statusLabel.setText("Mikrofon Hatası: " + error.getMessage()));
                });
    }

    private void loadMidiLibrary() {
        try {
            Path midiDir = Paths.get("midis");
            if (!Files.exists(midiDir)) {
                Files.createDirectory(midiDir);
            }
            try (Stream<Path> paths = Files.walk(midiDir)) {
                List<File> files = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().toLowerCase().endsWith(".mid"))
                        .map(Path::toFile)
                        .collect(Collectors.toList());
                midiListView.getItems().setAll(files);
            }
        } catch (IOException e) {
            statusLabel.setText("Error loading library.");
        }
    }

    @Override
    public void stop() {
        gameEngine.stop();
        pitchDetector.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
