package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.NowPlaying;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Sky / Follow mode – floating bubbles in 3D space */
public class SkyLyricsHud {
    private static final long FADE_OUT_MS = 2500L;
    private static final double[] TAU = {0.05, 0.12, 0.25};
    private static final List<Bubble> bubbles = new ArrayList<>();
    private static int lastIdx = -2;
    private static String lastKey = "";
    private static double anchorYaw, anchorPitch;
    private static long lastFrameNs;

    static void render(DrawContext ctx, TextRenderer tr, List<Lyrics.Line> ls, long pos, Config c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean follow = c.lyricsMode == 0;
        NowPlaying.Snap snap = NowPlaying.snap;
        String key = snap == null ? "" : snap.title() + "|" + snap.artist();
        if (!key.equals(lastKey)) {
            bubbles.clear();
            lastKey = key;
            lastIdx = -2;
        }

        Camera cam = mc.gameRenderer.getCamera();
        float camYaw = cam.getYaw();
        float camPitch = cam.getPitch();
        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);

        long now = System.currentTimeMillis();
        long ns = System.nanoTime();

        if (follow) {
            double dt = (ns - lastFrameNs) / 1e9;
            lastFrameNs = ns;
            if (dt > 0.5 || dt <= 0) {
                anchorYaw = camYaw;
                anchorPitch = camPitch;
            } else {
                double a = 1.0 - Math.exp(-dt / TAU[Math.floorMod(c.followSmooth, 3)]);
                anchorYaw += wrap(camYaw - anchorYaw) * a;
                anchorPitch += (camPitch - anchorPitch) * a;
            }
        } else {
            lastFrameNs = 0;
        }

        int idx = Lyrics.indexAt(ls, pos);
        if (idx != lastIdx) {
            lastIdx = idx;
            for (Bubble b : bubbles) {
                if (b.end == 0) b.end = now;
            }
            if (idx >= 0 && !ls.get(idx).text().isEmpty()) {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                Bubble b = new Bubble();
                b.text = ls.get(idx).text();
                b.start = now;
                double offDeg = Math.toDegrees(Math.atan(0.68 * tanHalf));
                b.yaw = camYaw + (float) r.nextDouble(-14, 14);
                b.pitch = (float) Math.max(-85, Math.min(-15, camPitch - offDeg + r.nextDouble(-2, 2)));
                bubbles.add(b);
            }
        }
        bubbles.removeIf(b -> b.end != 0 && now - b.end > FADE_OUT_MS);

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        double yr = Math.toRadians(camYaw);
        double pr = Math.toRadians(camPitch);
        double fx = -Math.sin(yr) * Math.cos(pr);
        double fy = -Math.sin(pr);
        double fz = Math.cos(yr) * Math.cos(pr);
        double rx = -Math.cos(yr);
        double rz = -Math.sin(yr);
        double ux = -rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy;
        double aspect = (double) sw / sh;
        float scale = c.lyricsSize * (follow ? 1.2f : 1.3f);
        double followNy = 0.62;
        double followOffDeg = Math.toDegrees(Math.atan(followNy * tanHalf));

        for (Bubble b : bubbles) {
            float t = b.end == 0 ? 0 : (float) (now - b.end) / FADE_OUT_MS;
            float te = smooth(0, 1, t);
            double yawDeg = follow ? anchorYaw : b.yaw;
            double pitchDeg = follow ? anchorPitch - followOffDeg : b.pitch - 6 * te;

            double by = Math.toRadians(yawDeg);
            double bp = Math.toRadians(pitchDeg);
            double dx = -Math.sin(by) * Math.cos(bp);
            double dy = -Math.sin(bp);
            double dz = Math.cos(by) * Math.cos(bp);

            double depth = dx * fx + dy * fy + dz * fz;
            if (depth < 0.05) continue;

            double nx = (dx * rx + dz * rz) / depth / (tanHalf * aspect);
            double ny = (dx * ux + dy * uy + dz * uz) / depth / tanHalf;
            if (Math.abs(nx) > 1.4 || Math.abs(ny) > 1.3) continue;

            float fit = LyricsHud.fitScaleApprox(tr, b.text, scale, sw);
            float vis = 1f;
            if (!follow && c.keepCenterClear) {
                double halfW = tr.getWidth(b.text) * fit / (float) sw;
                double ex = Math.max(0, Math.abs(nx) - halfW);
                double r = Math.hypot(ex / 0.2, Math.abs(ny) / 0.45);
                vis = smooth(0.9f, 1.35f, (float) r);
            }

            float fadeIn = smooth(0, 1, (float) (now - b.start) / 450f);
            float a = fadeIn * (1 - te) * vis * (b.end == 0 ? 1f : 0.8f);
            int alpha = (int) (255 * a);
            if (alpha < 8) continue;

            int color = (alpha << 24) | (b.end == 0 ? c.accentColor & 0xFFFFFF : 0xFFFFFF);
            float sx = (float) (sw / 2.0 * (1 + nx));
            float sy = (float) (sh / 2.0 * (1 - ny));
            if (follow) sy -= 20 * scale * te;

            LyricsHud.drawCentered(ctx, tr, b.text, sx, sy, scale, color);
        }
    }

    private static float smooth(float e0, float e1, float x) {
        float t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    private static double wrap(double d) {
        d = ((d + 540) % 360 + 360) % 360 - 180;
        return d;
    }

    private static class Bubble {
        String text;
        long start;
        long end = 0;
        float yaw, pitch;
    }
}
