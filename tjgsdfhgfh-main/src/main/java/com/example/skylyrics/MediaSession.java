package com.example.skylyrics;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple media session bridge.
 * On real systems this would use DBus (Linux), Windows Media Session, or a small native helper.
 * For the open-source version we provide a clean placeholder that can be extended,
 * and also support a simple HTTP status endpoint if you run a local bridge.
 */
public class MediaSession {
    private static final ScheduledExecutorService SCH = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "skylyrics-media");
        t.setDaemon(true);
        return t;
    });

    public static void start() {
        // Poll every 500ms – in a full implementation this would listen to OS media sessions
        SCH.scheduleAtFixedRate(MediaSession::poll, 0, 500, TimeUnit.MILLISECONDS);
    }

    private static void poll() {
        // Placeholder: in production replace with real MediaSession / playerctl / Windows API
        // For testing you can hard-code or read from a local file / HTTP endpoint.
        // Example of what a real update looks like:
        // NowPlaying.update("Song Title", "Artist", 180000, 45000, true);
    }
}
