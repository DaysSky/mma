package com.floweytf.fma;

import com.floweytf.fma.features.Waypoint;
import com.floweytf.fma.features.cz.data.CharmEffectType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

@Config(name = "fma")
public class FMAConfig implements ConfigData {
    public @interface Hidden {
    }

    public static class InventoryOverlayToggles {
        public boolean enable = true;
        public boolean enableRarity = false;
        public boolean enableCZCharmRarity = false;
        public boolean enableCooldown = true;
        public boolean enableCZCharmPower = false;
        public boolean enablePICount = true;
        public boolean enableLoomFirmCount = false;
        @ConfigEntry.BoundedDiscrete(min = 0, max = 20)
        public int updateDelayTicks = 5;
    }

    public static class SidebarToggles {
        public boolean enable = true;
        public boolean enableProxy = true;
        public boolean enableShard = true;
        public boolean enableIp = true;
        public boolean enableIpElision = true;
        public boolean situationals = true;
    }

    public static class FeatureToggles {
        public boolean enableHpIndicators = true;
        public boolean enableTimerAndStats = true;
        public boolean enableVanillaEffectInUMMHud = false;
        public boolean enableVanityDurability = true;
        public boolean enableCustomSplash = true;
        @ConfigEntry.Gui.CollapsibleObject
        public InventoryOverlayToggles inventoryOverlay = new InventoryOverlayToggles();
        @ConfigEntry.Gui.CollapsibleObject
        public SidebarToggles sidebarToggles = new SidebarToggles();
        public boolean enableDebug = SharedConstants.IS_RUNNING_IN_IDE;
        public boolean suppressDebugWarning = !SharedConstants.IS_RUNNING_IN_IDE;
        public boolean versionCheck = false;
        public boolean versionCheckIncludeBeta = false;
        public boolean contractCheck = true;
        public int contractThreshold = 40;
        @ConfigEntry.Gui.CollapsibleObject
        public Waypoint.Config waypoint = new Waypoint.Config();
    }

    public static class Appearance {
        @ConfigEntry.ColorPicker
        public int bracketColor = 0xb7bdf8;
        @ConfigEntry.ColorPicker
        public int tagColor = 0xc6a0f6;

        public String tagText = "MAID";

        @ConfigEntry.ColorPicker
        public int textColor = 0xf4dbd6;
        @ConfigEntry.ColorPicker
        public int numericColor = 0xf38ba8;
        @ConfigEntry.ColorPicker
        public int detailColor = 0x6c6f85;
        @ConfigEntry.ColorPicker
        public int playerNameColor = 0xef9f76;
        @ConfigEntry.ColorPicker
        public int altTextColor = 0xb4befe;

        @ConfigEntry.ColorPicker
        public int errorColor = 0xe64553;
        @ConfigEntry.ColorPicker
        public int warningColor = 0xdf8e1d;
    }

    public static class HpIndicator {
        public boolean enableGlowingPlayer = true;
        public boolean enableHitboxColoring = true;
        public boolean countAbsorptionAsHp = true;

        public boolean smoothColor = false;

        @ConfigEntry.BoundedDiscrete(max = 100)
        public int goodHpPercent = 70;
        @ConfigEntry.BoundedDiscrete(max = 100)
        public int mediumHpPercent = 50;
        @ConfigEntry.BoundedDiscrete(max = 100)
        public int lowHpPercent = 25;

        @ConfigEntry.ColorPicker
        public int goodHpColor = 0x33ef2f;
        @ConfigEntry.ColorPicker
        public int mediumHpColor = 0xd9ef2f;
        @ConfigEntry.ColorPicker
        public int lowHpColor = 0xefa22f;
        @ConfigEntry.ColorPicker
        public int criticalHpColor = 0xef472d;
    }

    public static class Zenith {
        @ConfigEntry.Gui.PrefixText
        public boolean enableCustomCharmInfo = true;
        @ConfigEntry.Gui.Tooltip
        public boolean disableMonumentaLore = true; // This feature is beta
        @ConfigEntry.Gui.Tooltip
        public boolean peliCompatibilityMode = false;
        @ConfigEntry.Gui.Tooltip
        public boolean enableStatBreakdown = false;
        @ConfigEntry.Gui.Tooltip
        public boolean displayRollValue = true;
        @ConfigEntry.Gui.Tooltip
        public boolean displayEffectRarity = false;
        @ConfigEntry.Gui.Tooltip
        public boolean displayUUID = false;
        public boolean nbtOrder = false;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public EnumMap<CharmEffectType, Boolean> ignoredAbilities = new EnumMap<>(CharmEffectType.class);
    }

    public static class Portal {
        public boolean enablePortalButtonIndicator = true;

        public boolean nodeSplit = true;
        public boolean soulsSplit = true;
        public boolean startBossSplit = true;
        public boolean phase1Split = false;
        public boolean phase2Split = false;
        public boolean phase3Split = false;
        public boolean bossSplit = true;
        public boolean enableIotaFix = true;
    }

    public static class Ruin {
        public boolean soulsSplit = true;
        public boolean startBossSplit = true;
        public boolean daggerSplit = false;
        public boolean dpsSplit = false;
        public boolean bossSplit = true;
    }

    public static class Chat {
        public String meowingChannel = "wc";
        public String meowingText = "meow";
    }

    @ConfigEntry.Category("features")
    @ConfigEntry.Gui.TransitiveObject
    public FeatureToggles features = new FeatureToggles();

    @ConfigEntry.Category("appearance")
    @ConfigEntry.Gui.TransitiveObject
    public Appearance appearance = new Appearance();

    @ConfigEntry.Category("hpIndicator")
    @ConfigEntry.Gui.TransitiveObject
    public HpIndicator hpIndicator = new HpIndicator();

    @ConfigEntry.Category("chat")
    @ConfigEntry.Gui.TransitiveObject
    public Chat chat = new Chat();

    @ConfigEntry.Category("strikes")
    @ConfigEntry.Gui.CollapsibleObject
    public Portal portal = new Portal();

    @ConfigEntry.Category("strikes")
    @ConfigEntry.Gui.CollapsibleObject
    public Ruin ruin = new Ruin();

    @ConfigEntry.Category("zenith")
    @ConfigEntry.Gui.TransitiveObject
    public Zenith zenith = new Zenith();

    @SuppressWarnings("rawtypes")
    private static AbstractConfigListEntry buildEntryToggle(CharmEffectType x, ConfigEntryBuilder builder,
                                                            EnumMap<CharmEffectType, Boolean> config) {
        return builder.startBooleanToggle(Component.literal(x.modifier), config.getOrDefault(x, false))
            .setSaveConsumer(b -> config.put(x, b))
            .setDefaultValue(false)
            .build();
    }

    @SuppressWarnings("rawtypes")
    private static <T, U> Stream<AbstractConfigListEntry> buildClassifying(
        Stream<T> entries, Function<T, U> classifier,
        Function<U, Component> nameGetter,
        Function<Stream<T>, Stream<AbstractConfigListEntry>> entryBuilder,
        ConfigEntryBuilder builder
    ) {
        return entries.collect(Collectors.groupingBy(classifier))
            .entrySet()
            .stream()
            .map(x -> builder.startSubCategory(nameGetter.apply(x.getKey()),
                entryBuilder.apply(x.getValue().stream()).toList()).build());
    }

    @SuppressWarnings("unchecked")
    public static ConfigHolder<FMAConfig> register() {
        final var holder = AutoConfig.register(FMAConfig.class, GsonConfigSerializer::new);

        AutoConfig.getGuiRegistry(FMAConfig.class).registerTypeProvider(
            (s, field, o, o1, guiRegistryAccess) -> {
                final EnumMap<CharmEffectType, Boolean> config;

                try {
                    config = (EnumMap<CharmEffectType, Boolean>) field.get(o);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                final var builder = ConfigEntryBuilder.create();

                final var res = buildClassifying(
                    Arrays.stream(CharmEffectType.values()),
                    charmEffectType -> charmEffectType.ability.zenithClass,
                    zenithClass -> Component.literal(zenithClass.displayName),
                    charmEffectTypes -> buildClassifying(
                        charmEffectTypes,
                        charmEffectType -> charmEffectType.ability,
                        zenithAbility -> Component.literal(zenithAbility.displayName),
                        entries -> entries.map(x -> buildEntryToggle(x, builder, config)),
                        builder
                    ),
                    builder
                ).toList();

                return List.of(builder.startSubCategory(Component.translatable(s), res).setExpanded(false).build());
            },
            EnumMap.class
        );

        AutoConfig.getGuiRegistry(FMAConfig.class).registerAnnotationProvider(
            (s, f, o, o1, a) -> List.of(),
            Hidden.class
        );

        holder.registerSaveListener((configHolder, config) -> {
            config.validatePostLoad();
            FMAClient.reload();
            return InteractionResult.PASS;
        });

        return holder;
    }

    @Override
    public void validatePostLoad() {
        if (hpIndicator.mediumHpPercent > hpIndicator.goodHpPercent)
            hpIndicator.mediumHpPercent = hpIndicator.goodHpPercent;

        if (hpIndicator.lowHpPercent > hpIndicator.mediumHpPercent)
            hpIndicator.lowHpPercent = hpIndicator.mediumHpPercent;
    }
}
