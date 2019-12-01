package m00nl1ght.ftl.tools.translation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GoogleTranslate {

    private static final Map<String, String> TUNE_TABLE = new LinkedHashMap<>();
    private static final Map<String, String> TUNE_TABLE_UNQUOTED = new LinkedHashMap<>();
    private static final String API_KEY = getApiKey();
    private static final String[] DUMMY = new String[0];
    private static String[] tune_i, tune_o, tune_iq, tune_oq;

    static {
        TUNE_TABLE.put("Besatzungsmitglieder", "Crewmitglieder");
        TUNE_TABLE.put("Leuchtfeuer", "Signalpunkt");
        TUNE_TABLE.put("Besatzung", "Crew");
        TUNE_TABLE.put("hagelt", "ruft");
        TUNE_TABLE.put("hageln", "rufen");
        TUNE_TABLE.put("Hagel", "Nachricht");
        tune_i = TUNE_TABLE.keySet().toArray(DUMMY);
        tune_o = TUNE_TABLE.values().toArray(DUMMY);
        TUNE_TABLE_UNQUOTED.put("Sie", "du");
        TUNE_TABLE_UNQUOTED.put("Ihre", "deine");
        TUNE_TABLE_UNQUOTED.put("Ihr", "dein");
        TUNE_TABLE_UNQUOTED.put("Ihnen", "dir");
        TUNE_TABLE_UNQUOTED.put("haben", "hast");
        TUNE_TABLE_UNQUOTED.put("könnten", "könntest");
        TUNE_TABLE_UNQUOTED.put("können", "kannst");
        TUNE_TABLE_UNQUOTED.put("sollen", "sollst");
        TUNE_TABLE_UNQUOTED.put("entdecken", "entdeckst");
        TUNE_TABLE_UNQUOTED.put("sammeln", "sammelst");
        tune_iq = TUNE_TABLE_UNQUOTED.keySet().toArray(DUMMY);
        tune_oq = TUNE_TABLE_UNQUOTED.values().toArray(DUMMY);
    }

    private GoogleTranslate() {}

    public static String translate(String langFrom, String langTo, String text) {
        try {
            String urlStr = "https://script.google.com/macros/s/" + API_KEY + "/exec" +
                    "?q=" + URLEncoder.encode(text, "UTF-8") +
                    "&target=" + langTo +
                    "&source=" + langFrom;
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String response = in.lines().collect(Collectors.joining());
            in.close();
            return response;
        } catch (Exception e) {
            return "<autotranslation_error>";
        }
    }

    public static String tune(String in) {
        String[] parts = in.split("\"");
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        for (String s : parts) {
            if (!s.isEmpty()) {
                String tuned = tuneSection(s, quoted);
                sb.append(tuned);
            }
            sb.append('"');
            quoted = !quoted;
        }

        if (quoted) sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static String tuneSection(String in, boolean quoted) {
        if (!quoted) in = SearchUtils.replaceEach(in, tune_iq, tune_oq);
        in = SearchUtils.replaceEach(in, tune_i, tune_o).trim();
        return in.substring(0, 1).toUpperCase() + in.substring(1);
    }

    private static String getApiKey() {
        try {
            return Files.readAllLines(Paths.get("api_key.txt")).get(0);
        } catch (Exception e) {
            throw new RuntimeException("failed to read api key", e);
        }
    }

}
