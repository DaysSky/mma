package com.floweytf.fma.mixin;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.util.ChatUtil;
import java.awt.TextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    private double lastWarned = -1;
    private static final double THRESHOLD_MILLIS = 2000;

    @Inject(method = "setLocalMode(Lnet/minecraft/world/level/GameType;)V", at = @At("HEAD"))
    private void onChangeGameMode(GameType type, CallbackInfo ci) {
        final long timeMillis = System.currentTimeMillis();
        if (FMAClient.config().features.contractCheck && type == GameType.SURVIVAL) {
            final LocalPlayer player = minecraft.player;

            if (player != null && player.experienceLevel >= FMAClient.config().features.contractThreshold &&
                    timeMillis - lastWarned > THRESHOLD_MILLIS) {
                lastWarned = timeMillis;

                minecraft.level.playSound(minecraft.player, player, SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER,
                        2.0f, 0.1f);
                ChatUtil.sendWarn(Component.translatable("text.fma.contract_warning"));
            }
        }
    }
}
