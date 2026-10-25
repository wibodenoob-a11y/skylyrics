package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;

public class LyricsHud {

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        Config c = Config.I;
        if (!c.showLyrics) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.currentScreen != null) return;
        if (NowPlaying.snap == null) return;

        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        List<Lyrics.Line> ls = Lyrics.lines;
        if (ls.isEmpty()) {
            String st = Lyrics.status;
            if (!st.isEmpty() && !st.equals("Loading lyrics...")) {
                drawCentered(ctx, tr, st, sw / 2f, sh * 0.5f, 1.0f, 0xFFAAAAAA);
            }
            return;
        }

        long pos = NowPlaying.positionMs() + c.lyricsOffsetMs;

        // Sky / Follow modes
        if (c.lyricsMode != 2) {
            SkyLyricsHud.render(ctx, tr, ls, pos, c);
            return;
        }

        // ===== SCREEN MODE – Floating Karaoke Bar =====
        renderFloatingBar(ctx, tr, ls, pos, c, sw, sh);
    }

    private static void renderFloatingBar(DrawContext ctx, TextRenderer tr,
                                          List<Lyrics.Line> ls, long pos, Config c,
                                          int sw, int sh) {
        int idx = Lyrics.indexAt(ls, pos);
        if (idx < 0) return;

        Lyrics.Line cur = ls.get(idx);
        if (cur.text().isEmpty()) return;

        float size = c.lyricsSize;
        float barY = sh * c.lyricsY;

        int activeWord = Lyrics.wordIndexAt(cur, pos);
        List<Lyrics.Word> allWords = cur.words();

        // Apply max-words window (split / limit words on screen)
        List<Lyrics.Word> words;
        int activeInWindow = activeWord;
        if (allWords != null && !allWords.isEmpty() && c.limitWordsPerLine && c.maxWordsPerLine > 0
                && allWords.size() > c.maxWordsPerLine) {
            int half = c.maxWordsPerLine / 2;
            int start = Math.max(0, activeWord - half);
            int end = Math.min(allWords.size(), start + c.maxWordsPerLine);
            // Adjust start if we hit the end
            if (end - start < c.maxWordsPerLine) {
                start = Math.max(0, end - c.maxWordsPerLine);
            }
            words = allWords.subList(start, end);
            activeInWindow = activeWord - start;
        } else {
            words = allWords;
        }

        // Calculate total width for the bar
        float scale = size * 0.95f;
        int totalWidth = 0;
        if (words != null && !words.isEmpty()) {
            for (Lyrics.Word w : words) {
                totalWidth += tr.getWidth(w.text() + " ");
            }
        } else {
            totalWidth = tr.getWidth(cur.text());
        }
        totalWidth = (int) (totalWidth * scale);

        int padX = 18;
        int padY = 10;
        int barW = totalWidth + padX * 2 + 40; // extra space for progress dots
        int barH = (int) (tr.fontHeight * scale) + padY * 2;

        // Horizontal position based on align / split
        float barX;
        if (c.splitLyrics) {
            barX = sw * 0.55f; // slightly right
        } else {
            barX = switch (c.lyricsAlign) {
                case 1 -> sw * 0.08f + barW / 2f;
                case 2 -> sw * 0.92f - barW / 2f;
                default -> sw / 2f;
            };
        }

        int left = (int) (barX - barW / 2f);
        int top  = (int) (barY - barH / 2f);

        // Background capsule
        int bgColor = applyOpacity(c.barBgColor, c.lyricsOpacity);
        drawRoundedRect(ctx, left, top, left + barW, top + barH, 14, bgColor);

        // Subtle border / glow
        if (c.glowEnabled) {
            int glow = (c.accentColor & 0x00FFFFFF) | 0x22000000;
            drawRoundedRect(ctx, left - 1, top - 1, left + barW + 1, top + barH + 1, 15, glow);
        }

        // Draw words with highlighting
        float textX = left + padX;
        float textY = top + padY;

        if (words != null && !words.isEmpty() && c.wordRevealMode == 0) {
            // Progressive: highlight current word, fade future words a bit
            for (int i = 0; i < words.size(); i++) {
                Lyrics.Word w = words.get(i);
                String part = w.text() + (i < words.size() - 1 ? " " : "");
                int color;
                if (i < activeInWindow) {
                    color = c.pastWordColor;
                } else if (i == activeInWindow) {
                    color = c.accentColor;
                } else {
                    color = c.futureWordColor;
                }
                color = applyOpacity(color, c.lyricsOpacity);

                ctx.getMatrices().push();
                ctx.getMatrices().translate(textX, textY, 0);
                ctx.getMatrices().scale(scale, scale, 1);
                ctx.drawText(tr, part, 0, 0, color, false);
                ctx.getMatrices().pop();
                textX += tr.getWidth(part) * scale;
            }
        } else {
            // Whole line mode (or no word data)
            int color = applyOpacity(c.accentColor, c.lyricsOpacity);

            ctx.getMatrices().push();
            ctx.getMatrices().translate(textX, textY, 0);
            ctx.getMatrices().scale(scale, scale, 1);
            ctx.drawText(tr, cur.text(), 0, 0, color, false);
            ctx.getMatrices().pop();
        }

        // Progress dots on the right
        int dotsX = left + barW - 28;
        int dotsY = top + barH / 2;
        for (int i = 0; i < 3; i++) {
            int alpha = (i == 0) ? 0xFF : (i == 1 ? 0xAA : 0x55);
            int col = (alpha << 24) | (c.accentColor & 0x00FFFFFF);
            ctx.fill(dotsX + i * 6, dotsY - 1, dotsX + i * 6 + 3, dotsY + 2, col);
        }

        // Previous line above
        // When fadePrevious is true  → use previousLineColor (faded)
        // When fadePrevious is false → show previous line at full strength (no fade-out)
        if (c.showContext && idx > 0) {
            Lyrics.Line prev = ls.get(idx - 1);
            if (!prev.text().isEmpty()) {
                float prevY = top - 22 * size;
                int prevColor = c.fadePrevious
                        ? c.previousLineColor
                        : (c.pastWordColor | 0xFF000000); // full strength, no fade
                drawCentered(ctx, tr, prev.text(), barX, prevY, size * 0.7f, prevColor);
            }
        }
    }

    private static int applyOpacity(int argb, float opacity) {
        int a = (int) (((argb >>> 24) & 0xFF) * opacity);
        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static void drawRoundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color) {
        // Simple approximation of rounded rect using fills
        ctx.fill(x1 + r, y1, x2 - r, y2, color);
        ctx.fill(x1, y1 + r, x2, y2 - r, color);
        ctx.fill(x1, y1, x1 + r, y1 + r, color);
        ctx.fill(x2 - r, y1, x2, y1 + r, color);
        ctx.fill(x1, y2 - r, x1 + r, y2, color);
        ctx.fill(x2 - r, y2 - r, x2, y2, color);
    }

    static void drawCentered(DrawContext ctx, TextRenderer tr, String text,
                             float cx, float y, float scale, int argb) {
        int a = (int) (((argb >>> 24) & 0xFF) * Config.I.lyricsOpacity);
        if (a < 8) return;
        int color = (a << 24) | (argb & 0x00FFFFFF);
        int w = tr.getWidth(text);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, y, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(-w / 2f, 0, 0);
        ctx.drawText(tr, text, 0, 0, color, false);
        ctx.getMatrices().pop();
    }

    /** Used by SkyLyricsHud */
    static float fitScaleApprox(TextRenderer tr, String text, float scale, int sw) {
        int w = Math.max(1, tr.getWidth(text));
        float maxW = sw * 0.8f;
        return Math.min(scale, maxW / w);
    }
}
