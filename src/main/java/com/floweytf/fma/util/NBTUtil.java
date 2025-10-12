package com.floweytf.fma.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NBTUtil {
    public static final String MONUMENTA_KEY = "Monumenta";
    public static final String DISPLAY_KEY = "display";
    public static final String PLAIN_KEY = "plain";
    public static final List<String> BLOCK_PLACER = List.of("Doorway from Eternity", "Worldshaper's Loom", "Firmament");

    public static Map<String, Integer> getAllEnchants(Player player) {
        HashMap<String, Integer> map = new HashMap<>();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Map<String, Integer> enchantMap = access(player.getItemBySlot(slot)).getEnchants();
            enchantMap.forEach((k, v) -> map.merge(k, v, Integer::sum));
        }

        return map;
    }

    public static Access access(ItemStack stack) {
        return new Access(stack.getOrCreateTag());
    }

    public static Access access(CompoundTag tag) {
        return new Access(tag);
    }
}
