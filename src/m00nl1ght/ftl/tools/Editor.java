package m00nl1ght.ftl.tools;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Editor extends Application {

    static final File LANG_IN_DIR = new File("./lang_in/");
    static final File LANG_OUT_DIR = new File("./lang_out/");
    static final File PATCHER_IN_DIR = new File("./patcher_in/");
    static final File PATCHER_OUT_DIR = new File("./patcher_out/");

    static Patcher EVENT_PATCHER, BLUEPRINT_PATCHER;
    static Patcher.TagType TAG_EVENT, TAG_CHOICE, TAG_REMOVE_CREW, TAG_EVENT_LIST, TAG_TEXT_LIST, TAG_SHIP, TAG_SHIP_DESTROYED, TAG_SHIP_DEAD_CREW;
    static Patcher.TagType TAG_BP_SYSTEM, TAG_BP_WEAPON, TAG_BP_CREW, TAG_BP_CREW_POWER, TAG_BP_AUG, TAG_BP_DRONE, TAG_BP_SHIP;

    final List<LangReader> LANG_IN = new ArrayList<>();
    final Map<String, LangEntry> MAP = new LinkedHashMap<>();
    public LangEntry current = null;
    private ListView<LangEntry> list;

    public static void main(String... args) {

        LANG_OUT_DIR.mkdirs();
        PATCHER_OUT_DIR.mkdirs();

        TAG_EVENT_LIST = new Patcher.TagType("eventList", (n, p, id, d, tag) -> "ce_eventList_" + n);
        TAG_EVENT = new Patcher.TagType("event", (n, p, id, d, tag) -> p==null?("ce_event_" + n):(p==TAG_CHOICE?(n + "_e"):(n + "_" + id)), "text");
        TAG_CHOICE = new Patcher.TagType("choice", (n, p, id, d, tag) -> n + "_c" + id, "text");
        TAG_REMOVE_CREW = new Patcher.TagType("removeCrew", (n, p, id, d, tag) -> n + "_clone", "text");
        TAG_TEXT_LIST = new Patcher.TagType("textList", (n, p, id, d, tag) -> "ce_textList_" + n + "_" + d, "text");
        TAG_SHIP = new Patcher.TagType("ship", (n, p, id, d, tag) -> "ce_ship_" + n);
        TAG_SHIP_DESTROYED = new Patcher.TagType("destroyed", (n, p, id, d, tag) -> n + "_destroyed", "text");
        TAG_SHIP_DEAD_CREW = new Patcher.TagType("deadCrew", (n, p, id, d, tag) -> n + "_deadCrew", "text");

        TAG_EVENT_LIST.sub(TAG_EVENT);
        TAG_EVENT.sub(TAG_CHOICE, TAG_REMOVE_CREW);
        TAG_CHOICE.sub(TAG_EVENT);
        TAG_SHIP.sub(TAG_SHIP_DESTROYED, TAG_SHIP_DEAD_CREW);
        TAG_SHIP_DESTROYED.sub(TAG_CHOICE);
        TAG_SHIP_DEAD_CREW.sub(TAG_CHOICE);

        List<String> event_files = Arrays.asList("events", "newEvents", "events_nebula", "events_pirate",
                "events_rebel", "events_engi", "events_mantis", "events_rock", "events_slug", "events_zoltan",
                "events_crystal", "events_fuel", "events_boss", "events_ships", "dlcEvents", "dlcEvents_anaerobic", "dlcEventsOverwrite");

        EVENT_PATCHER = new Patcher(PATCHER_IN_DIR, PATCHER_OUT_DIR,"text_events.xml", event_files,
                TAG_EVENT, TAG_EVENT_LIST, TAG_TEXT_LIST, TAG_SHIP);

        TAG_BP_SYSTEM = new Patcher.TagType("systemBlueprint", (n, p, id, d, tag) -> "ce_systemBlueprint_" + n + "_" + tag, "title", "desc");
        TAG_BP_WEAPON = new Patcher.TagType("weaponBlueprint", (n, p, id, d, tag) -> "ce_weaponBlueprint_" + n + "_" + tag, "title", "desc", "short", "tooltip");
        TAG_BP_CREW = new Patcher.TagType("crewBlueprint", (n, p, id, d, tag) -> "ce_crewBlueprint_" + n + "_" + tag, "title", "desc", "short");
        TAG_BP_CREW_POWER = new Patcher.TagType("powerList", (n, p, id, d, tag) -> n + "_" + d, "power");
        TAG_BP_AUG = new Patcher.TagType("augBlueprint", (n, p, id, d, tag) -> "ce_augBlueprint_" + n + "_" + tag, "title", "desc");
        TAG_BP_DRONE = new Patcher.TagType("droneBlueprint", (n, p, id, d, tag) -> "ce_droneBlueprint_" + n + "_" + tag, "title", "desc", "short");
        TAG_BP_SHIP = new Patcher.TagType("shipBlueprint", (n, p, id, d, tag) -> "ce_shipBlueprint_" + n + "_" + tag, "unlock", "desc");

        TAG_BP_CREW.sub(TAG_BP_CREW_POWER);

        List<String> blueprint_files = Arrays.asList("blueprints", "dlcBlueprints", "dlcBlueprintsOverwrite");

        BLUEPRINT_PATCHER = new Patcher(PATCHER_IN_DIR, PATCHER_OUT_DIR,"text_blueprints.xml", blueprint_files,
                TAG_BP_SYSTEM, TAG_BP_WEAPON, TAG_BP_CREW, TAG_BP_AUG, TAG_BP_DRONE, TAG_BP_SHIP);

        launch(args);

    }

    @SuppressWarnings("SimplifyStreamApiCallChains")
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

        CheckBox chkMissinOnly = new CheckBox("Only show missing translations");
        chkMissinOnly.setOnAction(event -> this.changeMode(chkMissinOnly));

        box.getChildren().addAll(btnSave, chkMissinOnly);

        list = new ListView<>();
        list.setMinSize(-1, 700);
        MAP.values().stream().forEachOrdered(entry ->  list.getItems().add(entry));
        list.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (current!=null) {
                current.translation = langB.getText();
            }
            current = newValue;
            langA.setText(current==null?"":current.value);
            langB.setText(current==null?"":current.translation);
        });

        pane.getChildren().addAll(list, new Label("langA"), langA, new Label("langB"), langB, box);
        Scene scene = new Scene(pane, 1000, 1000);
        //scene.getRoot().setStyle("-fx-base: rgba(60, 63, 65, 255)");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    @SuppressWarnings({"SimplifyStreamApiCallChains", "SimplifyForEach"})
    private void changeMode(CheckBox box) {
        list.getItems().clear();
        if (box.isSelected()) {
            final int[] amount = {0};
            MAP.values().stream().forEachOrdered(entry -> {if (entry.translation.isEmpty() || entry.value.isEmpty() || entry.translation.endsWith(") ")) {list.getItems().add(entry); amount[0]++;}});
            box.setText("Only show missing translations ("+ amount[0] +" entries)");
        } else {
            MAP.values().stream().forEachOrdered(entry -> list.getItems().add(entry));
            box.setText("Only show missing translations");
        }
    }

    public static class LangEntry {
        public String key, value = "", translation = "", file = "";
        public int dupes = 0;
        public String toString() {
            return file.isEmpty()?key:("["+file+"] "+key+(dupes>0?" ("+dupes+")":""));
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
            for (LangEntry e : MAP.values()) {
                if (e.translation.isEmpty() && e.value.startsWith("(")) {
                    int c = e.value.indexOf(')');
                    if (c<3) continue;
                    String b = e.value.substring(1, c);
                    e.translation = "("+b+") ";
                }
            }
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
            if (entry.translation.isEmpty() || entry.translation.endsWith(") ")) no_de++;
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
