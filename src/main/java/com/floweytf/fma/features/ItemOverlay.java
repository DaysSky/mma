package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.Graphics;
import com.floweytf.fma.features.cz.CharmItemManager;
import com.floweytf.fma.util.NBTUtil;
import com.floweytf.fma.util.Util;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import java.util.Scanner;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import static com.floweytf.fma.util.FormatUtil.literal;

public class ItemOverlay {
    private static void renderRarity(GuiGraphics graphics, NBTUtil.Access access, int x, int y) {
        access.getTier()
            .flatMap(t -> Util.get(Graphics.RARITY_TO_TEXTURE, t))
            .ifPresent(texture -> Graphics.renderTexture(graphics, x, y, 0, 0, 16, 16, 16, 16, texture));
    }

    private static void renderCzCharmRarity(GuiGraphics graphics, CompoundTag czCharmData, int x, int y) {
        final var level = czCharmData.getInt(CharmItemManager.CHARM_RARITY_KEY);
        if (level > 0) {
            final var texture = Graphics.CHARM_RARITY_TO_TEXTURE.get(level - 1);
            Graphics.renderTexture(graphics, x, y, 0, 0, 16, 16, 16, 16, texture);
        }
    }

    private static void renderCooldowns(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        final var cooldowns = FMAClient.player().getCooldowns();
        if (cooldowns.cooldowns.containsKey(stack.getItem())) {
            final var endTime = cooldowns.cooldowns.get(stack.getItem()).endTime;
            final var startTime = cooldowns.cooldowns.get(stack.getItem()).startTime;
            final var currTime = cooldowns.tickCount;
            final var timeLeft = (endTime - currTime + 19) / 20;
            Graphics.drawString(
                graphics, font, Component.literal(String.valueOf(timeLeft)), x, y, 200,
                0xff000000 | Util.colorRange(currTime - startTime, endTime - startTime)
            );
        }
    }

    private static void renderCzCharmPower(GuiGraphics graphics, Font font, int charmPower, int x, int y) {
        final var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 200);
        pose.scale(0.5f, 0.5f, 1);

        final var str = charmPower < 4 ? "★".repeat(charmPower) : charmPower + "★";
        Graphics.drawString(graphics, font, Component.literal(str), 0, 0, 0xFFFFFA75);
        pose.popPose();
    }

    private static void renderPiCount(GuiGraphics graphics, Font font, NBTUtil.Access access, int x, int y) {
        // don't use stream API because the overhead actually matters
        String target = null;
        final var lore = access.getPlainLore();
        for (int i = 0; i < lore.size() - 1; i++) {
            if (lore.get(i).contains("Selected Potion")) {
                target = lore.get(i + 1);
            }
        }

        if (target == null) {
            return;
        }

        final var finalTarget = target;

        var count = 0;
        for (final var tag : access.getContainedItemsTag()) {
            final var compoundTag = ((CompoundTag) tag);
            final var stackCount = compoundTag.getInt("Count");
            final var stackAccess = NBTUtil.access(compoundTag.getCompound("tag"));

            if (stackAccess.getPlainName().orElse("").equals(finalTarget)) {
                count += stackCount;
            }
        }

        final var text = literal(count);

        Graphics.drawString(
            graphics, font, text,
            x + 17 - font.width(text), y + 9, 200, 0xff000000 | Util.colorRange(count, 27)
        );
    }

    private static void renderPlacerCount(GuiGraphics graphics, Font font, NBTUtil.Access access, int x, int y) {
        var count = 0;

        for (final var tag : access.getContainedItemsTag()) {
            final var compoundTag = ((CompoundTag) tag);
            count += compoundTag.getInt("Count");
        }

        final var t = literal(count);

        if (count < 100) {
            Graphics.drawString(graphics, font, t, x + 17 - font.width(t), y + 9, 200, 0xffffffff);
        } else {
            final var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + 17 - (float) font.width(t) / 2, y + 12, 200);
            pose.scale(0.5f, 0.5f, 1);
            Graphics.drawString(graphics, font, t, 0, 0, 0xffffffff);
            pose.popPose();
        }
    }

    private static void renderItemOverlay0(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        final var config = FMAClient.features().inventoryOverlay;

        if (!config.enable) {
            return;
        }

        final var dataAccess = NBTUtil.access(stack);
        final var tier = dataAccess.getTier();
        final var czCharmData = dataAccess.getPlayerModified();
        final var charmPower = dataAccess.getCharmPower();

        if (config.enableRarity) {
            renderRarity(graphics, dataAccess, x, y);
        }

        if (config.enableCZCharmRarity && czCharmData.isPresent()) {
            renderCzCharmRarity(graphics, czCharmData.get(), x, y);
        }

        if (config.enableCZCharmPower && charmPower.isPresent() && czCharmData.isPresent()) {
            renderCzCharmPower(graphics, font, charmPower.get(), x, y);
        }

        // ignore for charms
        if (config.enableCooldown && charmPower.isEmpty()) {
            renderCooldowns(graphics, font, stack, x, y);
        }

        final var itemName = dataAccess.getPlainName().orElseGet(() -> stack.getHoverName().getString());

        if (config.enablePICount && (itemName.contains("Potion Injector") || itemName.contains("Iridium Injector"))) {
            renderPiCount(graphics, font, dataAccess, x, y);
        }

        if (config.enableLoomFirmCount && NBTUtil.BLOCK_PLACER.stream().anyMatch(itemName::contains)) {
            renderPlacerCount(graphics, font, dataAccess, x, y);
        }
    }

    public static void renderItemOverlay(GuiGraphics context, Font font, ItemStack stack, int x, int y) {
        try {
            renderItemOverlay0(context, font, stack, x, y);
        } catch (Throwable e) {
            FMAClient.LOGGER.warn("ItemOverlay#renderItemOverlay: ", e);
        }
    }

    public static void renderVanityDurability0(GuiGraphics graphics, ItemStack stack, int x, int y) {
        final var lore = NBTUtil.access(stack).getPlainLore();

        if (lore.isEmpty()) {
            return;
        }

        final var line = lore.get(lore.size() - 1);

        if (!line.startsWith("Durability: ")) {
            return;
        }

        final var scanner = new Scanner(line);
        scanner.next();
        final var durability = scanner.nextInt();
        scanner.next();
        final var maxDurability = scanner.nextInt();

        if (durability == maxDurability) {
            return;
        }

        RenderSystem.disableDepthTest();
        int width = Math.round((durability * 13.0f) / maxDurability);
        int color = Util.colorRange(durability, maxDurability);
        int barX = x + 2;
        int barY = y + 13;
        Graphics.fill(graphics, barX, barY, barX + 13, barY + 2, 0xff000000);
        Graphics.fill(graphics, barX, barY, barX + width, barY + 1, color | 0xFF000000);
        RenderSystem.enableDepthTest();
    }

    public static void renderVanityDurability(GuiGraphics graphics, ItemStack stack, int x, int y) {
        try {
            renderVanityDurability0(graphics, stack, x, y);
        } catch (Throwable e) {
            FMAClient.LOGGER.warn("ItemOverlay#renderVanityDurability: ", e);
        }
    }
}