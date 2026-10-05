package com.dayssky.mma.features;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.util.ChatUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class Reminders {
    private static double lastWarned = -1;
    private static final double THRESHOLD_MILLIS = 2000;
    private static double playerX;
    private static double playerY;
    private static double playerZ;
    private static final Minecraft mc = Minecraft.getInstance();
    private static boolean isInZenithArea = false;

    public static void onChangeGameMode() {
        final long timeMillis = System.currentTimeMillis();

        if (mc.player != null && timeMillis - lastWarned > THRESHOLD_MILLIS) {
            lastWarned = timeMillis;

            boolean shouldPlaySound = false;
            boolean originalTriggered = false;
            boolean customTriggered = false;

            // Original contract warning (only if enabled in config)
            if (MMAClient.config().reminders.contractCheck &&
                    mc.player.experienceLevel >= MMAClient.config().reminders.contractThreshold) {
                originalTriggered = true;
                shouldPlaySound = true;
            }

            // Custom reminders (if enabled)
            if (MMAClient.config().reminders.enableCustomReminders) {
                for (String msg : MMAClient.config().reminders.customReminderMessages) {
                    if (msg != null && !msg.trim().isEmpty()) {
                        customTriggered = true;
                        shouldPlaySound = true;
                        break;
                    }
                }
            }

            // Play sound once if any reminder will be shown
            if (shouldPlaySound) {
                playReminderSound();
            }

            // Send original contract warning
            if (originalTriggered) {
                ChatUtil.sendWarn(Component.literal(MMAClient.config().reminders.contractCheckText));
            }

            // Send all custom reminders (if any)
            if (customTriggered) {
                for (String msg : MMAClient.config().reminders.customReminderMessages) {
                    if (msg != null && !msg.trim().isEmpty()) {
                        ChatUtil.send(Component.literal(msg));
                    }
                }
            }
        }
    }

    public static void tick() {
        if (!(mc.player != null && mc.player.experienceLevel <= MMAClient.config().reminders.czContractThreshold)) return;
        updateXYZ(mc.player);
        if (!isInZenithArea && inZenithArea()) {
            playReminderSound();
            ChatUtil.sendWarn(Component.literal(MMAClient.config().reminders.contractCheckText));
        }
        isInZenithArea = inZenithArea();
    }

    private static void playReminderSound() {
        if (mc.player == null || mc.level == null) return;

        String soundConfig = MMAClient.config().reminders.reminderSound;
        String[] parts = soundConfig.split(" ");
        String soundId = parts.length >= 1 ? parts[0] : "minecraft:entity.player.levelup";
        float volume = 1.0f;
        float pitch = 1.0f;

        try {
            if (parts.length >= 2) volume = Float.parseFloat(parts[1]);
            if (parts.length >= 3) pitch = Float.parseFloat(parts[2]);
        } catch (NumberFormatException e) {
            MMAClient.LOGGER.warn("Invalid volume/pitch in reminderSound config: '{}'", soundConfig);
        }

        ResourceLocation id = ResourceLocation.tryParse(soundId);
        if (id == null) {
            MMAClient.LOGGER.warn("Invalid sound ID in reminderSound config: '{}'", soundId);
            return;
        }

        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(id);
        if (soundEvent == null) {
            MMAClient.LOGGER.warn("Unknown sound event: {}", id);
            return;
        }

        mc.level.playSound(mc.player, mc.player, soundEvent, SoundSource.MASTER, volume, pitch);
    }

    private static void updateXYZ(Player player) {
        playerX = player.getX();
        playerY = player.getY();
        playerZ = player.getZ();
    }

    private static boolean inZenithArea() {
        if (mc.player == null) return false;
        ResourceLocation dimension = mc.player.level().dimension().location();
        if (!dimension.equals(new ResourceLocation("monumenta", "ring"))) {
            return false;
        }
        return playerX >= 33 && playerX <= 71 && playerY >= 14 && playerY <= 30 && playerZ >= -1410 && playerZ <= -1396;
    }
}