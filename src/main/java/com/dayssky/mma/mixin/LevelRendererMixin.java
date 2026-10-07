package com.dayssky.mma.mixin;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.features.HpIndicator;
import com.dayssky.mma.features.gamestate.HexfallStateTracker;
import com.dayssky.mma.util.SafeExceptionLogger;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.Objects;

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
    private static final SafeExceptionLogger mma$EH = new SafeExceptionLogger("PlayerGlowing");

    @ModifyExpressionValue(
            method = {"renderLevel"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"
            )}
    )
    private boolean modifyPlayerGlowingStatus(boolean original, @Local Entity entity) {
        return mma$EH.<Boolean>runSafely(() -> {
            if (!MMAClient.config().features.enableHpIndicators) {
                return original;
            } else if (!MMAClient.config().hpIndicator.enableGlowingPlayer) {
                return original;
            } else if (!(entity instanceof Player player) || player.getScoreboardName().startsWith("|npc_")) { // fake players
                return original;
            } else {
                return !MMAClient.config().hpIndicator.disableSelf || player != MMAClient.player();
            }
        }).orElse(original);
    }

    @WrapOperation(
            method = {"renderLevel"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"
            )}
    )
    private int modifyPlayerGlowingColor(Entity entity, Operation<Integer> original) {
        int fallback = original.call(entity);

        return mma$EH.<Integer>runSafely(() -> {

            // HF totem glow — self only, bypasses HP-indicator toggles.
            if (entity == Minecraft.getInstance().player) {
                HexfallStateTracker hexfall = MMAClient.GAME_STATE.hexfall();
                if (hexfall != null && hexfall.totemElement != null) {
                    return Objects.equals(hexfall.totemElement, "Death") ? 9915173 : 5569364;
                }
            }

            if (!MMAClient.config().features.enableHpIndicators
                    || !MMAClient.config().hpIndicator.enableGlowingPlayer) {
                return fallback;
            }

            if (MMAClient.config().hpIndicator.disableInHycenea) {
                for (LerpingBossEvent boss : Minecraft.getInstance().gui.getBossOverlay().events.values()) {
                    if (boss.getName().getString().contains("Hycenea")) {
                        return fallback;
                    }
                }
            }

            if (entity instanceof Player player && !player.getScoreboardName().startsWith("|npc_")) {
                return HpIndicator.computeEntityHealthColor(player);
            }

            return fallback;
        }).orElse(fallback);
    }
}
