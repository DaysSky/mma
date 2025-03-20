package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.Graphics;
import com.floweytf.fma.features.cz.CharmItemManager;
import com.floweytf.fma.util.NBTUtil;
import com.floweytf.fma.util.Util;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import static com.floweytf.fma.util.FormatUtil.literal;

public class ItemOverlay {
    @FunctionalInterface
    public interface RenderOp {
        void render(GuiGraphics graphics, Font font, int x, int y);
    }

    private static Optional<RenderOp> renderRarity(NBTUtil.Access access) {
        return access.getTier()
            .flatMap(t -> Util.get(Graphics.RARITY_TO_TEXTURE, t))
            .map(texture -> (g, f, x, y) -> Graphics.renderTexture(g, x, y, 0, 0, 16, 16, 16, 16, texture));
    }

    private static Optional<RenderOp> renderCzCharmRarity(CompoundTag czCharmData) {
        final var level = czCharmData.getInt(CharmItemManager.CHARM_RARITY_KEY);
        if (level > 0) {
            final var texture = Graphics.CHARM_RARITY_TO_TEXTURE.get(level - 1);
            return Optional.of((g, f, x, y) -> Graphics.renderTexture(g, x, y, 0, 0, 16, 16, 16, 16, texture));
        }

        return Optional.empty();
    }

    private static RenderOp renderCzCharmPower(int charmPower) {
        final var str = Component.literal(charmPower < 4 ? "★".repeat(charmPower) : charmPower + "★");

        return (g, f, x, y) -> {
            final var pose = g.pose();
            pose.pushPose();
            pose.translate(x, y, 200);
            pose.scale(0.5f, 0.5f, 1);


            Graphics.drawString(g, f, str, 0, 0, 0xFFFFFA75);
            pose.popPose();
        };
    }

    // can't defer this
    private static void renderCooldowns(ItemStack stack, GuiGraphics graphics, Font font, int x, int y) {
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

    private static Optional<RenderOp> renderPiCount(NBTUtil.Access access) {
        // don't use stream API because the overhead actually matters
        String target = null;
        final var lore = access.getPlainLore();
        for (int i = 0; i < lore.size() - 1; i++) {
            if (lore.get(i).contains("Selected Potion")) {
                target = lore.get(i + 1);
            }
        }

        if (target == null) {
            return Optional.empty();
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

        int finalCount = count;
        return Optional.of((g, f, x, y) -> Graphics.drawString(
            g, f, text,
            x + 17 - f.width(text), y + 9, 200, 0xff000000 | Util.colorRange(finalCount, 27)
        ));
    }

    private static RenderOp renderPlacerCount(NBTUtil.Access access) {
        var count = 0;

        for (final var tag : access.getContainedItemsTag()) {
            final var compoundTag = ((CompoundTag) tag);
            count += compoundTag.getInt("Count");
        }

        final var t = literal(count);

        if (count < 100) {
            return (g, f, x, y) -> Graphics.drawString(g, f, t, x + 17 - f.width(t), y + 9, 200, 0xffffffff);
        } else {
            return (g, f, x, y) -> {
                final var pose = g.pose();
                pose.pushPose();
                pose.translate(x + 17 - (float) f.width(t) / 2, y + 12, 200);
                pose.scale(0.5f, 0.5f, 1);
                Graphics.drawString(g, f, t, 0, 0, 0xffffffff);
                pose.popPose();
            };
        }
    }

    private static List<RenderOp> buildRenderOps(ItemStack stack) {
        final var config = FMAClient.features().inventoryOverlay;

        final var dataAccess = NBTUtil.access(stack);
        final var czCharmData = dataAccess.getPlayerModified();
        final var charmPower = dataAccess.getCharmPower();
        final var list = new ArrayList<RenderOp>();

        if (config.enableRarity) {
            renderRarity(dataAccess).ifPresent(list::add);
        }

        if (config.enableCZCharmRarity && czCharmData.isPresent()) {
            renderCzCharmRarity(czCharmData.get()).ifPresent(list::add);
        }

        if (config.enableCZCharmPower && charmPower.isPresent() && czCharmData.isPresent()) {
            list.add(renderCzCharmPower(charmPower.get()));
        }

        final var itemName = dataAccess.getPlainName().orElseGet(() -> stack.getHoverName().getString());

        if (config.enablePICount && (itemName.contains("Potion Injector") || itemName.contains("Iridium Injector"))) {
            renderPiCount(dataAccess).ifPresent(list::add);
        }

        if (config.enableLoomFirmCount && NBTUtil.BLOCK_PLACER.stream().anyMatch(itemName::contains)) {
            list.add(renderPlacerCount(dataAccess));
        }

        return list;
    }

    public static void renderItemOverlay(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        try {
            final var config = FMAClient.features().inventoryOverlay;

            if (!config.enable) {
                return;
            }

            // ignore for charms, can't cache this
            if (config.enableCooldown && NBTUtil.access(stack).getCharmPower().isEmpty()) {
                renderCooldowns(stack, graphics, font, x, y);
            }

            final var rc = stack.fma$getRenderCache();
            if (rc.lastUpdateTick + config.updateDelayTicks <= Minecraft.getInstance().clientTickCount) {
                rc.lastUpdateTick = Minecraft.getInstance().clientTickCount;
                rc.renderOps = null;
            }

            if (rc.renderOps == null) {
                rc.renderOps = buildRenderOps(stack);
            }

            for (RenderOp renderOp : rc.renderOps) {
                renderOp.render(graphics, font, x, y);
            }
        } catch (Throwable e) {
            FMAClient.LOGGER.warn("ItemOverlay#renderItemOverlay: ", e);
        }
    }

    public static void renderVanityDurability0(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (!FMAClient.features().enableVanityDurability) {
            return;
        }

        final var access = NBTUtil.access(stack);

        if(!access.isVirtualItem()) {
            return;
        }

        final var lore = access.getRawLore();

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

        int width = Math.round((durability * 13.0f) / maxDurability);
        int color = Util.colorRange(durability, maxDurability);
        int barX = x + 2;
        int barY = y + 13;
        graphics.pose().pushPose();
        graphics.pose().translate(0, -1000, 0);
        Graphics.fill(graphics, barX, barY, barX + 13, barY + 2, 0xff000000);
        Graphics.fill(graphics, barX, barY, barX + width, barY + 1, color | 0xFF000000);
        graphics.pose().popPose();
    }

    public static void renderVanityDurability(GuiGraphics graphics, ItemStack stack, int x, int y) {
        try {
            renderVanityDurability0(graphics, stack, x, y);
        } catch (Throwable e) {
            FMAClient.LOGGER.warn("ItemOverlay#renderVanityDurability: ", e);
        }
    }
}