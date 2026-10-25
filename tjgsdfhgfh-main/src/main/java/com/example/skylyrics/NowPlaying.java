package com.example.skylyrics;

public class NowPlaying {
    public static volatile Snap snap = null;
    public static volatile String status = "Looking for a media player...";

    public static void update(String title, String artist, long durMs, long posMs, boolean playing) {
        snap = new Snap(title, artist, durMs, Math.max(0L, posMs), System.nanoTime(), playing);
        status = "";
    }

    public static void clear(String why) {
        snap = null;
        status = why;
    }

    public static long positionMs() {
        Snap s = snap;
        if (s == null) return 0L;
        long p = s.basePosMs();
        if (s.playing()) {
            p += (System.nanoTime() - s.baseNanos()) / 1_000_000L;
        }
        if (s.durationMs() > 0L) {
            p = Math.min(p, s.durationMs());
        }
        return Math.max(0L, p);
    }

    public record Snap(String title, String artist, long durationMs, long basePosMs, long baseNanos, boolean playing) {}
}
