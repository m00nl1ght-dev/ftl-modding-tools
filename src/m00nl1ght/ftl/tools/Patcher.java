package m00nl1ght.ftl.tools;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.IntStream;

public class Patcher {

    private static final TagType[] EMPTY_SUB = new TagType[0];
    private static final boolean DEBUG_PATCHER = false;

    private final List<String> source;
    private final File sourceDir, destDir;

    private final TagType[] TAGS;
    private final String TARGET_FILE;
    private String line = null;
    private String name = null, srcfile = null;
    private boolean inComment = false;
    private List<TagType> tags = new ArrayList<>();
    private int[] tag_ids = new int[20];
    private int data_id = 0;
    private BufferedWriter writer;
    private Map<String, Editor.LangEntry> existing_keys = new LinkedHashMap<>(); // text -> key
    private Map<String, Editor.LangEntry> MAP;

    public Patcher(File sourceDir, File destDir, String target_file, List<String> source, TagType... tags) {
        this.source = source;
        this.sourceDir = sourceDir;
        this.destDir = destDir;
        this.TAGS = tags;
        this.TARGET_FILE = target_file;
    }

    public static class TagType {
        final String name;
        final String[] data;
        final INameFunction nf;
        TagType[] sub = EMPTY_SUB;

        public TagType(String name, INameFunction nf, String... data) {
            this.name = name;
            this.nf = nf;
            this.data = data;
        }

        public void sub(TagType... sub) {
            this.sub = sub;
        }
    }

    public interface INameFunction {
        String apply(String name, TagType parent, int tag_id, int data_id, String tag);
    }

    public void patch(Map<String, Editor.LangEntry> MAP) throws IOException {

        this.MAP=MAP;
        existing_keys.clear();
        MAP.forEach((key, langEntry) -> {
            if (langEntry.file.equals(TARGET_FILE)) existing_keys.put(langEntry.value, langEntry);
        });

        for(String sName : source) {
            srcfile = sName;
            File src = new File(sourceDir, sName+".xml.append");
            System.out.println("Patching file ["+src.getName()+"] for target ["+TARGET_FILE+"] ...");
            File file = new File(destDir, src.getName());
            file.delete();
            file.createNewFile();
            FileOutputStream fos0 = new FileOutputStream(file);
            writer = new BufferedWriter(new OutputStreamWriter(fos0));
            Files.lines(src.toPath()).forEachOrdered(this::processLine);
            writer.close();
            fos0.close();
            line = null; name = null; Arrays.fill(tag_ids, 0); tags.clear();
        }

        System.out.println("Files sucessfully patched for target ["+TARGET_FILE+"].");

    }

    private void processLine(String in) {
        try {
            line = in;
            if (inComment || line.trim().startsWith("<!--")) {
                inComment = !line.contains("-->");
            } else if (tags.isEmpty()) {
                readTagIfPresent();
            } else if (!readEndIfPresent() && !readSubTagIfPresent()) {
                readDataTagIfPresent();
            }
            writer.write(line);
            writer.newLine();
        } catch (Exception e) {
            throw new RuntimeException("exception in line: " + line, e);
        }
    }

    private boolean readTagIfPresent() {
        for (TagType type : TAGS) {
            if (line.startsWith("<"+type.name+" ")) {
                int i = type.name.length()+2;
                while (line.charAt(i) != '"') {i++;}
                int j = i;
                while (line.charAt(j + 1) != '"') {j++;}
                tags.clear();
                tags.add(type);
                name = line.substring(i + 1, j + 1);
                Arrays.fill(tag_ids, 0);
                tag_ids[0]++;
                data_id = 0;
                return true;
            }
        }
        return false;
    }

    private boolean readSubTagIfPresent() {
        for (TagType type : tags.get(tags.size()-1).sub) {
            if (line.contains("<"+type.name) && !line.contains("/>")) {
                tags.add(type);
                tag_ids[tags.size() - 1]++;
                data_id = 0;
                return true;
            }
        }
        return false;
    }

    private boolean readEndIfPresent() {
        if (line.contains("</"+tags.get(tags.size()-1).name+">")) {
            tags.remove(tags.size() - 1);
            Arrays.fill(tag_ids, tags.size() + 1, tag_ids.length - 1, 0);
            data_id = 0;
            return true;
        }
        if (tags.size()>1 && line.contains("</"+tags.get(tags.size()-2).name+">")) {
            throw new IllegalStateException("Malformed xml, expected end tag </"+tags.get(tags.size()-1)+"> but found parent end tag </"+tags.get(tags.size()-2)+">");
        }
        return false;
    }

    private boolean readDataTagIfPresent() {
        int tabs = -1;
        String tag = "";
        for(String t : tags.get(tags.size()-1).data) {
            tabs = line.indexOf("<"+t);
            if (tabs>=0) {tag=t; break;}
        }
        if (tabs<0) return false;
        int end = line.indexOf("</"+tag+">");
        if (end>tabs) {
            for (int i = 0; i < tabs; i++) {
                if (!Character.isWhitespace(line.charAt(i))) {
                    if (DEBUG_PATCHER) System.out.println("WARN: Unknown symbol (line start), ignoring: "+line);
                    return false;
                }
            }
            int pre_idx = line.indexOf('>');
            String pre = line.substring(0, pre_idx);
            String text = line.substring(pre_idx + 1, end);
            String post = line.length() > end + 3 +tag.length() ? line.substring(end + 3 + tag.length()) : "";
            if (DEBUG_PATCHER && !post.isEmpty()) {
                if (IntStream.range(0, post.length()).anyMatch(i -> !Character.isWhitespace(post.charAt(i)))) {
                    System.out.println("WARN: Unknown symbol (line end), appending: " + line);
                }
            }
            Editor.LangEntry entry = existing_keys.get(text);
            data_id++;
            if (entry == null) {
                entry = new Editor.LangEntry();
                entry.key = generateKey(tag);
                if (entry.key.isEmpty()) return false;
                entry.value = text;
                entry.file = TARGET_FILE+".append";
                entry.src = srcfile;
                existing_keys.put(entry.value, entry);
                MAP.put(entry.key, entry);
            } else {
                entry.dupes++;
            }
            line = pre + " id=\"" + entry.key + "\" />"+post;
            return true;
        }
        return false;
    }

    private String generateKey(String tag) {
        String key = name;
        for (int i = 0; i < tags.size(); i++) {
            key = tags.get(i).nf.apply(key, i>0?tags.get(i-1):null, tag_ids[i], i==tags.size()-1?data_id:-1, tag);
        }
        if (MAP.get(key)!=null) {
            System.out.println("[WARN] Existing key, overriding: "+key);
        }
        return key;
    }

}
