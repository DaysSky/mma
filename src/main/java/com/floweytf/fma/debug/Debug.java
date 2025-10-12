package com.floweytf.fma.debug;

import com.floweytf.fma.util.ChatUtil;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class Debug {
    public static boolean ENTITY_DEBUG = false;
    public static boolean BLOCK_DEBUG = false;

    private Debug() {
    }

    public static void init() {
        AttackEntityCallback.EVENT.register((AttackEntityCallback) (player, world, hand, entity, hitResult) -> {
            if (ENTITY_DEBUG && !player.isSpectator()) {
                dumpEntityInfo(entity);
            }

            return InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((AttackBlockCallback) (player, world, hand, pos, direction) -> {
            if (BLOCK_DEBUG && !player.isSpectator()) {
                ChatUtil.send(Component.literal("Block Info Dump").withStyle(ChatFormatting.BOLD));
                ChatUtil.send("x = %d, y = %d, z = %d".formatted(pos.getX(), pos.getY(), pos.getZ()));
                ChatUtil.send("direction = " + direction);
                ChatUtil.send("blockState = " + world.getBlockState(pos));
            }

            return InteractionResult.PASS;
        });
    }

    public static void dumpEntityInfo(Entity entity) {
        ChatUtil.send(Component.literal("Entity Info Dump").withStyle(ChatFormatting.BOLD));
        ChatUtil.send(Component.literal("getName = "), entity.getName());
        ChatUtil.send("type = " + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        if (entity.getCustomName() != null) {
            ChatUtil.send(Component.literal("getCustomName = "), entity.getCustomName());
        }

        ChatUtil.send("slots = {");

        for (ItemStack armorSlot : entity.getArmorSlots()) {
            ChatUtil.send("  " + armorSlot.toString() + armorSlot.getTag());
        }

        ChatUtil.send("}");
        ChatUtil.send("x = %f, y = %f, z = %f".formatted(entity.getX(), entity.getY(), entity.getZ()));
        ChatUtil.send("tags = " + entity.getTags());
    }
}
