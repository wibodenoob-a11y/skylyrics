package com.example.skylyrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skylyrics.json");
    public static Config I = new Config();

    // Core
    public boolean showOverlay = true;
    public boolean showSpectrum = true;
    public int cardCorner = 0;
    public boolean showLyrics = true;
    public boolean showContext = true;
    public int lyricsMode = 2;              // 0=Follow, 1=Sky, 2=Screen (floating bar)
    public int followSmooth = 1;
    public float lyricsOpacity = 0.85f;
    public boolean keepCenterClear = true;
    public float lyricsSize = 1.4f;
    public int lyricsOffsetMs = 0;
    public int accentColor = 0xFF9B5DE5;   // nice purple matching the screenshot

    // New features
    public boolean splitLyrics = false;     // left / right split
    public int lyricsAlign = 0;             // 0=Center, 1=Left, 2=Right
    public float lyricsY = 0.78f;           // vertical position (bottom-ish like the image)
    public boolean fadePrevious = true;     // previous lines fade out (when false = show previous at full strength)
    public int wordRevealMode = 0;          // 0 = Progressive (one after another), 1 = Whole line (wait)
    public boolean showSongTitleIntro = true;
    public boolean glowEnabled = true;

    // Word limit / split
    public boolean limitWordsPerLine = true; // enable max words window
    public int maxWordsPerLine = 6;          // how many words to show on the bar at once

    // Colors
    public int pastWordColor = 0xFFCCCCCC;   // already sung words
    public int futureWordColor = 0x66FFFFFF; // not-yet-sung words
    public int barBgColor = 0xE01A1A1E;      // floating bar background
    public int previousLineColor = 0x55FFFFFF; // previous line when faded

    // Color palette indices (for cycling in settings)
    public int accentPaletteIdx = 0;
    public int pastPaletteIdx = 0;
    public int futurePaletteIdx = 0;
    public int bgPaletteIdx = 0;

    public static final int[] ACCENT_PALETTE = {
            0xFF9B5DE5, // purple
            0xFF00F5D4, // cyan
            0xFFFF6B6B, // coral
            0xFFFFD166, // gold
            0xFF06D6A0, // mint
            0xFF118AB2, // blue
            0xFFEF476F, // pink-red
            0xFFFFFFFF  // white
    };

    public static final int[] PAST_PALETTE = {
            0xFFCCCCCC,
            0xFFAAAAAA,
            0xFF888888,
            0xFF66FFFFFF,
            0xFF9B5DE5,
            0xFF00F5D4
    };

    public static final int[] FUTURE_PALETTE = {
            0x66FFFFFF,
            0x44FFFFFF,
            0x88FFFFFF,
            0xFF666666,
            0xFF444444,
            0x33FFFFFF
    };

    public static final int[] BG_PALETTE = {
            0xE01A1A1E,
            0xE0121216,
            0xE0000000,
            0xC01A1A2E,
            0xE02A1A1E,
            0xD01A2A1E
    };

    public void cycleAccent() {
        accentPaletteIdx = (accentPaletteIdx + 1) % ACCENT_PALETTE.length;
        accentColor = ACCENT_PALETTE[accentPaletteIdx];
    }

    public void cyclePast() {
        pastPaletteIdx = (pastPaletteIdx + 1) % PAST_PALETTE.length;
        pastWordColor = PAST_PALETTE[pastPaletteIdx];
    }

    public void cycleFuture() {
        futurePaletteIdx = (futurePaletteIdx + 1) % FUTURE_PALETTE.length;
        futureWordColor = FUTURE_PALETTE[futurePaletteIdx];
    }

    public void cycleBg() {
        bgPaletteIdx = (bgPaletteIdx + 1) % BG_PALETTE.length;
        barBgColor = BG_PALETTE[bgPaletteIdx];
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                I = GSON.fromJson(Files.readString(FILE), Config.class);
            }
        } catch (Exception e) {
            I = new Config();
        }
        if (I == null) I = new Config();
        // Ensure palette indices are valid after load
        if (I.accentPaletteIdx < 0 || I.accentPaletteIdx >= ACCENT_PALETTE.length) I.accentPaletteIdx = 0;
        if (I.pastPaletteIdx < 0 || I.pastPaletteIdx >= PAST_PALETTE.length) I.pastPaletteIdx = 0;
        if (I.futurePaletteIdx < 0 || I.futurePaletteIdx >= FUTURE_PALETTE.length) I.futurePaletteIdx = 0;
        if (I.bgPaletteIdx < 0 || I.bgPaletteIdx >= BG_PALETTE.length) I.bgPaletteIdx = 0;
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(I));
        } catch (Exception ignored) {}
    }
}
