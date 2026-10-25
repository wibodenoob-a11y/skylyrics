package com.example.skylyrics.client;

import com.example.skylyrics.Config;
import com.example.skylyrics.Lyrics;
import com.example.skylyrics.MediaSession;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SkyLyricsClient implements ClientModInitializer {
    public static final String MOD_ID = "skylyrics";
    private static KeyBinding openMenu;

    @Override
    public void onInitializeClient() {
        Config.load();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skylyrics.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "key.category.skylyrics.main"
        ));

        MediaSession.start();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) {
                client.setScreen(new SettingsScreen());
            }
            Lyrics.tick();
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            NowPlayingHud.render(ctx, tickCounter);
            LyricsHud.render(ctx, tickCounter);
        });
    }
}
