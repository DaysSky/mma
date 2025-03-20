package com.floweytf.fma.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NBTUtil {
    public record Access(CompoundTag tag) {
        @SuppressWarnings("unchecked")
        private <T extends Tag> Optional<T> resolve(String key, String... parentPathFrag) {
            CompoundTag root = tag;

            for (String s : parentPathFrag) {
                if (!root.contains(s, Tag.TAG_COMPOUND)) {
                    return Optional.empty();
                }

                root = root.getCompound(s);
            }

            return Optional.ofNullable((T) root.get(key));
        }

        private <T> Optional<T> primitive(String key, byte type, Function<Tag, T> map, String... parentPathFrag) {
            return resolve(key, parentPathFrag).flatMap(tag -> {
                if (tag.getId() != type) {
                    return Optional.empty();
                }

                return Optional.of(map.apply(tag));
            });
        }

        public Optional<String> getTier() {
            return primitive("Tier", Tag.TAG_STRING, Tag::getAsString, MONUMENTA_KEY);
        }

        public Optional<CompoundTag> getPlayerModified() {
            return resolve("PlayerModified", MONUMENTA_KEY);
        }

        public Optional<Integer> getCharmPower() {
            return primitive("CharmPower", Tag.TAG_INT, tag -> ((IntTag) tag).getAsInt(), MONUMENTA_KEY);
        }

        public Optional<String> getPlainName() {
            return primitive("Name", Tag.TAG_STRING, Tag::getAsString, PLAIN_KEY, DISPLAY_KEY);
        }

        public boolean isVirtualItem() {
            return primitive("IsVirtualItem", Tag.TAG_BYTE, tag -> ((ByteTag) tag).getAsByte() == 1, MONUMENTA_KEY)
                .orElse(false);
        }

        public List<String> getPlainLore() {
            return this.<ListTag>resolve("Lore", PLAIN_KEY, DISPLAY_KEY).map(
                x -> {
                    // TODO: streams are slow for some reason...
                    final List<String> list = new ArrayList<>(x.size());
                    for (final var t : x) {
                        list.add(t.getAsString());
                    }

                    return list;
                }
            ).orElse(List.of());
        }

        public List<String> getRawLore() {
            return this.<ListTag>resolve("Lore", DISPLAY_KEY).map(
                x -> {
                    // TODO: streams are slow for some reason...
                    final List<String> list = new ArrayList<>(x.size());
                    for (final var t : x) {
                        final var line = Component.Serializer.fromJson(t.getAsString());
                        list.add(line == null ? "" : line.getString());
                    }

                    return list;
                }
            ).orElse(List.of());
        }

        public Map<String, Integer> getEnchants() {
            return this.<CompoundTag>resolve("Enchantments", MONUMENTA_KEY, "Stock").map(tag ->
                tag.entries().entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    x -> ((CompoundTag) x.getValue()).getInt("Level")
                ))
            ).orElse(Map.of());
        }

        public ListTag getContainedItemsTag() {
            return this.<ListTag>resolve("Items", "BlockEntityTag").orElse(new ListTag());
        }
    }

    public static final String MONUMENTA_KEY = "Monumenta";
    public static final String DISPLAY_KEY = "display";
    public static final String PLAIN_KEY = "plain";
    public static final List<String> BLOCK_PLACER = List.of("Doorway from Eternity", "Worldshaper's Loom", "Firmament");

    public static Map<String, Integer> getAllEnchants(Player player) {
        final var map = new HashMap<String, Integer>();

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
