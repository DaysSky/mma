package com.dayssky.mma.features;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.MMAConfig;
import com.dayssky.mma.util.ChatUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class ContractCheck {
    private static double lastWarned = -1;
    private static final double THRESHOLD_MILLIS = 2000;
    private static double playerX;
    private static double playerY;
    private static double playerZ;
    private static final Minecraft mc = Minecraft.getInstance();
    private static boolean isInZenithArea = false;

    public static void onChangeGameMode() {
        final long timeMillis = System.currentTimeMillis();

        if (mc.player != null && mc.player.experienceLevel >= MMAClient.config().features.contractThreshold &&
                timeMillis - lastWarned > THRESHOLD_MILLIS) {
                lastWarned = timeMillis;
                mc.level.playSound(mc.player, mc.player, SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 2.0f, 0.1f);
                ChatUtil.sendWarn(Component.literal(new MMAConfig().features.contractCheckText));
        }
    }

    public static void tick() {
        if (!(mc.player != null && mc.player.experienceLevel <= MMAClient.config().features.czContractThreshold)) return;
        updateXYZ(mc.player);
        if (!isInZenithArea && inZenithArea()) {
                mc.level.playSound(mc.player, mc.player, SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 2.0f, 0.1f);
                ChatUtil.sendWarn(Component.literal(new MMAConfig().features.contractCheckText));
        }
        isInZenithArea = inZenithArea();
    }

    private static void updateXYZ(Player player) {
        playerX = player.getX();
        playerY = player.getY();
        playerZ = player.getZ();
    }

    private static boolean inZenithArea() {
        return playerX >= 33 && playerX <= 71 && playerY >= 14 && playerY <= 30 && playerZ >= -1410 && playerZ <= -1396;
    }

}
