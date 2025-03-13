package com.floweytf.fma.util;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NBTUtil {
    public static final String MONUMENTA_KEY = "Monumenta";
    public static final String BLOCK_ENTITY_KEY = "BlockEntityTag";
    public static final String DISPLAY_KEY = "display";
    public static final String LORE_KEY = "Lore";
    public static final String NAME_KEY = "Name";
    public static final String STOCK = "Stock";
    public static final String MONUMENTA_TIER_KEY = "Tier";
    public static final String MONUMENTA_CHARM_POWER_KEY = "CharmPower";
    public static final String PLAIN_KEY = "plain";

    public static final String PLAYER_MODIFIED = "PlayerModified";
    public static final List<String> BLOCK_PLACER = List.of("Doorway from Eternity", "Worldshaper's Loom", "Firmament");

    public static Optional<CompoundTag> get(ItemStack stack) {
        return Optional.ofNullable(stack.getTag());
    }

    public static Optional<CompoundTag> getCompound(CompoundTag root, String name) {
        if (!root.contains(name, Tag.TAG_COMPOUND))
            return Optional.empty();
        return Optional.of(root.getCompound(name));
    }

    public static Optional<String> getString(CompoundTag root, String name) {
        if (!root.contains(name, Tag.TAG_STRING))
            return Optional.empty();
        return Optional.of(root.getString(name));
    }

    public static Optional<Integer> getInt(CompoundTag root, String name) {
        if (!root.contains(name, Tag.TAG_INT))
            return Optional.empty();
        return Optional.of(root.getInt(name));
    }

    public static Optional<Double> getDouble(CompoundTag root, String name) {
        if (!root.contains(name, Tag.TAG_DOUBLE))
            return Optional.empty();
        return Optional.of(root.getDouble(name));
    }

    public static Optional<ListTag> getList(CompoundTag root, String name, int type) {
        if (!root.contains(name, Tag.TAG_LIST))
            return Optional.empty();
        return Optional.of(root.getList(name, type));
    }

    public static Optional<CompoundTag> getMonumenta(ItemStack stack) {
        return get(stack).flatMap(tag -> getCompound(tag, MONUMENTA_KEY));
    }


    public static Optional<CompoundTag> getPlayerModified(ItemStack stack) {
        return getMonumenta(stack).flatMap(tag -> getCompound(tag, PLAYER_MODIFIED));
    }

    public static Optional<CompoundTag> getStock(ItemStack stack) {
        return getMonumenta(stack).flatMap(tag -> getCompound(tag, STOCK));
    }

    public static Optional<CompoundTag> getEnchantsTag(ItemStack stack) {
        return getStock(stack).flatMap(tag -> getCompound(tag, "Enchantments"));
    }

    public static Map<String, Integer> getEnchants(ItemStack stack) {
        return getEnchantsTag(stack)
            .map(v -> v.entries().entrySet().stream().collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        (entry) -> ((CompoundTag) entry.getValue()).getInt("Level"))
                )
            )
            .orElse(Map.of());
    }

    public static Optional<String> getTier(ItemStack stack) {
        return getMonumenta(stack).flatMap(tag -> getString(tag, MONUMENTA_TIER_KEY));
    }

    public static Optional<Integer> getCharmPower(ItemStack stack) {
        return getMonumenta(stack).flatMap(tag -> getInt(tag, MONUMENTA_CHARM_POWER_KEY));
    }

    public static Optional<CompoundTag> getDisplay(ItemStack stack) {
        return get(stack).flatMap(tag -> getCompound(tag, DISPLAY_KEY));
    }

    public static Optional<CompoundTag> getBlockEntityData(ItemStack stack) {
        return get(stack).flatMap(tag -> getCompound(tag, BLOCK_ENTITY_KEY));
    }

    public static Optional<ListTag> getLore(ItemStack stack) {
        return getDisplay(stack).flatMap(tag -> getList(tag, LORE_KEY, Tag.TAG_STRING));
    }

    public static Optional<CompoundTag> getPlainTag(ItemStack stack) {
        return get(stack).flatMap(tag -> getCompound(tag, PLAIN_KEY));
    }

    public static String getPlainName(ItemStack stack) {
        return getPlainTag(stack)
            .flatMap(tag -> getString(tag, NAME_KEY))
            .orElseGet(() -> stack.getHoverName().getString());
    }

    public static Optional<IntIntPair> getVanityDurabilityInfo(ItemStack stack) {
        // Items with durability don't have custom vanity info
        if (stack.getItem().canBeDepleted()) {
            return Optional.empty();
        }

        return getLore(stack).flatMap(loreTag -> {
            if (loreTag.size() < 2) {
                return Optional.empty();
            }

            final var lastLine = NBTUtil.jsonToRaw(loreTag.getString(loreTag.size() - 1));

            if (!lastLine.startsWith("Durability: ")) {
                return Optional.empty();
            }

            final var parts = lastLine.split("\\s+");
            if (parts.length != 4) {
                return Optional.empty();
            }

            return Optional.of(IntIntPair.of(
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[3])
            ));
        });
    }

    public static Optional<List<ItemStack>> getInventory(ItemStack stack) {
        return NBTUtil.getBlockEntityData(stack)
            .flatMap(t -> NBTUtil.getList(t, "Items", Tag.TAG_COMPOUND))
            .map(list -> list.stream().map(data -> ItemStack.of((CompoundTag) data)).toList());
    }

    public static String jsonToRaw(String name) {
        return Objects.requireNonNull(Component.Serializer.fromJson(name)).getString();
    }

    public static int getEnchantLevel(Player player, String name) {
        return Arrays.stream(EquipmentSlot.values())
            .map(player::getItemBySlot)
            .map(item -> getEnchants(item).getOrDefault(name, 0))
            .reduce(0, Integer::sum);
    }
}
