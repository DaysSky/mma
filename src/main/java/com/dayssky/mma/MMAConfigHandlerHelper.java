package com.dayssky.mma;

import com.dayssky.mma.MMAConfig.DisplayCharmExamples;
import com.dayssky.mma.MMAConfig.Zenith;
import com.dayssky.mma.MMAConfig.ZenithAbilitySelection;
import com.dayssky.mma.features.cz.CharmLorePeliRenderer;
import com.dayssky.mma.features.cz.CharmLoreRenderer;
import com.dayssky.mma.features.cz.CharmLoreTabularRenderer;
import com.dayssky.mma.features.cz.ZenithModule;
import com.dayssky.mma.features.cz.data.Charm;
import com.dayssky.mma.features.cz.data.CharmDataRegistries;
import com.dayssky.mma.features.cz.data.CharmEffectType;
import com.dayssky.mma.features.cz.data.CharmType;
import com.dayssky.mma.util.NBTUtil;
import com.dayssky.mma.util.Util;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.io.IOUtils;

public class MMAConfigHandlerHelper {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(new TypeToken<Set<CharmEffectType>>() {
            }.getType(), new CharmEffectTypeSer())
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static class CharmEffectTypeSer implements JsonSerializer<Set<CharmEffectType>>, JsonDeserializer<Set<CharmEffectType>> {
        @Override
        public JsonElement serialize(Set<CharmEffectType> src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.stream().map(x -> x.name).collect(Collectors.toSet()));
        }

        @Override
        public Set<CharmEffectType> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Set<String> names = context.deserialize(json, new TypeToken<Set<String>>() {
            }.getType());
            return names.stream()
                    .map(name -> CharmDataRegistries.getMain().charmEffectType.getMap().values().stream()
                            .filter(type -> type.name.equals(name))
                            .findFirst())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
        }
    }

    private static BooleanListEntry buildEntryToggle(CharmEffectType x, ConfigEntryBuilder builder, Set<CharmEffectType> config) {
        return builder.startBooleanToggle(Component.literal(x.modifier), config.contains(x)).setSaveConsumer(value -> {
            if (value) {
                config.add(x);
            } else {
                config.remove(x);
            }
        }).setDefaultValue(false).build();
    }

    private static <T, U> Stream<AbstractConfigListEntry> buildClassifying(
            Stream<T> entries,
            Function<T, U> classifier,
            Function<U, Component> nameGetter,
            Function<Stream<T>, Stream<AbstractConfigListEntry>> b,
            ConfigEntryBuilder builder
    ) {
        return entries.collect(Collectors.groupingBy(classifier))
                .entrySet()
                .stream()
                .map(x -> builder.startSubCategory(nameGetter.apply(x.getKey()), b.apply((Stream<T>) x.getValue().stream()).toList()).build());
    }

    private static Charm readDummyCharm(String path) {
        try {
            Charm var4;
            try (InputStream reader = CharmDataRegistries.class.getResourceAsStream(path)) {
                Preconditions.checkState(reader != null);
                String data = IOUtils.toString(reader, StandardCharsets.UTF_8);
                CompoundTag tag = new TagParser(new StringReader(data)).readStruct();
                var4 = ZenithModule.getCharm(NBTUtil.access(tag), CharmDataRegistries.getDummy()).orElseThrow();
            }

            return var4;
        } catch (CommandSyntaxException | IOException var7) {
            return Util.sneakyThrow(var7);
        }
    }

    public static void register() {
        Map<CharmType, Charm> charmDummies = Map.of(
                CharmType.ABILITY,
                readDummyCharm("/assets/mma/zenith_charm_dummy_ability.snbt"),
                CharmType.TREE,
                readDummyCharm("/assets/mma/zenith_charm_dummy_tree.snbt"),
                CharmType.WILDCARD,
                readDummyCharm("/assets/mma/zenith_charm_dummy_wildcard.snbt")
        );
        AutoConfig.getGuiRegistry(MMAConfig.class)
                .registerAnnotationProvider(
                        (name, field, obj, defaultObj, reg) -> {
                            Set<CharmEffectType> config;
                            try {
                                config = (Set<CharmEffectType>) field.get(obj);
                            } catch (IllegalAccessException var8) {
                                throw new RuntimeException(var8);
                            }

                            ConfigEntryBuilder builder = ConfigEntryBuilder.create();
                            List<AbstractConfigListEntry> res = buildClassifying(
                                    CharmDataRegistries.getMain().charmEffectType.getMap().values().stream(),
                                    charmEffectType -> charmEffectType.ability.tree,
                                    zenithClass -> Component.literal(zenithClass.displayName),
                                    charmEffectTypes -> buildClassifying(
                                            charmEffectTypes,
                                            charmEffectType -> charmEffectType.ability,
                                            zenithAbility -> Component.literal(zenithAbility.name),
                                            entries -> entries.map(x -> buildEntryToggle(x, builder, config)),
                                            builder
                                    ),
                                    builder
                            )
                                    .toList();
                            return List.of(builder.startSubCategory(Component.translatable(name), res).setExpanded(false).build());
                        },
                        new Class[]{ZenithAbilitySelection.class}
                );
        AutoConfig.getGuiRegistry(MMAConfig.class)
                .registerAnnotationTransformer(
                        (l, name, field, obj, defaultObj, reg) -> {
                            Zenith config = (Zenith) obj;
                            ConfigEntryBuilder builder = ConfigEntryBuilder.create();
                            DisplayCharmExamples annotation = field.getAnnotation(DisplayCharmExamples.class);
                            List<AbstractConfigListEntry> list = IntStream.range(0, annotation.value().length)
                                    .mapToObj(
                                            i -> {
                                                MutableComponent title = Component.translatable(name + ".displayCharmExample." + i);
                                                return builder.startTextDescription(title)
                                                        .setTooltipSupplier(
                                                                () -> {
                                                                    ArrayList<Component> res = new ArrayList<>();
                                                                    Charm dummy = charmDummies.get(annotation.value()[i].value());
                                                                    CharmLoreRenderer renderer = (CharmLoreRenderer) (config.peliCompatibilityMode
                                                                            ? new CharmLorePeliRenderer(config)
                                                                            : new CharmLoreTabularRenderer(config, Minecraft.getInstance().fontFilterFishy));
                                                                    dummy.render(config, renderer, res, InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340));
                                                                    return Optional.of(res.toArray(Component[]::new));
                                                                }
                                                        )
                                                        .build();
                                            }
                                    )
                                    .collect(Collectors.toList());
                            list.addAll(l);
                            return list;
                        },
                        new Class[]{DisplayCharmExamples.class}
                );
    }
}
