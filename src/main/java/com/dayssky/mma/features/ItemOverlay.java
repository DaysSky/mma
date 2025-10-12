package com.dayssky.mma.features;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.Graphics;
import com.dayssky.mma.MMAConfig.InventoryOverlayToggles;
import com.dayssky.mma.util.FormatUtil;
import com.dayssky.mma.util.NBTUtil;
import com.dayssky.mma.util.SafeExceptionLogger;
import com.dayssky.mma.util.Util;
import com.dayssky.mma.util.Access;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemCooldowns.CooldownInstance;

public class ItemOverlay {
    private static final SafeExceptionLogger EXCEPTION_LOGGER = new SafeExceptionLogger("ItemOverlay");

    private static Optional<RenderOp> renderRarity(Access access) {
        return access.getTier()
                .flatMap(t -> Util.get(Graphics.RARITY_TO_TEXTURE, t))
                .map(texture -> (g, f, x, y) -> Graphics.renderTexture(g, x, y, 0.0F, 0.0F, 16, 16, 16, 16, texture));
    }

    private static Optional<RenderOp> renderCzCharmRarity(CompoundTag czCharmData) {
        int level = czCharmData.getInt("DEPTHS_CHARM_RARITY");
        if (level > 0) {
            ResourceLocation texture = Graphics.CHARM_RARITY_TO_TEXTURE.get(level - 1);
            return Optional.of((g, f, x, y) -> Graphics.renderTexture(g, x, y, 0.0F, 0.0F, 16, 16, 16, 16, texture));
        } else {
            return Optional.empty();
        }
    }

    private static RenderOp renderCzCharmPower(int charmPower) {
        MutableComponent str = Component.literal(charmPower < 4 ? "★".repeat(charmPower) : charmPower + "★");
        return (g, f, x, y) -> {
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(x, y, 200.0F);
            pose.scale(0.5F, 0.5F, 1.0F);
            Graphics.drawString(g, f, str, 0, 0, -1419);
            pose.popPose();
        };
    }

    private static void renderCooldowns(ItemStack stack, GuiGraphics graphics, Font font, int x, int y) {
        ItemCooldowns cooldowns = MMAClient.player().getCooldowns();
        if (cooldowns.cooldowns.containsKey(stack.getItem())) {
            int endTime = ((CooldownInstance) cooldowns.cooldowns.get(stack.getItem())).endTime;
            int startTime = ((CooldownInstance) cooldowns.cooldowns.get(stack.getItem())).startTime;
            int currTime = cooldowns.tickCount;
            int timeLeft = (endTime - currTime + 19) / 20;
            Graphics.drawString(
                    graphics, font, Component.literal(String.valueOf(timeLeft)), x, y, 200, 0xFF000000 | Util.colorRange(currTime - startTime, endTime - startTime)
            );
        }
    }

    private static Optional<RenderOp> renderPICount(Access access) {
        String target = null;
        List<String> lore = access.getPlainLore();

        for (int i = 0; i < lore.size() - 1; i++) {
            if (lore.get(i).contains("Selected Potion")) {
                target = lore.get(i + 1);
            }
        }

        if (target == null) {
            return Optional.empty();
        } else {
            String finalTarget = target;
            int count = 0;

            for (Tag tag : access.getContainedItemsTag()) {
                CompoundTag compoundTag = (CompoundTag) tag;
                int stackCount = compoundTag.getInt("Count");
                Access stackAccess = NBTUtil.access(compoundTag.getCompound("tag"));
                if (stackAccess.getPlainName().orElse("").equals(finalTarget)) {
                    count += stackCount;
                }
            }

            MutableComponent text = FormatUtil.literal(count);
            int finalCount = count;
            return Optional.of((g, f, x, y) -> Graphics.drawString(g, f, text, x + 17 - f.width(text), y + 9, 200, 0xFF000000 | Util.colorRange(finalCount, 27)));
        }
    }

    private static Optional<RenderOp> renderIICount(Access access) {
        int count = 0;

        for (Tag tag : access.getContainedItemsTag()) {
            CompoundTag compoundTag = (CompoundTag) tag;
            int stackCount = compoundTag.getInt("Count");
            count += stackCount;
        }

        MutableComponent text = FormatUtil.literal(count);
        int finalCount = count;
        return Optional.of((g, f, x, y) -> Graphics.drawString(g, f, text, x + 17 - f.width(text), y + 9, 200, 0xFF000000 | Util.colorRange(finalCount, 27)));
    }

    private static RenderOp renderPlacerCount(Access access) {
        int count = 0;

        for (Tag tag : access.getContainedItemsTag()) {
            CompoundTag compoundTag = (CompoundTag) tag;
            count += compoundTag.getInt("Count");
        }

        MutableComponent t = FormatUtil.literal(count);
        return count < 100 ? (g, f, x, y) -> Graphics.drawString(g, f, t, x + 17 - f.width(t), y + 9, 200, -1) : (g, f, x, y) -> {
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(x + 17 - f.width(t) / 2.0F, y + 12, 200.0F);
            pose.scale(0.5F, 0.5F, 1.0F);
            Graphics.drawString(g, f, t, 0, 0, -1);
            pose.popPose();
        };
    }

    private static Optional<RenderOp> renderVanityDurability(ItemStack stack) {
        Access access = NBTUtil.access(stack);
        if (!access.isVirtualItem()) {
            return Optional.empty();
        } else {
            List<String> lore = access.getRawLore();
            if (lore.isEmpty()) {
                return Optional.empty();
            } else {
                String line = lore.get(lore.size() - 1);
                if (!line.startsWith("Durability: ")) {
                    return Optional.empty();
                } else {
                    Scanner scanner = new Scanner(line);
                    scanner.next();
                    int durability = scanner.nextInt();
                    scanner.next();
                    int maxDurability = scanner.nextInt();
                    if (durability == maxDurability) {
                        return Optional.empty();
                    } else {
                        int width = Math.round(durability * 13.0F / maxDurability);
                        int color = Util.colorRange(durability, maxDurability);
                        return Optional.of((graphics, font, x, y) -> {
                            int barX = x + 2;
                            int barY = y + 13;
                            graphics.pose().pushPose();
                            graphics.pose().translate(0.0F, 0.0F, 1000.0F);
                            Graphics.fill(graphics, barX, barY, barX + 13, barY + 2, -16777216);
                            Graphics.fill(graphics, barX, barY, barX + width, barY + 1, color | 0xFF000000);
                            graphics.pose().popPose();
                        });
                    }
                }
            }
        }
    }

    private static List<RenderOp> buildRenderOps(ItemStack stack) {
        InventoryOverlayToggles config = MMAClient.features().inventoryOverlay;
        Access dataAccess = NBTUtil.access(stack);
        Optional<CompoundTag> czCharmData = dataAccess.getPlayerModified();
        Optional<Integer> charmPower = dataAccess.getCharmPower();
        ArrayList<RenderOp> list = new ArrayList<>();
        if (config.enableRarity) {
            renderRarity(dataAccess).ifPresent(list::add);
        }

        if (config.enableCZCharmRarity && czCharmData.isPresent()) {
            renderCzCharmRarity(czCharmData.get()).ifPresent(list::add);
        }

        if (config.enableCZCharmPower && charmPower.isPresent() && czCharmData.isPresent()) {
            list.add(renderCzCharmPower(charmPower.get()));
        }

        String itemName = dataAccess.getPlainName().orElseGet(() -> stack.getHoverName().getString());
        if (config.enablePICount && itemName.contains("Potion Injector")) {
            renderPICount(dataAccess).ifPresent(list::add);
        }

        if (config.enablePICount && itemName.contains("Iridium Injector")) {
            renderIICount(dataAccess).ifPresent(list::add);
        }

        if (config.enableLoomFirmCount && NBTUtil.BLOCK_PLACER.stream().anyMatch(itemName::contains)) {
            list.add(renderPlacerCount(dataAccess));
        }

        if (MMAClient.features().enableVanityDurability) {
            renderVanityDurability(stack).ifPresent(list::add);
        }

        return list;
    }

    public static void renderItemOverlay(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        EXCEPTION_LOGGER.runSafely(() -> {
            InventoryOverlayToggles config = MMAClient.features().inventoryOverlay;
            if (config.enable) {
                if (config.enableCooldown && NBTUtil.access(stack).getCharmPower().isEmpty()) {
                    renderCooldowns(stack, graphics, font, x, y);
                }

                stack.mma$getOverlayRenderCache().render(config.updateDelayTicks, () -> buildRenderOps(stack), graphics, font, x, y);
            }
        });
    }

    @FunctionalInterface
    public interface RenderOp {
        void render(GuiGraphics graphics, Font font, int x, int y);
    }
}
