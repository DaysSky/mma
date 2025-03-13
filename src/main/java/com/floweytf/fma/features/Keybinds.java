package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.util.ChatUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    private static final KeyMapping keyBindingMeow = new KeyMapping(
        "key.fma.meow",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        "category.fma"
    );
    private static final KeyMapping keyBindingPS = new KeyMapping(
        "key.fma.playerstats",
        InputConstants.Type.MOUSE,
        GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
        "category.fma"
    );
    private static long meowMsNext = System.currentTimeMillis();

    public static void init() {
        KeyBindingHelper.registerKeyBinding(keyBindingMeow);

        KeyBindingHelper.registerKeyBinding(keyBindingPS);
    }

    public static void tick() {
        if (keyBindingMeow.isDown()) {
            onPressedMeow();
        }
        if (keyBindingPS.isDown()) {
            onPressedPS();
        }
    }

    public static void onPressedMeow() {
        final var current = System.currentTimeMillis();

        if (meowMsNext > current) {
            return;
        }

        meowMsNext = current + 3000;

        ChatUtil.sendCommand(String.format(
            "chat say %s %s",
            FMAClient.config().chat.meowingChannel,
            FMAClient.config().chat.meowingText
        ));
    }

    public static void onPressedPS() {
        final var entity = Minecraft.getInstance().crosshairPickEntity;
        if (entity instanceof Player player) {
            ChatUtil.sendCommand("ps " + player.getScoreboardName());
        }
    }
}
