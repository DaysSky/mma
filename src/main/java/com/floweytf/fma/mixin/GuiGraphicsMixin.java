package com.floweytf.fma.mixin;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.Graphics;
import com.floweytf.fma.features.cz.CharmItemManager;
import static com.floweytf.fma.util.FormatUtil.literal;
import com.floweytf.fma.util.NBTUtil;
import com.floweytf.fma.util.Util;
import static com.floweytf.fma.util.Util.c;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Shadow
    @Final
    private PoseStack pose;

    @Inject(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;" +
            "IILjava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"
        )
    )
    private void renderItemOverlay(Font font, ItemStack stack, int x, int y, String string, CallbackInfo ci) {
        try {
            final var config = FMAClient.features().inventoryOverlay;

            if (!config.enable) {
                return;
            }

            final var tier = NBTUtil.getTier(stack);
            final var czCharmData = NBTUtil.getPlayerModified(stack);
            final var charmPower = NBTUtil.getCharmPower(stack);

            if (config.enableRarity) {
                tier.flatMap(t -> Util.get(Graphics.RARITY_TO_TEXTURE, t))
                    .ifPresent(texture -> Graphics.renderTexture(c(this), x, y, 0, 0, 16, 16, 16, 16, texture));
            }

            if (config.enableCZCharmRarity) {
                czCharmData.flatMap(c -> NBTUtil.getInt(c, CharmItemManager.CHARM_RARITY_KEY))
                    .map(c -> Graphics.CHARM_RARITY_TO_TEXTURE.get(c - 1))
                    .ifPresent(texture -> Graphics.renderTexture(c(this), x, y, 0, 0, 16, 16, 16, 16, texture));
            }

            if (config.enableCooldown && charmPower.isEmpty()) {
                final var cooldowns = FMAClient.player().getCooldowns();
                if (cooldowns.cooldowns.containsKey(stack.getItem())) {
                    final var endTime = cooldowns.cooldowns.get(stack.getItem()).endTime;
                    final var startTime = cooldowns.cooldowns.get(stack.getItem()).startTime;
                    final var currTime = cooldowns.tickCount;
                    final var timeLeft = (endTime - currTime + 19) / 20;
                    Graphics.drawString(
                        c(this), font, Component.literal(String.valueOf(timeLeft)), x, y, 200,
                        0xff000000 | Util.colorRange(currTime - startTime, endTime - startTime)
                    );
                }
            }

            if (config.enableCZCharmPower && charmPower.isPresent()) {
                pose.pushPose();
                pose.translate(x, y, 200);
                pose.scale(0.5f, 0.5f, 1);

                final var cp = charmPower.get();
                final var str = cp < 4 ? "★".repeat(cp) : cp + "★";

                czCharmData.ifPresent(compoundTag -> Graphics.drawString(
                    c(this), font,
                    Component.literal(str),
                    0, 0, 0xFFFFFA75
                ));
                pose.popPose();
            }

            final var itemName = NBTUtil.getPlainName(stack);

            if (config.enablePICount && (itemName.contains("Potion Injector") || itemName.contains("Iridium Injector"))) {
                final var countOpt = NBTUtil.getLore(stack).flatMap(lore -> {
                    boolean f = false;
                    for (final var tag : lore) {
                        final var str = NBTUtil.jsonToRaw(tag.getAsString());

                        if (f) {
                            return Optional.of(str);
                        }
                        if (str.contains("Selected Potion")) {
                            f = true;
                        }
                    }

                    return Optional.empty();
                }).flatMap(potionName -> NBTUtil.getInventory(stack)
                    .map(inventory -> inventory.stream()
                        .filter(item -> NBTUtil.getPlainName(item).equals(potionName))
                        .map(ItemStack::getCount)
                        .reduce(0, Integer::sum)
                    )
                );

                countOpt.ifPresent(count -> {
                    final var t = literal(count);

                    Graphics.drawString(
                        c(this), font, t,
                        x + 17 - font.width(t), y + 9, 200, 0xff000000 | Util.colorRange(count, 27)
                    );
                });
            }

            if (config.enableLoomFirmCount && NBTUtil.BLOCK_PLACER.stream().anyMatch(itemName::contains)) {
                final var countOpt = NBTUtil.getInventory(stack)
                    .map(inventory -> inventory.stream()
                        .map(ItemStack::getCount)
                        .reduce(0, Integer::sum)
                    );

                countOpt.ifPresent(count -> {
                    final var t = literal(count);

                    if (count < 100) {
                        Graphics.drawString(
                            c(this), font, t,
                            x + 17 - font.width(t), y + 9, 200, 0xffffffff
                        );
                    } else {
                        pose.pushPose();
                        pose.translate(x + 17 - (float) font.width(t) / 2, y + 12, 200);
                        pose.scale(0.5f, 0.5f, 1);

                        Graphics.drawString(
                            c(this), font, t,
                            0, 0, 0xffffffff
                        );

                        pose.popPose();
                    }
                });
            }
        } catch (Exception e) {
            FMAClient.LOGGER.error("rendering fail", e);
        }
    }

    @Inject(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;" +
            "IILjava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isBarVisible()Z"
        )
    )
    private void renderVanityDurability(Font font, ItemStack stack, int x, int y, String string, CallbackInfo ci) {
        final var res = NBTUtil.getVanityDurabilityInfo(stack);
        if (res.isEmpty() || !FMAClient.features().enableVanityDurability) {
            return;
        }

        final var data = res.get();
        final var durability = data.firstInt();
        final var maxDurability = data.secondInt();

        if (durability == maxDurability) {
            return;
        }

        RenderSystem.disableDepthTest();
        int width = Math.round((durability * 13.0f) / maxDurability);
        int color = Util.colorRange(durability, maxDurability);
        int barX = x + 2;
        int barY = y + 13;
        Graphics.fill(c(this), barX, barY, barX + 13, barY + 2, 0xff000000);
        Graphics.fill(c(this), barX, barY, barX + width, barY + 1, color | 0xFF000000);
        RenderSystem.enableDepthTest();
    }
}
