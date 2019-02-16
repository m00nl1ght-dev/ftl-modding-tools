package m00nl1ght.ftl.tools;

import java.io.*;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Patcher {

    private final File source;
    private final File dest;

    private final List<String> TAGS;
    private final List<String> DATA_TAGS;
    private final String TARGET_FILE;
    private String line = null;
    private String name = null;
    private int id = 0;
    private String tagType = "";
    private BufferedWriter writer;
    private Map<String, Editor.LangEntry> existing_keys = new LinkedHashMap<>(); // text -> key
    private Map<String, Editor.LangEntry> MAP;

    public int duplicates = 0;

    public Patcher(File source, File dest, String target_file, List<String> tags, List<String> data_tags) {
        this.source = source;
        this.dest = dest;
        this.TAGS = tags;
        this.TARGET_FILE = target_file;
        this.DATA_TAGS = data_tags;
    }

    public void patch(Map<String, Editor.LangEntry> MAP) throws IOException {

        this.MAP=MAP;
        existing_keys.clear();
        MAP.forEach((key, langEntry) -> {
            if (langEntry.file.equals(TARGET_FILE)) existing_keys.put(langEntry.value, langEntry);
        });

        for(File src : source.listFiles()) {
            System.out.println("Patching file ["+src.getName()+"] for target ["+TARGET_FILE+"] ...");
            File file = new File(dest, src.getName());
            file.delete();
            file.createNewFile();
            FileOutputStream fos0 = new FileOutputStream(file);
            writer = new BufferedWriter(new OutputStreamWriter(fos0));
            Files.lines(src.toPath()).forEachOrdered(this::processLine);
            writer.close();
            fos0.close();
            line = null; name = null; id = 0; tagType = "";
        }

        System.out.println("Files sucessfully patched for target ["+TARGET_FILE+"].");

    }

    private void processLine(String in) {
        try {
            line = in;
            if (tagType.isEmpty()) {
                readTagIfPresent();
            } else if (!readEndIfPresent()) {
                readTextTagIfPresent();
            }
            writer.write(line);
            writer.newLine();
        } catch (Exception e) {
            throw new RuntimeException("exception in line: " + line, e);
        }
    }

    private boolean readTagIfPresent() {
        for (String prefix : TAGS) {
            if (line.startsWith("<"+prefix+" ")) {
                int i = prefix.length()+2;
                while (line.charAt(i) != '"') {i++;}
                int j = i;
                while (line.charAt(j + 1) != '"') {j++;}
                tagType = prefix;
                name = line.substring(i + 1, j + 1);
                id = 0;
                return true;
            }
        }
        return false;
    }

    private boolean readEndIfPresent() {
        if (line.startsWith("</"+tagType+">")) {
            tagType = "";
            name = null;
            id = 0;
            return true;
        }
        return false;
    }

    private boolean readTextTagIfPresent() {
        int tabs = -1;
        String tag = "";
        for(String t : DATA_TAGS) {
            tabs = line.indexOf("<"+t);
            if (tabs>=0) {tag=t; break;}
        }
        if (tabs<0) return false;
        int end = line.indexOf("</"+tag+">");
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
            if (text.length()<=5) {
                try {
                    Float.parseFloat(text);
                    return false;
                } catch (Exception ignored) {}
            }
            String post = line.length() > end + 3 +tag.length() ? line.substring(end + 3 + tag.length()) : "";
            Editor.LangEntry entry = existing_keys.get(text);
            if (entry == null) {
                entry = new Editor.LangEntry();
                entry.key = generateKey(tag);
                entry.value = text;
                entry.file = TARGET_FILE+".append";
                while (MAP.get(entry.key)!=null) {
                    System.out.println("["+TARGET_FILE+"] Existing key: "+entry.key);
                    entry.key += "x";
                }
                existing_keys.put(entry.value, entry);
                MAP.put(entry.key, entry);
                id++;
            } else {
                duplicates++;
            }
            line = pre + " id=\"" + entry.key + "\" />"+post;
            return true;
        }
        return false;
    }

    private String generateKey(String tag) {
        if (tagType.isEmpty()) throw new IllegalStateException();
        String key = "ce_"+tagType+"_";
        key += name;
        if (tag.equals("text")) {
            key += "_" + (id + 1);
        } else {
            key+="_"+tag;
        }
        return key;
    }

}
