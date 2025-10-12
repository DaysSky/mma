package com.floweytf.fma.mixin;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.features.HpIndicator;
import com.floweytf.fma.util.SafeExceptionLogger;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
    @Unique
    private static final SafeExceptionLogger fma$EH = new SafeExceptionLogger("PlayerGlowing");

    @ModifyExpressionValue(
            method = {"renderLevel"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"
            )}
    )
    private boolean modifyPlayerGlowingStatus(boolean original, @Local Entity entity) {
        return fma$EH.<Boolean>runSafely(() -> {
            if (!FMAClient.features().enableHpIndicators) {
                return original;
            } else if (!FMAClient.config().hpIndicator.enableGlowingPlayer) {
                return original;
            } else {
                return !(entity instanceof Player) ? original : !FMAClient.config().hpIndicator.disableSelf || entity != FMAClient.player();
            }
        }).orElse(original);
    }

    @ModifyExpressionValue(
            method = {"renderLevel"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"
            )}
    )
    private int modifyPlayerGlowingColor(int original, @Local Entity entity) {
        return fma$EH.<Integer>runSafely(() -> {
            if (!FMAClient.features().enableHpIndicators) {
                return original;
            } else if (!FMAClient.config().hpIndicator.enableGlowingPlayer) {
                return original;
            } else {
                if (FMAClient.config().hpIndicator.disableInHycenea) {
                    Map<UUID, LerpingBossEvent> events = Minecraft.getInstance().gui.getBossOverlay().events;

                    for (LerpingBossEvent value : events.values()) {
                        if (value.getName().getString().contains("Hycenea")) {
                            return original;
                        }
                    }
                }

                return entity instanceof Player player ? HpIndicator.computeEntityHealthColor(player) : original;
            }
        }).orElse(original);
    }
}
