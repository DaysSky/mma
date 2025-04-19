package com.floweytf.fma.util;

import com.floweytf.fma.FMAClient;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static com.floweytf.fma.util.FormatUtil.colored;
import static com.floweytf.fma.util.FormatUtil.join;
import static com.floweytf.fma.util.FormatUtil.literal;
import static com.floweytf.fma.util.FormatUtil.withColor;
import static net.minecraft.network.chat.Component.empty;

public class ChatUtil {
    public static void send(Component... message) {
        final var config = FMAClient.appearance();
        FMAClient.player().sendSystemMessage(join(
            withColor("[", config.bracketColor),
            withColor(config.tagText, config.tagColor).withStyle(ChatFormatting.BOLD),
            withColor("] ", config.bracketColor),
            colored(config.textColor).append(join(message))
        ));
    }

    public static void send(String message) {
        send(literal(message));
    }

    public static void sendWarn(Component message) {
        send(empty()
            .append(withColor("WARN", FMAClient.appearance().warningColor))
            .append(" ")
            .append(message)
        );
    }

    public static void sendWarn(String message) {
        sendWarn(literal(message));
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
