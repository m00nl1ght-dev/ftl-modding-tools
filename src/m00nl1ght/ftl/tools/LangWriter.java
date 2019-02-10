package m00nl1ght.ftl.tools;

import java.io.*;
import java.util.Map;

public class LangWriter {

    public final File file;
    private final String lang_id;

    public LangWriter(File file) {
        this.file = file;
        this.lang_id = "";
    }

    public LangWriter(File file, String lang_id) {
        this.file = file;
        this.lang_id = lang_id;
    }

    public void write(Map<String, Editor.LangEntry> keys) throws IOException {

        System.out.println("Generating "+file.getName()+" ...");
        file.delete();
        file.createNewFile();
        FileOutputStream fos1 = new FileOutputStream(file);
        BufferedWriter lang_writer = new BufferedWriter(new OutputStreamWriter(fos1));
        if (file.getName().endsWith(".append")) {
            lang_writer.write("<!-- GENERATED LANG XML (FTL Localization Editor by m00nl1ght_dev) -->");
            lang_writer.newLine();
            lang_writer.newLine();
        } else {
            lang_writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            lang_writer.newLine();
            lang_writer.write("<!-- GENERATED LANG XML (FTL Localization Editor by m00nl1ght_dev) -->");
            lang_writer.newLine();
            lang_writer.newLine();
            lang_writer.write("<FTL>");
            lang_writer.newLine();
        }

        int count = 0;
        if (this.lang_id.isEmpty()) {
            String f = file.getName();
            for (Editor.LangEntry entry : keys.values()) {
                if (entry.file.equals(f)) {
                    lang_writer.write("<text name=\"" + entry.key + "\">" + entry.value + "</text>");
                    lang_writer.newLine();
                    count++;
                }
            }
        } else {
            String last_file = "";
            for (Editor.LangEntry entry : keys.values()) {
                if (entry.translation.isEmpty()) {
                    //System.out.println("WARN: Missing translation for key "+entry.key);
                    continue;
                }
                if (!last_file.equals(entry.file)) {
                    lang_writer.newLine();
                    lang_writer.write("<!-- FILE: "+entry.file+" -->");
                    lang_writer.newLine();
                    lang_writer.newLine();
                    last_file = entry.file;
                }
                lang_writer.write("<text name=\"" + entry.key + "\" language=\""+lang_id+"\">" + entry.translation + "</text>");
                lang_writer.newLine();
                count++;
            }
        }

        if (!file.getName().endsWith(".append")) {
            lang_writer.write("</FTL>");
            lang_writer.newLine();
        }
        lang_writer.close();
        fos1.close();
        System.out.println("Finished ("+count+" entries).");

    }

}
