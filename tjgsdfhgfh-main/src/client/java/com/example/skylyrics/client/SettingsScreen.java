package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SettingsScreen extends Screen {

    private static Tab current = Tab.LYRICS;

    private static final String[] MODES     = {"Follow", "Sky", "Screen (Bar)"};
    private static final String[] SMOOTH    = {"Snappy", "Smooth", "Floaty"};
    private static final String[] ALIGN     = {"Center", "Left", "Right"};
    private static final String[] WORD_MODE = {"Progressive (1 by 1)", "Whole line (wait)"};

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 340;
    private static final int SIDEBAR = 108;

    public SettingsScreen() {
        super(Text.literal("SkyLyrics"));
    }

    @Override
    protected void init() {
        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;

        // Sidebar tabs
        int ty = top + 48;
        for (Tab t : Tab.values()) {
            boolean selected = current == t;
            String label = (selected ? "§f▶ " : "§7  ") + t.label;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                current = t;
                clearAndInit();
            }).dimensions(left + 8, ty, SIDEBAR - 16, 24).build());
            ty += 28;
        }

        int cx = left + SIDEBAR + 14;
        int cy = top + 42;
        int cw = PANEL_W - SIDEBAR - 28;
        Config c = Config.I;

        switch (current) {
            case LYRICS    -> buildLyricsTab(cx, cy, cw, c);
            case DISPLAY   -> buildDisplayTab(cx, cy, cw, c);
            case COSMETICS -> buildCosmeticsTab(cx, cy, cw, c);
            case PLAYER    -> buildPlayerTab(cx, cy, cw, c);
        }
    }

    // ─── LYRICS TAB ───────────────────────────────────────────────
    private void buildLyricsTab(int cx, int cy, int cw, Config c) {
        addToggle(cx, cy, cw, "Show lyrics", () -> c.showLyrics, v -> c.showLyrics = v);
        cy += 24;

        addButton(cx, cy, cw, "Mode: " + MODES[Math.floorMod(c.lyricsMode, 3)], b -> {
            c.lyricsMode = (c.lyricsMode + 1) % 3;
            clearAndInit();
        });
        cy += 24;

        if (c.lyricsMode == 0) {
            addButton(cx, cy, cw, "Follow smoothness: " + SMOOTH[Math.floorMod(c.followSmooth, 3)], b -> {
                c.followSmooth = (c.followSmooth + 1) % 3;
                clearAndInit();
            });
        } else if (c.lyricsMode == 1) {
            addToggle(cx, cy, cw, "Keep center clear", () -> c.keepCenterClear, v -> c.keepCenterClear = v);
        } else {
            addToggle(cx, cy, cw, "Show previous / next", () -> c.showContext, v -> c.showContext = v);
        }
        cy += 26;

        addButton(cx, cy, cw, "Word style: " + WORD_MODE[Math.floorMod(c.wordRevealMode, 2)], b -> {
            c.wordRevealMode = (c.wordRevealMode + 1) % 2;
            clearAndInit();
        });
        cy += 24;

        addToggle(cx, cy, cw, "Limit words on bar", () -> c.limitWordsPerLine, v -> c.limitWordsPerLine = v);
        cy += 24;

        if (c.limitWordsPerLine) {
            addButton(cx, cy, cw, "Max words per line: " + c.maxWordsPerLine, b -> {
                c.maxWordsPerLine += 1;
                if (c.maxWordsPerLine > 12) c.maxWordsPerLine = 3;
                clearAndInit();
            });
            cy += 24;
        }

        addToggle(cx, cy, cw, "Split Left / Right", () -> c.splitLyrics, v -> c.splitLyrics = v);
        cy += 24;

        addButton(cx, cy, cw, "Line position: " + ALIGN[Math.floorMod(c.lyricsAlign, 3)], b -> {
            c.lyricsAlign = (c.lyricsAlign + 1) % 3;
            clearAndInit();
        });
        cy += 24;

        addButton(cx, cy, cw, String.format("Vertical height: %.0f%%", c.lyricsY * 100), b -> {
            c.lyricsY += 0.04f;
            if (c.lyricsY > 0.88f) c.lyricsY = 0.15f;
            clearAndInit();
        });
        cy += 24;

        addToggle(cx, cy, cw, "Previous lines fade out", () -> c.fadePrevious, v -> c.fadePrevious = v);
        cy += 26;

        // Size + Opacity side by side
        int half = cw / 2 - 3;
        addButton(cx, cy, half, String.format("Size: %.2fx", c.lyricsSize), b -> {
            c.lyricsSize = c.lyricsSize >= 2.8f ? 0.8f : c.lyricsSize + 0.2f;
            clearAndInit();
        });
        addButton(cx + half + 6, cy, half, "Opacity: " + Math.round(c.lyricsOpacity * 100) + "%", b -> {
            c.lyricsOpacity = c.lyricsOpacity >= 0.99f ? 0.25f : Math.min(1f, c.lyricsOpacity + 0.1f);
            clearAndInit();
        });
        cy += 26;

        // Timing
        addButton(cx, cy, half, "Earlier −0.25s", b -> {
            c.lyricsOffsetMs -= 250;
            clearAndInit();
        });
        addButton(cx + half + 6, cy, half, "Later +0.25s", b -> {
            c.lyricsOffsetMs += 250;
            clearAndInit();
        });
        cy += 24;
        addButton(cx, cy, cw,
                String.format("Offset: %.2fs  (click to reset)", c.lyricsOffsetMs / 1000.0),
                b -> { c.lyricsOffsetMs = 0; clearAndInit(); });
    }

    // ─── DISPLAY TAB ──────────────────────────────────────────────
    private void buildDisplayTab(int cx, int cy, int cw, Config c) {
        addToggle(cx, cy, cw, "Now-playing card", () -> c.showOverlay, v -> c.showOverlay = v);
        cy += 24;
        addToggle(cx, cy, cw, "Decorative bars", () -> c.showSpectrum, v -> c.showSpectrum = v);
        cy += 24;
        addButton(cx, cy, cw, "Card position: " + NowPlayingHud.CORNERS[Math.floorMod(c.cardCorner, 4)], b -> {
            c.cardCorner = (c.cardCorner + 1) % 4;
            clearAndInit();
        });
        cy += 26;
        addToggle(cx, cy, cw, "Song title intro", () -> c.showSongTitleIntro, v -> c.showSongTitleIntro = v);
        cy += 24;
        addToggle(cx, cy, cw, "Glow on lyric bar", () -> c.glowEnabled, v -> c.glowEnabled = v);
        cy += 28;

        // Quick info box
        addButton(cx, cy, cw, "§7Tip: Use Cosmetics tab for colors & themes", b -> {
            current = Tab.COSMETICS;
            clearAndInit();
        });
    }

    // ─── COSMETICS TAB (main request) ─────────────────────────────
    private void buildCosmeticsTab(int cx, int cy, int cw, Config c) {
        // Color cyclers with live swatches drawn in render()
        addButton(cx, cy, cw, "Accent (current word)  →  cycle", b -> {
            c.cycleAccent();
            clearAndInit();
        });
        cy += 26;

        addButton(cx, cy, cw, "Past words color  →  cycle", b -> {
            c.cyclePast();
            clearAndInit();
        });
        cy += 26;

        addButton(cx, cy, cw, "Future words color  →  cycle", b -> {
            c.cycleFuture();
            clearAndInit();
        });
        cy += 26;

        addButton(cx, cy, cw, "Bar background  →  cycle", b -> {
            c.cycleBg();
            clearAndInit();
        });
        cy += 30;

        // Theme presets
        int half = cw / 2 - 3;
        addButton(cx, cy, half, "§dPurple Karaoke", b -> {
            applyTheme(c, 0xFF9B5DE5, 0xFFCCCCCC, 0x66FFFFFF, 0xE01A1A1E, 0);
            clearAndInit();
        });
        addButton(cx + half + 6, cy, half, "§bCyan Neon", b -> {
            applyTheme(c, 0xFF00F5D4, 0xFFAAAAAA, 0x44FFFFFF, 0xE0121216, 1);
            clearAndInit();
        });
        cy += 24;

        addButton(cx, cy, half, "§cCoral Warm", b -> {
            applyTheme(c, 0xFFFF6B6B, 0xFFCCCCCC, 0x66FFFFFF, 0xE02A1A1E, 2);
            clearAndInit();
        });
        addButton(cx + half + 6, cy, half, "§eGold Classic", b -> {
            applyTheme(c, 0xFFFFD166, 0xFFAAAAAA, 0x66FFFFFF, 0xE01A1A1E, 3);
            clearAndInit();
        });
        cy += 24;

        addButton(cx, cy, half, "§aMint Fresh", b -> {
            applyTheme(c, 0xFF06D6A0, 0xFFCCCCCC, 0x55FFFFFF, 0xE01A2A1E, 4);
            clearAndInit();
        });
        addButton(cx + half + 6, cy, half, "§9Ocean Blue", b -> {
            applyTheme(c, 0xFF118AB2, 0xFFBBBBBB, 0x55FFFFFF, 0xE0121620, 5);
            clearAndInit();
        });
        cy += 28;

        addToggle(cx, cy, cw, "Glow effect", () -> c.glowEnabled, v -> c.glowEnabled = v);
        cy += 24;
        addToggle(cx, cy, cw, "Previous lines fade out", () -> c.fadePrevious, v -> c.fadePrevious = v);
    }

    private void applyTheme(Config c, int accent, int past, int future, int bg, int idx) {
        c.accentColor = accent;
        c.pastWordColor = past;
        c.futureWordColor = future;
        c.barBgColor = bg;
        c.previousLineColor = 0x55FFFFFF;
        c.accentPaletteIdx = idx;
    }

    // ─── PLAYER TAB ───────────────────────────────────────────────
    private void buildPlayerTab(int cx, int cy, int cw, Config c) {
        // Content is drawn in render(); only the action button here
        addDrawableChild(ButtonWidget.builder(Text.literal("Reload lyrics"), b -> Lyrics.reload())
                .dimensions(cx, cy + 90, cw, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Force refresh media"), b -> {
            NowPlaying.clear("Refreshing...");
            Lyrics.reload();
        }).dimensions(cx, cy + 118, cw, 22).build());
    }

    // ─── Helpers ──────────────────────────────────────────────────
    private void addToggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(
                Text.literal(label + ": " + (get.getAsBoolean() ? "§aON" : "§cOFF")),
                b -> {
                    set.accept(!get.getAsBoolean());
                    clearAndInit();
                }).dimensions(x, y, w, 20).build());
    }

    private void addButton(int x, int y, int w, String label, ButtonWidget.PressAction action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), action)
                .dimensions(x, y, w, 20).build());
    }

    // ─── RENDER ───────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;
        Config c = Config.I;

        // Main panel background
        ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF0101014);
        // Sidebar
        ctx.fill(left, top, left + SIDEBAR, top + PANEL_H, 0xFF0A0A0E);
        // Accent line under title
        ctx.fill(left, top + 36, left + PANEL_W, top + 37, c.accentColor & 0x55FFFFFF);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 12, 0xFFFFFF);
        ctx.drawText(textRenderer, "§8v0.7.0", left + PANEL_W - 42, top + 14, 0x555555, false);

        // Section header
        String section = switch (current) {
            case LYRICS    -> "LYRICS & KARAOKE";
            case DISPLAY   -> "DISPLAY & OVERLAY";
            case COSMETICS -> "COSMETICS & THEMES";
            case PLAYER    -> "NOW PLAYING";
        };
        ctx.drawText(textRenderer, "§7" + section, left + SIDEBAR + 14, top + 24, 0xAAAAAA, false);

        // ── Cosmetics: large color swatches + live preview bar ──
        if (current == Tab.COSMETICS) {
            int sx = left + SIDEBAR + 14;
            int sy = top + 268;

            // Color swatches row
            drawSwatch(ctx, sx, sy, 28, 16, c.accentColor, "Accent");
            drawSwatch(ctx, sx + 70, sy, 28, 16, c.pastWordColor | 0xFF000000, "Past");
            drawSwatch(ctx, sx + 140, sy, 28, 16, c.futureWordColor | 0xFF000000, "Future");
            drawSwatch(ctx, sx + 210, sy, 28, 16, c.barBgColor | 0xFF000000, "Bar BG");

            // Mini karaoke bar preview
            int barX = left + SIDEBAR + 20;
            int barY = top + 300;
            int barW = PANEL_W - SIDEBAR - 40;
            int barH = 22;

            // background
            ctx.fill(barX, barY, barX + barW, barY + barH, c.barBgColor);
            // glow edge
            if (c.glowEnabled) {
                int glow = (c.accentColor & 0x00FFFFFF) | 0x33000000;
                ctx.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, glow);
            }

            // sample words
            String[] sample = {"This", "is", "how", "your", "lyrics", "look"};
            int[] colors = {
                    c.pastWordColor, c.pastWordColor, c.accentColor,
                    c.futureWordColor, c.futureWordColor, c.futureWordColor
            };
            float tx = barX + 10;
            for (int i = 0; i < sample.length; i++) {
                String w = sample[i] + (i < sample.length - 1 ? " " : "");
                ctx.drawText(textRenderer, w, (int) tx, barY + 7, colors[i] | 0xFF000000, false);
                tx += textRenderer.getWidth(w);
            }
        }

        // ── Player tab content ──
        if (current == Tab.PLAYER) {
            int x = left + SIDEBAR + 14;
            int y = top + 50;
            NowPlaying.Snap s = NowPlaying.snap;
            if (s == null) {
                ctx.drawText(textRenderer, "§7Nothing playing", x, y, 0xFFFFFF, false);
                ctx.drawText(textRenderer, textRenderer.trimToWidth(NowPlaying.status, 260), x, y + 14, 0xAAAAAA, false);
            } else {
                ctx.drawText(textRenderer, textRenderer.trimToWidth(s.title(), 260), x, y, 0xFFFFFF, false);
                ctx.drawText(textRenderer, textRenderer.trimToWidth(s.artist(), 260), x, y + 14, 0xAAAAAA, false);

                // simple progress
                if (s.durationMs() > 0) {
                    float p = Math.min(1f, (float) NowPlaying.positionMs() / s.durationMs());
                    int barW = 200;
                    ctx.fill(x, y + 36, x + barW, y + 40, 0xFF333333);
                    ctx.fill(x, y + 36, x + (int) (barW * p), y + 40, c.accentColor);
                }

                String ls = Lyrics.lines.isEmpty()
                        ? Lyrics.status
                        : Lyrics.lines.size() + " lyric lines loaded";
                ctx.drawText(textRenderer, textRenderer.trimToWidth(ls, 260), x, y + 50, c.accentColor, false);
            }
        }
    }

    private void drawSwatch(DrawContext ctx, int x, int y, int w, int h, int color, String label) {
        // border
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF333333);
        ctx.fill(x, y, x + w, y + h, color);
        ctx.drawText(textRenderer, "§8" + label, x + w + 4, y + 4, 0x888888, false);
    }

    @Override
    public void close() {
        Config.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum Tab {
        LYRICS("Lyrics"),
        DISPLAY("Display"),
        COSMETICS("Cosmetics"),
        PLAYER("Player");

        final String label;
        Tab(String l) { this.label = l; }
    }
}
