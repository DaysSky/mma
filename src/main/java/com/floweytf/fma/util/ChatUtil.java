package com.floweytf.fma.util;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig.Appearance;

import java.util.Objects;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatUtil {
    public static void send(Component... message) {
        Appearance config = FMAClient.appearance();
        FMAClient.player()
                .sendSystemMessage(
                        FormatUtil.join(
                                FormatUtil.withColor("[", config.bracketColor),
                                FormatUtil.withColor(config.tagText, config.tagColor).withStyle(ChatFormatting.BOLD),
                                FormatUtil.withColor("] ", config.bracketColor),
                                FormatUtil.colored(config.textColor).append(FormatUtil.join(message))
                        )
                );
    }

    public static void send(String message) {
        send(FormatUtil.literal(message));
    }

    public static void sendWarn(Component message) {
        send(Component.empty().append(FormatUtil.withColor("WARN", FMAClient.appearance().warningColor)).append(" ").append(message));
    }

    public static void sendWarn(String message) {
        sendWarn(FormatUtil.literal(message));
    }

    public static void sendDebug(String message) {
        if (!FMAClient.features().suppressDebugWarning) {
            sendWarn("(debug/possible bug) " + message);
        } else {
            FMAClient.LOGGER.warn(message);
        }
    }

    public static void sendCommand(String command) {
        if (command.startsWith("/")) {
            FMAClient.LOGGER.warn("leading /");
        }

        FMAClient.LOGGER.debug("running command as client: {}", command);
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).sendCommand(command);
    }
}
