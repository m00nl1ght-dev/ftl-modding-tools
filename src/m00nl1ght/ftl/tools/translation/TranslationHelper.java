package m00nl1ght.ftl.tools.translation;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class TranslationHelper {

    private static final Pattern SECTION_SPLIT_PATTERN = Pattern.compile("[.?!:]");
    private static final Pattern FULL_SPLIT_PATTERN = Pattern.compile("[.?!:\"]");

    private final Map<String, String> dictionary = new HashMap<>();
    private final List<VBox> boxes = new ArrayList<>(10);
    private final List<TextArea> textAreas = new ArrayList<>(10);
    private final List<ComboBox<Suggestion>> comboBoxes = new ArrayList<>(10);
    private final Consumer<String> callback;
    private final List<String> sections = new ArrayList<>();
    private final List<String> splits = new ArrayList<>();
    private final VBox mainBox = new VBox();
    private final HBox btnBox = new HBox();
    private final TextArea baseSgText = new TextArea();
    private final Button acceptBaseBtn = new Button("Accept Compound");
    private final Scene scene = buildScene();

    public TranslationHelper(Consumer<String> callback) {
        this.callback = callback;
    }

    private Scene buildScene() {
        VBox pane = new VBox();
        pane.setPadding(new Insets(20, 20, 20, 20));
        pane.setSpacing(15);

        mainBox.setSpacing(15);
        for (int i = 0; i < 8; i++) {
            final VBox box = new VBox();
            box.setSpacing(5);
            boxes.add(box);
            final TextArea textArea = new TextArea();
            textArea.wrapTextProperty().setValue(true);
            textArea.setPrefHeight(20);
            textAreas.add(textArea);
            final ComboBox<Suggestion> comboBox = new ComboBox<>();
            comboBox.setPrefWidth(2000);
            comboBox.setEditable(true);
            comboBoxes.add(comboBox);
            box.getChildren().addAll(textArea, comboBox);
        }

        final Button button = new Button("Accept");
        button.setOnAction(e -> callback.accept(getResult()));
        final Button buttonC = new Button("Cancel");
        buttonC.setOnAction(e -> callback.accept(""));
        acceptBaseBtn.setOnAction(e -> callback.accept(getFullResult()));
        acceptBaseBtn.setVisible(false);
        btnBox.setSpacing(10);
        btnBox.getChildren().addAll(buttonC, button, acceptBaseBtn);
        baseSgText.setPrefHeight(100);

        pane.getChildren().addAll(mainBox, btnBox);
        return new Scene(pane);
    }

    public void setQuery(String query) {
        mainBox.getChildren().clear();
        sections.clear(); splits.clear();

        CompletableFuture<String> gtTask = CompletableFuture.supplyAsync(() ->
                GoogleTranslate.translate("en", "de", query));

        split(query, sections, splits);
        for (int i = 0; i < sections.size() && i < 8; i++) {
            final String str = sections.get(i);
            textAreas.get(i).setText(str);
            final ComboBox<Suggestion> comboBox = comboBoxes.get(i);
            comboBox.getItems().clear();
            lookup(comboBox.getItems(), str, 0.6D);
            if (!comboBox.getItems().isEmpty()) comboBox.getSelectionModel().select(0);
            mainBox.getChildren().add(boxes.get(i));
        }

        Suggestion fullSg = lookupBest(query, 0.6D);

        try {
            String gtResult = gtTask.get();
            List<String> gtSections = new ArrayList<>();
            split(gtResult, gtSections, null);
            if (gtSections.size() == sections.size()) {
                boolean quoted = false;
                for (int i = 0; i < sections.size() && i < 8; i++) {
                    if (splits.get(i).contains("\"")) quoted = !quoted;
                    final String str = GoogleTranslate.tuneSection(gtSections.get(i), quoted);
                    final ComboBox<Suggestion> comboBox = comboBoxes.get(i);
                    comboBox.getItems().add(new Suggestion(sections.get(i), str.trim(), -1F));
                    comboBox.getSelectionModel().select(0);
                }
            } else if (fullSg == null) {
                fullSg = new Suggestion("", GoogleTranslate.tune(gtResult), -1F);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (fullSg != null) {
            baseSgText.setText(fullSg.translation);
            mainBox.getChildren().add(baseSgText);
            acceptBaseBtn.setVisible(true);
        } else {
            acceptBaseBtn.setVisible(false);
        }
    }

    public String getResult() {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < sections.size(); i++) {
            builder.append(splits.get(i));
            ComboBox<Suggestion> comboBox = comboBoxes.get(i);
            int sel = comboBox.getSelectionModel().getSelectedIndex();
            if (sel >= 0) {
                builder.append(comboBox.getItems().get(sel).translation);
            }
        }

        if (splits.size() > sections.size()) builder.append(splits.get(sections.size()));
        return builder.toString().trim();
    }

    public String getFullResult() {
        String str = baseSgText.getText();
        return str.trim();
    }

    private List<Suggestion> lookup(List<Suggestion> res, String query, double threshold) {
        for (Entry<String, String> entry : dictionary.entrySet()) {
            final double val = SearchUtils.similarity(query, entry.getKey());
            if (val >= threshold) res.add(new Suggestion(entry.getKey(), entry.getValue(), val));
        }
        Collections.sort(res);
        return res;
    }

    private Suggestion lookupBest(String query, double threshold) {
        double bestV = -1D; Suggestion best = null;
        for (Entry<String, String> entry : dictionary.entrySet()) {
            final double val = SearchUtils.similarity(query, entry.getKey());
            if (val > bestV && val >= threshold) {
                bestV = val; best = new Suggestion(entry.getKey(), entry.getValue(), val);
            }
        }
        return best;
    }

    public void put(String phrase, String translation) {
        phrase = phrase.trim(); translation = translation.trim();
        if (phrase.isEmpty() || translation.isEmpty()) return;
        String[] p1 = phrase.split("\"");
        String[] t1 = translation.split("\"");
        if (p1.length != t1.length) return;
        for (int i = 0; i < p1.length; i++) {
            String[] p2 = SECTION_SPLIT_PATTERN.split(p1[i]);
            String[] t2 = SECTION_SPLIT_PATTERN.split(t1[i]);
            if (p2.length == t2.length) {
                for (int k = 0; k < p2.length; k++) {
                    final String val = p2[k].trim();
                    final String res = t2[k].trim();
                    if (!val.isEmpty() && !res.isEmpty() && !val.equals(res)) {
                        dictionary.put(val, res);
                    }
                }
            } else {
                dictionary.put(phrase, translation);
                final String val = p1[i].trim();
                final String res = t1[i].trim();
                if (!val.isEmpty() && !res.isEmpty() && !val.equals(res)) {
                    dictionary.put(val, res);
                }
            }
        }
    }

    private void split(String str, List<String> sections, List<String> splits) {
        boolean flag = true; int k = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            boolean s = c == '.' || c == ':' || c == '?' || c == '!' || c == '"' || (flag && c == ' ');
            if (s != flag) {
                String r = str.substring(k, i);
                if (flag) {
                    if (splits != null) splits.add(r);
                    flag = false; k = i;
                } else {
                    r = r.trim();
                    if (!sections.contains(r)) sections.add(r);
                    flag = true; k = i;
                    while (k > 0 && str.charAt(k - 1) == ' ') k--;
                }
            }
        }

        String r = str.substring(k);
        if (flag) {
            if (splits != null) splits.add(r);
        } else {
            r = r.trim();
            if (!sections.contains(r)) sections.add(r);
        }
    }

    public void dump() {
        System.out.println("############### DICT DUMP ###############");
        for (Entry<String, String> e : dictionary.entrySet()) {
            System.out.println(e.getKey() + " => " + e.getValue());
        }
        System.out.println("(" + dictionary.size() + " entries)");
        System.out.println("#########################################");
    }

    public Scene getScene() {
        return scene;
    }

    /**
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    private static class Suggestion implements Comparable<Suggestion> {
        private final String value;
        private final String translation;
        private final double match;

        private Suggestion(String value, String translation, double match) {
            this.value = value;
            this.translation = translation;
            this.match = match;
        }

        @Override
        public int compareTo(Suggestion o) {
            return Double.compare(o.match, match);
        }

        @Override
        public String toString() {
            return translation + " (" + match + ")";
        }

    }

}
