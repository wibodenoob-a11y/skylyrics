package com.example.skylyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lyrics {
    public static volatile List<Line> lines = List.of();
    public static volatile String status = "";
    private static volatile String loadedKey = "";
    private static volatile long generation = 0L;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private static final ExecutorService EX = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "skylyrics-lyrics");
        t.setDaemon(true);
        return t;
    });

    private static final Pattern TS = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]");
    private static final Pattern WORD_TS = Pattern.compile("<(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?>");

    public static void tick() {
        NowPlaying.Snap s = NowPlaying.snap;
        String key = s == null ? "" : s.title() + "|" + s.artist();
        if (key.equals(loadedKey)) return;

        loadedKey = key;
        lines = List.of();
        if (s == null) {
            status = "";
            return;
        }
        status = "Loading lyrics...";
        long gen = ++generation;
        EX.submit(() -> fetch(s, gen));
    }

    public static void reload() {
        loadedKey = "\0reload";
    }

    public static int indexAt(List<Line> ls, long posMs) {
        int idx = -1;
        for (int i = 0; i < ls.size(); i++) {
            if (ls.get(i).timeMs() <= posMs) idx = i;
            else break;
        }
        return idx;
    }

    /** Returns the index of the currently active word inside a line, or -1 */
    public static int wordIndexAt(Line line, long posMs) {
        if (line.words == null || line.words.isEmpty()) return -1;
        int idx = -1;
        for (int i = 0; i < line.words.size(); i++) {
            if (line.words.get(i).timeMs() <= posMs) idx = i;
            else break;
        }
        return idx;
    }

    private static void fetch(NowPlaying.Snap s, long gen) {
        try {
            String q = URLEncoder.encode(s.title() + " " + s.artist(), StandardCharsets.UTF_8);
            // Try LRCLIB first (supports synced + sometimes word level via enhanced)
            String url = "https://lrclib.net/api/search?q=" + q;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "SkyLyrics/0.6")
                    .GET()
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (gen != generation) return;

            if (resp.statusCode() == 200) {
                JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    String synced = obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull()
                            ? obj.get("syncedLyrics").getAsString() : null;
                    if (synced != null && !synced.isBlank()) {
                        List<Line> parsed = parseLrc(synced);
                        if (!parsed.isEmpty()) {
                            lines = parsed;
                            status = "";
                            return;
                        }
                    }
                }
            }
            status = "No synced lyrics found";
        } catch (Exception e) {
            if (gen == generation) status = "Failed to load lyrics";
        }
    }

    private static List<Line> parseLrc(String lrc) {
        List<Line> result = new ArrayList<>();
        String[] rawLines = lrc.split("\\R");
        for (String raw : rawLines) {
            Matcher m = TS.matcher(raw);
            long time = -1;
            int end = 0;
            while (m.find()) {
                time = parseTimestamp(m.group(1), m.group(2), m.group(3));
                end = m.end();
            }
            if (time < 0) continue;
            String text = raw.substring(end).trim();
            if (text.isEmpty()) continue;

            // Try to extract word-level timestamps if present (enhanced LRC)
            List<Word> words = parseWords(text, time);
            String cleanText = text.replaceAll("<\\d{1,3}:\\d{1,2}(?:[.:]\\d{1,3})?>", "").trim();
            result.add(new Line(time, cleanText, words));
        }
        result.sort(Comparator.comparingLong(Line::timeMs));
        return result;
    }

    private static List<Word> parseWords(String text, long lineStart) {
        List<Word> words = new ArrayList<>();
        Matcher m = WORD_TS.matcher(text);
        int lastEnd = 0;
        long lastTime = lineStart;
        while (m.find()) {
            String before = text.substring(lastEnd, m.start()).trim();
            if (!before.isEmpty()) {
                // previous word without timestamp gets estimated
                words.add(new Word(lastTime, before));
            }
            long t = parseTimestamp(m.group(1), m.group(2), m.group(3));
            lastTime = t;
            lastEnd = m.end();
        }
        String remaining = text.substring(lastEnd).trim();
        if (!remaining.isEmpty()) {
            words.add(new Word(lastTime, remaining));
        }

        // If no word timestamps found, create estimated progressive words
        if (words.isEmpty() || words.size() == 1) {
            words.clear();
            String[] parts = text.replaceAll("<[^>]+>", "").trim().split("\\s+");
            if (parts.length == 0) return List.of();
            // Estimate ~350ms per word as a reasonable karaoke speed
            for (int i = 0; i < parts.length; i++) {
                words.add(new Word(lineStart + i * 350L, parts[i]));
            }
        }
        return words;
    }

    private static long parseTimestamp(String min, String sec, String frac) {
        long ms = Long.parseLong(min) * 60_000L + Long.parseLong(sec) * 1000L;
        if (frac != null) {
            if (frac.length() == 2) ms += Long.parseLong(frac) * 10L;
            else if (frac.length() == 3) ms += Long.parseLong(frac);
            else ms += Long.parseLong(frac) * 100L;
        }
        return ms;
    }

    public record Line(long timeMs, String text, List<Word> words) {}
    public record Word(long timeMs, String text) {}
}
