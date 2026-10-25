package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class NowPlayingHud {
    static final String[] CORNERS = {"Top-left", "Top-right", "Bottom-left", "Bottom-right"};
    private static final int W = 196;
    private static final int H = 68;

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        Config c = Config.I;
        if (!c.showOverlay) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.currentScreen != null) return;

        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int corner = Math.floorMod(c.cardCorner, 4);
        int ox = corner % 2 == 0 ? 8 : sw - W - 8;
        int oy = corner < 2 ? 40 : sh - H - 8;

        NowPlaying.Snap s = NowPlaying.snap;

        // Card background
        ctx.fill(ox, oy, ox + W, oy + H, 0xE01A1A1E);
        // Accent square
        ctx.fill(ox + 4, oy + 4, ox + 52, oy + 52, 0xFF2A2A30);
        ctx.drawText(tr, "♪", ox + 23, oy + 24, c.accentColor, false);

        int tx = ox + 60;
        if (s == null) {
            ctx.drawText(tr, "Nothing playing", tx, oy + 8, 0xFFFFFF, false);
            String st = NowPlaying.status;
            if (!st.isEmpty()) {
                ctx.drawText(tr, tr.trimToWidth(st, 130), tx, oy + 22, 0xAAAAAA, false);
            }
            return;
        }

        ctx.drawText(tr, tr.trimToWidth(s.title(), 130), tx, oy + 8, 0xFFFFFF, false);
        ctx.drawText(tr, tr.trimToWidth(s.artist(), 130), tx, oy + 22, 0xAAAAAA, false);

        // Simple progress bar
        if (s.durationMs() > 0) {
            float p = Math.min(1f, (float) NowPlaying.positionMs() / s.durationMs());
            int barW = 120;
            ctx.fill(tx, oy + 48, tx + barW, oy + 52, 0xFF333333);
            ctx.fill(tx, oy + 48, tx + (int) (barW * p), oy + 52, c.accentColor);
        }
    }
}
