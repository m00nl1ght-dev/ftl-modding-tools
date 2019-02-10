package m00nl1ght.ftl.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public class LangReader {

    public final File file;
    private Map<String, Editor.LangEntry> keys;
    public final String lang_id;

    public LangReader(File file) {
        this.file = file;
        this.lang_id = "";
    }

    public LangReader(File file, String lang_id) {
        this.file = file;
        this.lang_id = lang_id;
    }

    public void read(Map<String, Editor.LangEntry> dest) throws IOException {
        keys = dest;
        if (file.exists()) {
            System.out.println("Reading lang file "+file.getName()+" ...");
            BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
            br.lines().forEachOrdered(this::processLangLine);
            br.close();
        }
    }

    private void processLangLine(String line) {
        if (line.startsWith("<text ")) {
            int i = 6;
            while (line.charAt(i) != '"') {i++;}
            int j = i;
            while (line.charAt(j + 1) != '"') {j++;}
            if (!lang_id.isEmpty()) {
                int f = j + 1;
                while (line.charAt(f + 1) != '"') {f++;}
                int g = f + 1;
                while (line.charAt(g + 1) != '"') {g++;}
                String lang = line.substring(f + 2, g + 1);
                if (!lang.equals(this.lang_id)) System.out.println("no: "+lang+" i "+i+" j "+j+" f "+f+" g "+g);
            }
            String key = line.substring(i + 1, j + 1);
            int end = line.indexOf("</text>");
            String text = line.substring(line.indexOf('>') + 1, end);
            Editor.LangEntry entry = keys.get(key);
            if (entry == null) {
                entry = new Editor.LangEntry();
                entry.key = key;
                if (this.lang_id.isEmpty()) {
                    entry.value = text;
                    entry.file = this.file.getName();
                } else {
                    entry.translation = text;
                }
                keys.put(entry.key, entry);
            } else {
                if (this.lang_id.isEmpty()) {
                    if (!entry.value.isEmpty() && entry.file.endsWith("append")) {
                        return;
                    }
                    entry.value = text;
                    entry.file = this.file.getName();
                } else {
                    entry.translation = text;
                }
            }
        }
    }

}
