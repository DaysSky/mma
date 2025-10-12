package com.floweytf.fma.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component.Serializer;

public record Access(CompoundTag tag) {
    private <T extends Tag> Optional<T> resolve(String key, String... parentPathFrag) {
        CompoundTag root = this.tag;

        for (String s : parentPathFrag) {
            if (!root.contains(s, 10)) {
                return Optional.empty();
            }

            root = root.getCompound(s);
        }

        return Optional.ofNullable((T)root.get(key));
    }

    private <T> Optional<T> primitive(String key, byte type, Function<Tag, T> map, String... parentPathFrag) {
        return this.<Tag>resolve(key, parentPathFrag).flatMap(tag -> tag.getId() != type ? Optional.empty() : Optional.of(map.apply(tag)));
    }

    public Optional<String> getTier() {
        return this.primitive("Tier", (byte)8, Tag::getAsString, "Monumenta");
    }

    public Optional<CompoundTag> getPlayerModified() {
        return this.resolve("PlayerModified", "Monumenta");
    }

    public Optional<Integer> getCharmPower() {
        return this.primitive("CharmPower", (byte)3, tag -> ((IntTag)tag).getAsInt(), "Monumenta");
    }

    public Optional<String> getPlainName() {
        return this.primitive("Name", (byte)8, Tag::getAsString, "plain", "display");
    }

    public boolean isVirtualItem() {
        return this.<Boolean>primitive("IsVirtualItem", (byte)1, tag -> ((ByteTag)tag).getAsByte() == 1, "Monumenta").orElse(false);
    }

    public List<String> getPlainLore() {
        return this.<ListTag>resolve("Lore", "plain", "display").map(x -> {
            List<String> list = new ArrayList<>(x.size());

            for (Tag t : x) {
                list.add(t.getAsString());
            }

            return list;
        }).orElse(List.of());
    }

    public List<String> getRawLore() {
        return this.<ListTag>resolve("Lore", "display").map(x -> {
            List<String> list = new ArrayList<>(x.size());

            for (Tag t : x) {
                MutableComponent line = Serializer.fromJson(t.getAsString());
                list.add(line == null ? "" : line.getString());
            }

            return list;
        }).orElse(List.of());
    }

    public Map<String, Integer> getEnchants() {
        return this.<CompoundTag>resolve("Enchantments", "Monumenta", "Stock")
            .map(tag -> tag.entries().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> ((CompoundTag)x.getValue()).getInt("Level"))))
            .orElse(Map.of());
    }

    public ListTag getContainedItemsTag() {
        return this.<ListTag>resolve("Items", "BlockEntityTag").orElse(new ListTag());
    }
}
