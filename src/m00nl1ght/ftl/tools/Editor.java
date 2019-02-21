package m00nl1ght.ftl.tools;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

public class Editor extends Application {

    static final Patcher EVENT_PATCHER = new Patcher(new File("./patcher_in/"), new File("./patcher_out/"),"text_events.xml",
            Arrays.asList("eventList", "event", "textList"),
            Arrays.asList("text"));
    static final Patcher BLUEPRINT_PATCHER = new Patcher(new File("./patcher_in/"), new File("./patcher_out/"),"text_blueprints.xml",
            Arrays.asList("systemBlueprint", "weaponBlueprint", "crewBlueprint", "augBlueprint", "droneBlueprint", "shipBlueprint"),
            Arrays.asList("desc", "tooltip", "unlock", "power", "title", "short"));
    static final List<LangReader> LANG_IN = new ArrayList<>();
    static final File LANG_IN_DIR = new File("./lang_in/");
    static final File LANG_OUT_DIR = new File("./lang_out/");

    final Map<String, LangEntry> MAP = new LinkedHashMap<>();
    public LangEntry current = null;

    public static void main(String... args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        this.setup();
        primaryStage.setTitle("FTL Localization Editor");

        VBox pane = new VBox();
        pane.setPadding(new Insets(20, 20, 20, 20));
        pane.setSpacing(5);

        TextArea langA = new TextArea();
        langA.wrapTextProperty().setValue(true);

        TextArea langB = new TextArea();
        langB.wrapTextProperty().setValue(true);

        HBox box = new HBox();
        box.setSpacing(10);

        Button btnSave = new Button("Save");
        btnSave.setOnAction(event -> this.save(btnSave));

        box.getChildren().addAll(btnSave);

        ListView<LangEntry> list = new ListView<>();
        list.setMinSize(-1, 700);
        MAP.values().stream().forEachOrdered(entry -> list.getItems().add(entry));
        list.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (current!=null) {
                current.translation = langB.getText();
            }
            current = newValue;
            langA.setText(current.value);
            langB.setText(current.translation);
        });

        pane.getChildren().addAll(list, new Label("langA"), langA, new Label("langB"), langB, box);
        Scene scene = new Scene(pane, 1000, 1000);
        //scene.getRoot().setStyle("-fx-base: rgba(60, 63, 65, 255)");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static class LangEntry {
        public String key, value = "", translation = "", file = "";
        public String toString() {
            return file.isEmpty()?key:("["+file+"] "+key);
        }
    }

    public void setup() {
        try {
            for (File file : LANG_IN_DIR.listFiles()) {
                LANG_IN.add(new LangReader(file));
            }
            for (LangReader reader : LANG_IN) {
                reader.read(MAP);
            }
            EVENT_PATCHER.patch(MAP);
            BLUEPRINT_PATCHER.patch(MAP);
            new LangReader(new File("text-de.xml"), "de").read(MAP);
            this.info();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void info() {
        int no_eng = 0, no_de = 0;
        for (LangEntry entry : MAP.values()) {
            if (entry.value.isEmpty()) {
                no_eng++;
                System.out.println("Entry missing value: "+entry.key);
            }
            if (entry.translation.isEmpty()) no_de++;
        }
        System.out.println("Results: "+MAP.size()+" entries, "+no_eng+" missing values and "+no_de+" missing translations.");
    }

    public void save(Button btnSave) {
        btnSave.textProperty().setValue("...");
        btnSave.disableProperty().setValue(true);
        try {
            for (LangReader reader : LANG_IN) {
                new LangWriter(new File(LANG_OUT_DIR, reader.file.getName()), reader.lang_id).write(MAP);
            }
            new LangWriter(new File("text_events.xml.append")).write(MAP);
            new LangWriter(new File("text_blueprints.xml.append")).write(MAP);
            new LangWriter(new File("text-de.xml"), "de").write(MAP);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        btnSave.textProperty().setValue("Save");
        btnSave.disableProperty().setValue(false);
    }

}
