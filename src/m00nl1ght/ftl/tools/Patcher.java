package m00nl1ght.ftl.tools;

import java.io.*;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class Patcher {

    private final File source;
    private final File dest;

    private String line = null;
    private String name = null;
    private int id = 0;
    private TagState tag = TagState.NONE;
    private BufferedWriter writer;
    private Map<String, Editor.LangEntry> keys = new LinkedHashMap<>(); // text -> key

    public int duplicates = 0;

    public Patcher(File source, File dest) {
        this.source = source;
        this.dest = dest;
    }

    public void patch(Map<String, Editor.LangEntry> MAP) throws IOException {

        MAP.forEach((key, langEntry) -> keys.put(langEntry.value, langEntry));

        for(File src : source.listFiles()) {
            System.out.println("Patching file "+src.getName()+" ...");
            File file = new File(dest, src.getName());
            file.delete();
            file.createNewFile();
            FileOutputStream fos0 = new FileOutputStream(file);
            writer = new BufferedWriter(new OutputStreamWriter(fos0));
            Files.lines(src.toPath()).forEachOrdered(this::processLine);
            writer.close();
            fos0.close();
            line = null; name = null; id = 0; tag = TagState.NONE;
        }

        keys.forEach((text, langEntry) -> MAP.put(langEntry.key, langEntry));

        System.out.println("Files sucessfully patched.");

    }

    private enum TagState {
        NONE, EVENT_LIST, EVENT, TEXT_LIST
    }

    private void processLine(String in) {
        try {
            line = in;
            switch (tag) {
                case NONE:
                    if (readTagIfPresent("<eventList ", TagState.EVENT_LIST)) break;
                    if (readTagIfPresent("<event ", TagState.EVENT)) break;
                    if (readTagIfPresent("<textList ", TagState.TEXT_LIST)) break;
                    break;
                case EVENT_LIST:
                    if (readEndIfPresent("</eventList>")) break;
                    readTextTagIfPresent();
                    break;
                case EVENT:
                    if (readEndIfPresent("</event>")) break;
                    readTextTagIfPresent();
                    break;
                case TEXT_LIST:
                    if (readEndIfPresent("</textList>")) break;
                    readTextTagIfPresent();
                    break;
            }
            writer.write(line);
            writer.newLine();
        } catch (Exception e) {
            throw new RuntimeException("exception in line: " + line, e);
        }
    }

    private boolean readTagIfPresent(String prefix, TagState type) {
        if (line.startsWith(prefix)) {
            int i = prefix.length();
            while (line.charAt(i) != '"') {i++;}
            int j = i;
            while (line.charAt(j + 1) != '"') {j++;}
            tag = type;
            name = line.substring(i + 1, j + 1);
            id = 0;
            return true;
        }
        return false;
    }

    private boolean readEndIfPresent(String suffix) {
        if (line.startsWith(suffix)) {
            tag = TagState.NONE;
            name = null;
            id = 0;
            return true;
        }
        return false;
    }

    private boolean readTextTagIfPresent() {
        int tabs = line.indexOf("<text");
        if (tabs<0) return false;
        int end = line.indexOf("</text>");
        if (end>tabs) {
            for (int i = 0; i < tabs; i++) {
                if (!Character.isWhitespace(line.charAt(i))) {
                    //System.out.println("WARN: Unknown symbol (line start), ignoring: "+line);
                    return false;
                }
            }
            int pre_idx = line.indexOf('>');
            String pre = line.substring(0, pre_idx);
            String text = line.substring(pre_idx + 1, end);
            String post = line.length() > end + 7 ? line.substring(end + 7) : "";
            Editor.LangEntry entry = keys.get(text);
            if (entry == null) {
                entry = new Editor.LangEntry();
                entry.key = generateKey();
                entry.value = text;
                entry.file = "text_events.xml.append";
                keys.put(entry.value, entry);
                id++;
            } else {
                duplicates++;
            }
            line = pre + " id=\"" + entry.key + "\" />"+post;
            return true;
        }
        return false;
    }

    private String generateKey() {
        String key = "ce_event_";
        switch (tag) {
            case NONE:
                throw new IllegalStateException();
            case EVENT_LIST:
                key += name;
                key += "_" + (id + 1);
                break;
            case EVENT:
                key += name;
                key += "_" + (id + 1);
                break;
            case TEXT_LIST:
                key += name;
                key += "_" + (id + 1);
                break;
        }
        return key;
    }

}
