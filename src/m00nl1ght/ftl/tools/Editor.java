package m00nl1ght.ftl.tools;

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import m00nl1ght.ftl.tools.Patcher.TagType;
import m00nl1ght.ftl.tools.translation.TranslationHelper;

import java.awt.*;
import java.awt.Desktop.Action;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Editor extends Application {

    static final double SUG_TRESHOLD = 100;
    static final File LANG_IN_DIR = new File("./lang_in/");
    static final File LANG_OUT_DIR = new File("./lang_out/");
    static final File PATCHER_IN_DIR = new File("./patcher_in/");
    static final File PATCHER_OUT_DIR = new File("./patcher_out/");
    static final List<String> PREFIX_LIST = Arrays.asList("ce", "event", "eventList", "textList", "text", "ship");

    static Patcher EVENT_PATCHER, BLUEPRINT_PATCHER;
    static TagType TAG_EVENT, TAG_CHOICE, TAG_REMOVE_CREW, TAG_EVENT_LIST, TAG_TEXT_LIST, TAG_SHIP, TAG_SHIP_DESTROYED, TAG_SHIP_DEAD_CREW;
    static TagType TAG_BP_SYSTEM, TAG_BP_WEAPON, TAG_BP_CREW, TAG_BP_CREW_POWER, TAG_BP_AUG, TAG_BP_DRONE, TAG_BP_SHIP;

    final List<LangReader> LANG_IN = new ArrayList<>();
    final Map<String, LangEntry> MAP = new LinkedHashMap<>();
    final TranslationHelper helper = new TranslationHelper(this::suggCallback);
    private TreeView<LangEntry> tree;
    private TextArea langA, langB;
    private Button btnSuggest;
    private Scene mainScene;
    private Stage primaryStage;

    public static void main(String... args) {

        LANG_OUT_DIR.mkdirs();
        PATCHER_OUT_DIR.mkdirs();

        TAG_EVENT_LIST = new TagType("eventList", (n, p, id, d, tag) -> "ce_eventList_" + n);
        TAG_EVENT = new TagType("event", (n, p, id, d, tag) -> p==null?("ce_event_" + n):(p==TAG_CHOICE?(n + "_e"):(n + '_' + id)), "text");
        TAG_CHOICE = new TagType("choice", (n, p, id, d, tag) -> n + "_c" + id, "text");
        TAG_REMOVE_CREW = new TagType("removeCrew", (n, p, id, d, tag) -> n + "_clone", "text");
        TAG_TEXT_LIST = new TagType("textList", (n, p, id, d, tag) -> "ce_textList_" + n + '_' + d, "text");
        TAG_SHIP = new TagType("ship", (n, p, id, d, tag) -> "ce_ship_" + n);
        TAG_SHIP_DESTROYED = new TagType("destroyed", (n, p, id, d, tag) -> n + "_destroyed", "text");
        TAG_SHIP_DEAD_CREW = new TagType("deadCrew", (n, p, id, d, tag) -> n + "_deadCrew", "text");

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

        TAG_BP_SYSTEM = new TagType("systemBlueprint", (n, p, id, d, tag) -> "ce_systemBlueprint_" + n + '_' + tag, "title", "desc");
        TAG_BP_WEAPON = new TagType("weaponBlueprint", (n, p, id, d, tag) -> "ce_weaponBlueprint_" + n + '_' + tag, "title", "desc", "short", "tooltip");
        TAG_BP_CREW = new TagType("crewBlueprint", (n, p, id, d, tag) -> "ce_crewBlueprint_" + n + '_' + tag, "title", "desc", "short");
        TAG_BP_CREW_POWER = new TagType("powerList", (n, p, id, d, tag) -> n + '_' + d, "power");
        TAG_BP_AUG = new TagType("augBlueprint", (n, p, id, d, tag) -> "ce_augBlueprint_" + n + '_' + tag, "title", "desc");
        TAG_BP_DRONE = new TagType("droneBlueprint", (n, p, id, d, tag) -> "ce_droneBlueprint_" + n + '_' + tag, "title", "desc", "short");
        TAG_BP_SHIP = new TagType("shipBlueprint", (n, p, id, d, tag) -> "ce_shipBlueprint_" + n + '_' + tag, "unlock", "desc");

        TAG_BP_CREW.sub(TAG_BP_CREW_POWER);

        List<String> blueprint_files = Arrays.asList("blueprints", "dlcBlueprints", "dlcBlueprintsOverwrite");

        BLUEPRINT_PATCHER = new Patcher(PATCHER_IN_DIR, PATCHER_OUT_DIR,"text_blueprints.xml", blueprint_files,
                TAG_BP_SYSTEM, TAG_BP_WEAPON, TAG_BP_CREW, TAG_BP_AUG, TAG_BP_DRONE, TAG_BP_SHIP);

        launch(args);

    }

    @Override
    public void start(Stage primaryStage) {

        this.setup();
        this.primaryStage = primaryStage;
        primaryStage.setTitle("FTL Localization Editor");
        primaryStage.setMaximized(true);

        VBox pane = new VBox();
        pane.setPadding(new Insets(20, 20, 20, 20));
        pane.setSpacing(5);

        langA = new TextArea();
        langA.wrapTextProperty().setValue(true);

        langB = new TextArea();
        langB.wrapTextProperty().setValue(true);

        HBox box = new HBox();
        box.setSpacing(10);

        Button btnSave = new Button("Save");
        btnSave.setOnAction(event -> this.save(btnSave));

        btnSuggest = new Button("Suggest");
        btnSuggest.setOnAction(event -> this.suggest());
        btnSuggest.disableProperty().setValue(true);

        Button btnLookup = new Button("Lookup");
        btnLookup.setOnAction(event -> this.lookup());

        CheckBox chkMissinOnly = new CheckBox("Only show missing translations");
        chkMissinOnly.setOnAction(event -> this.changeMode(chkMissinOnly));

        Button btnExport = new Button("Export Slice");
        btnExport.setOnAction(event -> this.exportSlice());

        Button btnImport = new Button("Import Slice");
        btnImport.setOnAction(event -> this.importSlice());

        box.getChildren().addAll(btnSave, chkMissinOnly, btnSuggest, btnLookup, btnExport, btnImport);

        tree = new TreeView<>();
        tree.setMinSize(-1, 700);
        tree.setShowRoot(false);
        this.changeMode(chkMissinOnly);
        tree.getSelectionModel().selectedItemProperty().addListener(this::changeSel);

        pane.getChildren().addAll(tree, new Label("langA"), langA, new Label("langB"), langB, box);
        mainScene = new Scene(pane);
        primaryStage.setScene(mainScene);
        primaryStage.show();

    }

    @SuppressWarnings({"SimplifyStreamApiCallChains", "SimplifyForEach"})
    private void changeMode(CheckBox box) {
        final TreeItem<LangEntry> root = new TreeItem<>();
        tree.setRoot(root);
        final LinkedHashMap<String, LinkedHashMap<String, List<LangEntry>>> data = new LinkedHashMap<>();
        final Predicate<LangEntry> filter = box.isSelected()?e -> (e.translation.isEmpty() || e.value.isEmpty() || e.translation.endsWith(") ")):e -> true;
        final int[] amount = {0};
        MAP.values().stream().forEachOrdered(entry -> {
            if (filter.test(entry)) {
                String prefix = getIDPrefix(entry);
                data.computeIfAbsent(entry.src, k -> new LinkedHashMap<>()).computeIfAbsent(prefix, k -> new ArrayList<>(8)).add(entry);
                amount[0]++;
            }
        });
        data.forEach((key, value) -> {
            LangEntry e = new LangEntry();
            TreeItem<LangEntry> node = new TreeItem<>(e);
            root.getChildren().add(node);
            if (value.size()==1) {
                value.values().iterator().next().forEach(langEntry -> node.getChildren().add(new TreeItem<>(langEntry)));
            } else {
                value.forEach((s, le) -> {
                    if (le.size()==1) {
                        node.getChildren().add(new TreeItem<>(le.get(0)));
                    } else {
                        if (s.isEmpty()) {
                            le.forEach(langEntry -> node.getChildren().add(new TreeItem<>(langEntry)));
                        } else {
                            LangEntry e1 = new LangEntry();
                            e1.key = s;
                            TreeItem<LangEntry> node1 = new TreeItem<>(e1);
                            node.getChildren().add(node1);
                            le.forEach(langEntry -> node1.getChildren().add(new TreeItem<>(langEntry)));
                        }
                    }
                });
            }
            int total = node.getChildren().stream().mapToInt(list -> list.getChildren().isEmpty() ? 1 : list.getChildren().size()).sum();
            e.key = key + " ("+node.getChildren().size()+ '/' +total+ ')';
        });
        box.setText("("+ amount[0] +" entries) Only show missing translations");
    }

    private String getIDPrefix(LangEntry e) {
        String[] s = e.key.split("_");
        StringBuilder r = new StringBuilder();
        int i = 0; while (i<s.length && (PREFIX_LIST.contains(s[i]))) {i++;}
        if (i>0 && i<s.length && Character.isUpperCase(s[i].charAt(0))) {
            for (int j = 0; j<=i; j++) {if (r.length()>0) r.append('_'); r.append(s[j]);}
            while (i++<s.length-1 && Character.isUpperCase(s[i].charAt(0))) {r.append('_').append(s[i]);}
        }
        return r.toString();
    }

    private void changeSel(ObservableValue<? extends TreeItem<LangEntry>> obs, TreeItem<LangEntry> old, TreeItem<LangEntry> val) {
        if (old!=null && !old.getValue().src.isEmpty()) {
            if (!old.getValue().translation.equals(langB.getText())) {
                old.getValue().translation = langB.getText();
                helper.put(old.getValue().value, langB.getText());
            }
        }
        if (val!=null && !val.getValue().src.isEmpty()) {
            langA.setText(val.getValue().value);
            langB.setText(val.getValue().translation);
            btnSuggest.disableProperty().setValue(false);
        } else {
            langA.setText("");
            langB.setText("");
            btnSuggest.disableProperty().setValue(true);
        }
    }

    public static class LangEntry {
        public String key = "", value = "", translation = "", file = "", src = "";
        public int dupes = 0;
        public String toString() {
            return key+(dupes>0?" ("+dupes+ ')' :"");
        }
    }

    public void setup() {
        try {
            for (File file : Objects.requireNonNull(LANG_IN_DIR.listFiles())) {
                LANG_IN.add(new LangReader(file));
            }
            for (LangReader reader : LANG_IN) {
                reader.read(MAP);
            }
            EVENT_PATCHER.patch(MAP);
            BLUEPRINT_PATCHER.patch(MAP);
            new LangReader(new File("text-de.xml"), "de").read(MAP);
            this.info();
            for (LangEntry entry : MAP.values()) {
                if (entry.file.startsWith("text_events")) {
                    String val = stripAdds(entry.value);
                    String res = stripAdds(entry.translation);
                    if (!val.isEmpty() && !res.isEmpty()) {
                        helper.put(val, res);
                    }
                }
            }
            // helper.dump();
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

    public void suggest() {
        final LangEntry entry = tree.getSelectionModel().getSelectedItem().getValue();
        btnSuggest.textProperty().setValue("...");
        btnSuggest.disableProperty().setValue(true);

        final String sel = langA.getSelectedText();
        final String query = sel.isEmpty() ? langA.getText() : sel;
        helper.setQuery(stripAdds(query));
        switchScene(helper.getScene());
    }

    public void suggCallback(String res) {
        final IndexRange dest = langB.getSelection();
        final String last = langB.getText();
        res = copyAdds(langA.getText(), res);
        if (dest.getLength() == 0) {
            langB.setText(res);
        } else {
            String pre = last.substring(0, dest.getStart());
            String post = last.substring(dest.getEnd());
            langB.setText(pre + res + post);
        }

        btnSuggest.textProperty().setValue("Suggest");
        btnSuggest.disableProperty().setValue(false);
        switchScene(mainScene);
    }

    private void lookup() {
        String str = langA.getSelectedText();
        if (str.isEmpty()) return;
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.dict.cc/?s=" + URLEncoder.encode(str, "UTF-8")));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public static String stripAdds(String str) {
        str = str.trim();

        if (str.startsWith("(")) {
            int c = str.indexOf(')');
            if (c>=3 && c<str.length() - 1) {
                str = str.substring(c + 1).trim();
            }
        }

        if (str.endsWith("]")) {
            int c = str.indexOf('[');
            if (c<str.length() - 4) {
                str = str.substring(0, c).trim();
            }
        }

        return str;
    }

    public String copyAdds(String from, String to) {
        to = stripAdds(to);

        if (from.startsWith("(")) {
            int c = from.indexOf(')');
            if (c>=3) {
                String b = from.substring(1, c);
                to = "(" + b + ") " + to;
            }
        }

        if (from.endsWith("]")) {
            int c = from.indexOf('[');
            if (c<from.length() - 4) {
                String s = from.substring(c + 1, from.length() - 1);
                to = to + " [" + s + "]";
            }
        }

        return to;
    }

    public void switchScene(Scene scene) {
        final boolean maximized = primaryStage.isMaximized();
        double w = primaryStage.getWidth();
        double h = primaryStage.getHeight();
        primaryStage.setScene(scene);
        primaryStage.setWidth(w);
        primaryStage.setHeight(h);
        primaryStage.setMaximized(maximized);
    }

    private static final int SLICE_SIZE = 25;
    private static final String SLICE_FILE = "slice.txt";

    public void exportSlice() {

        final List<LangEntry> missing = MAP.values().stream()
                .filter(e -> e.translation.isEmpty() || e.translation.endsWith(") ")).collect(Collectors.toList());

        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(SLICE_FILE))) {
            for (int i = 0; i < SLICE_SIZE && i < missing.size(); i++) {
                final LangEntry entry = missing.get(i);
                final String from = stripAdds(entry.value);
                writer.append(from);
                writer.newLine();
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void importSlice() {

        final List<LangEntry> missing = MAP.values().stream()
                .filter(e -> e.translation.isEmpty() || e.translation.endsWith(") ")).collect(Collectors.toList());

        try {
            final List<String> lines = Files.readAllLines(new File(SLICE_FILE).toPath());
            int i = 0; for (final String line : lines) {
                if (line.isEmpty()) continue;
                if (i >= missing.size()) break;
                final LangEntry entry = missing.get(i);
                if (!entry.translation.isEmpty()) throw new IllegalStateException();
                entry.translation = copyAdds(entry.value, line.trim());
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
