package com.floweytf.fma.mixin.umm;

import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.features.effects.Effect;
import ch.njol.unofficialmonumentamod.features.effects.EffectOverlay;
import com.floweytf.fma.FMAClient;
import com.floweytf.fma.Graphics;
import com.floweytf.fma.util.FormatUtil;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        value = {EffectOverlay.class},
        remap = false
)
public abstract class EffectOverlayMixin extends HudElement {
    @Unique
    private static final Set<MobEffect> fma$BAD_EFFECTS = Set.of(
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SLOWDOWN,
            MobEffects.CONFUSION,
            MobEffects.BLINDNESS,
            MobEffects.HUNGER,
            MobEffects.WEAKNESS,
            MobEffects.POISON,
            MobEffects.WITHER,
            MobEffects.UNLUCK,
            MobEffects.BAD_OMEN
    );
    @Unique
    private List<Component> fma$effectCache = null;
    @Unique
    private boolean fma$prevRAlign = false;
    @Shadow
    @Final
    private ArrayList<Effect> effects;

    @Unique
    private void fma$renderLine(Font font, GuiGraphics matrix, Component text, boolean rAlign, int width, LocalIntRef currentY) {
        Graphics.drawString(matrix, font, text, rAlign ? width - 5 - font.width(text) : 5, currentY.get(), -1);
        currentY.set(currentY.get() + 11);
    }

    @Unique
    private List<Component> fma$getTextToRender(boolean rAlign) {
        if (rAlign != this.fma$prevRAlign) {
            this.fma$effectCache = null;
            this.fma$prevRAlign = rAlign;
        }

        if (this.fma$effectCache == null) {
            this.fma$effectCache = new ArrayList<>();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                MutableComponent space = FormatUtil.literal(" ");

                for (MobEffectInstance activeEffect : player.getActiveEffects()) {
                    Component nameText = activeEffect.getEffect().getDisplayName();
                    int amp = activeEffect.getAmplifier();
                    MutableComponent levelText = FormatUtil.literal(amp == 0 ? "" : amp + 1 + " ");
                    Component timeText = MobEffectUtil.formatDuration(activeEffect, 1.0F, 20.0F);
                    ChatFormatting color = fma$BAD_EFFECTS.contains(activeEffect.getEffect()) ? ChatFormatting.RED : ChatFormatting.GREEN;
                    MutableComponent effectText = FormatUtil.join(nameText, space, levelText).withStyle(color);
                    MutableComponent text = rAlign ? FormatUtil.join(effectText, timeText) : FormatUtil.join(timeText, space, effectText);
                    this.fma$effectCache.add(text);
                }
            }
        }

        return this.fma$effectCache;
    }

    @Inject(
            method = {"tick"},
            at = {@At("HEAD")}
    )
    private void updateCache(CallbackInfo ci) {
        this.fma$effectCache = null;
    }

    @Inject(
            method = {"render"},
            at = {@At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;iterator()Ljava/util/Iterator;"
            )}
    )
    private void renderVanilla(
            GuiGraphics graphics,
            float tickDelta,
            CallbackInfo ci,
            @Local(name = {"currentY"}) LocalIntRef currentY,
            @Local boolean rAlign,
            @Local Font font,
            @Local(name = {"width"}) int width
    ) {
        if (FMAClient.features().enableVanillaEffectInUMMHud) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                if (!player.getActiveEffects().isEmpty()) {
                    this.fma$renderLine(font, graphics, Component.translatable("hud.fma.ummEffects.vanillaCategory"), rAlign, width, currentY);
                }

                for (Component text : this.fma$getTextToRender(rAlign)) {
                    this.fma$renderLine(font, graphics, text, rAlign, width, currentY);
                }

                if (!this.effects.isEmpty()) {
                    this.fma$renderLine(font, graphics, Component.translatable("hud.fma.ummEffects.monumentaCategory"), rAlign, width, currentY);
                }
            }
        }
    }

    @ModifyReturnValue(
            method = {"getHeight"},
            at = {@At("RETURN")}
    )
    private int modifyHeight(int original) {
        if (!FMAClient.features().enableVanillaEffectInUMMHud) {
            return original;
        } else {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return original;
            } else {
                original += 11 * player.getActiveEffects().size();
                if (!this.effects.isEmpty()) {
                    original += 11;
                }

                if (!player.getActiveEffects().isEmpty()) {
                    original += 11;
                }

                return original;
            }
        }
    }

    @ModifyReturnValue(
            method = {"isVisible"},
            at = {@At("RETURN")}
    )
    private boolean setVisibleVanillaEffects(boolean original) {
        if (!FMAClient.features().enableVanillaEffectInUMMHud) {
            return original;
        } else {
            LocalPlayer player = Minecraft.getInstance().player;
            return player == null ? original : original || !player.getActiveEffects().isEmpty();
        }
    }
}
