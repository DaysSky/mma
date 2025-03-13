package com.floweytf.fma.features.cz;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig;
import com.floweytf.fma.features.cz.data.CharmEffectRarity;
import com.floweytf.fma.features.cz.data.CharmEffectType;
import com.floweytf.fma.features.cz.data.CharmRarity;
import com.floweytf.fma.util.FormatUtil;
import com.floweytf.fma.util.Util;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import static com.floweytf.fma.util.FormatUtil.fmtDouble;
import static com.floweytf.fma.util.FormatUtil.fmtDoubleDelta;
import static com.floweytf.fma.util.FormatUtil.join;
import static com.floweytf.fma.util.FormatUtil.joiner;
import static com.floweytf.fma.util.FormatUtil.literal;
import static com.floweytf.fma.util.FormatUtil.tabulate;
import static com.floweytf.fma.util.FormatUtil.twoDecimal;

public final class CharmEffectInstance {
    private final double rollValue;
    private final CharmEffectType effect;
    private final CharmEffectRarity effectRarity;

    private final double baseValue;
    private final double delta;
    private final double value;
    private final double displayRollValue;
    private final Style rarityColor;
    private final Style rollColor;

    public CharmEffectInstance(double rollValue, CharmEffectType effect, CharmEffectRarity rarity) {
        this.rollValue = rollValue;
        this.effect = effect;
        effectRarity = rarity;
        // we should compute a few other values
        final var rawBaseValue = effect.rarityValue(rarity.charmRarity);

        var baseValue = rawBaseValue;
        var delta = effect.variance * (2 * rollValue - 1);
        double value = baseValue;

        if (effect.variance != 0) {
            value = baseValue + delta;
            if (baseValue >= 5) {
                value = Math.round(value);
            } else {
                value = twoDecimal(value);
            }
        }

        if (rarity.isNegative) {
            value = -value;
            baseValue = -baseValue;
            delta = -delta;
        }

        // prep colors
        rarityColor = Style.EMPTY.withColor(rarity.color);

        // basically, if the base value is negative, we can assume that a low roll is good
        // or else we can assume a high roll is good
        // if it's negative, we negate this

        displayRollValue = (rawBaseValue < 0 == rarity.isNegative) ? rollValue :
            (1 - rollValue);

        rollColor = Style.EMPTY.withColor(Util.colorRange((float) displayRollValue));
        this.baseValue = baseValue;
        this.delta = delta;
        this.value = value;
    }

    private static List<Component> formatParts(FMAConfig.Zenith config, boolean includeAbility,
                                               CharmEffectInstance self, @Nullable CharmEffectInstance upgrade) {
        final var parts = new ArrayList<Component>();

        // Janky way to disable rendering stuff...
        if (self.equals(upgrade)) {
            upgrade = null;
        }

        parts.add(upgrade == null ? self.modText() : join(
            self.modText(),
            literal(" -> ", ChatFormatting.GRAY),
            upgrade.modText()
        ));

        if (includeAbility) {
            parts.add(self.effect.ability.coloredName);
        }

        parts.add(self.effectText());

        if (config.enableStatBreakdown) {
            parts.add(upgrade == null ? self.modDetailText() : join(
                self.modDetailText(),
                literal(" -> ", ChatFormatting.GRAY),
                upgrade.modDetailText()
            ));
        }

        if (config.displayEffectRarity) {
            parts.add(upgrade == null ? self.rarityText() : join(
                self.rarityText(),
                literal(" -> ", ChatFormatting.GRAY),
                upgrade.rarityText()
            ));
        }

        if (config.displayRollValue) {
            parts.add(self.rollText());
        }

        return parts;
    }

    private static List<Component> getHeader(FMAConfig.Zenith config, boolean includeAbility) {
        final var header = new ArrayList<Component>();
        header.add(literal("Mod", ChatFormatting.GRAY, ChatFormatting.UNDERLINE));
        if (includeAbility) {
            header.add(literal("Ability", ChatFormatting.GRAY, ChatFormatting.UNDERLINE));
        }

        header.add(literal("Effect", ChatFormatting.GRAY, ChatFormatting.UNDERLINE));

        if (config.enableStatBreakdown)
            header.add(literal("Mod Detail", ChatFormatting.GRAY, ChatFormatting.UNDERLINE));
        if (config.displayEffectRarity)
            header.add(literal("Rarity", ChatFormatting.GRAY, ChatFormatting.UNDERLINE));
        if (config.displayRollValue)
            header.add(literal("Roll", ChatFormatting.GRAY, ChatFormatting.UNDERLINE));
        return header;
    }

    private static List<Component> formatPeli(List<CharmEffectInstance> effects,
                                              @Nullable List<CharmEffectInstance> upgradedEffects,
                                              FMAConfig.Zenith config) {
        return IntStream.range(0, effects.size()).mapToObj(i -> {
            final var effect = effects.get(i);
            final var upgradedEffect = upgradedEffects == null ? null : upgradedEffects.get(i);

            final var builder = joiner();

            if (upgradedEffects == null || effect.equals(upgradedEffect)) {
                builder.add(effect.modText());
            } else {
                builder.add(effect.modText(), literal(" -> ", ChatFormatting.GRAY), upgradedEffect.modText());
            }

            return (Component) builder.add(
                literal(" "),
                effect.effect().ability.coloredName.copy().withStyle(effect.modText().getStyle()),
                literal(" "),
                effect.effectText(),
                literal(" [", ChatFormatting.GRAY),
                effect.rollText(),
                literal("]", ChatFormatting.GRAY)
            ).build().withStyle(x -> {
                if (config.ignoredAbilities.getOrDefault(effect.effect, false)) {
                    return x.withStrikethrough(true);
                }

                return x;
            });
        }).toList();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static List<MutableComponent> formatTabular(Font font, List<CharmEffectInstance> effects,
                                                        @Nullable List<CharmEffectInstance> upgradedEffects,
                                                        boolean includeAbility, FMAConfig.Zenith config) {
        List<MutableComponent> result;
        if (upgradedEffects == null) {
            result = tabulate(font, Stream.concat(
                Stream.of(getHeader(config, includeAbility)),
                effects.stream().map(entry -> formatParts(config, includeAbility, entry, null))
            ).toList());
        } else {
            result = tabulate(font, Stream.concat(
                Stream.of(getHeader(config, includeAbility)),
                Streams.zip(
                    effects.stream(),
                    upgradedEffects.stream(),
                    (entry, upgrade) -> formatParts(config, includeAbility, entry, upgrade)
                )
            ).toList());
        }

        for (int i = 0; i < effects.size(); i++) {
            int finalI = i;
            result.get(i + 1).withStyle(x -> {
                if (config.ignoredAbilities.getOrDefault(effects.get(finalI).effect, false)) {
                    return x.withStrikethrough(true);
                }

                return x;
            });
        }

        return result;
    }

    public static List<? extends Component> format(Font font, List<CharmEffectInstance> effects,
                                                   @Nullable List<CharmEffectInstance> upgradedEffects,
                                                   boolean includeAbility) {
        final var config = FMAClient.config().zenith;

        if (config.peliCompatibilityMode) {
            return formatPeli(effects, upgradedEffects, config);
        } else {
            return formatTabular(font, effects, upgradedEffects, includeAbility, config);
        }
    }

    public CharmEffectInstance withRarity(UnaryOperator<CharmEffectRarity> rarityMod) {
        return new CharmEffectInstance(rollValue, effect, rarityMod.apply(effectRarity));
    }

    public CharmEffectInstance withRarity(CharmEffectRarity rarity) {
        return new CharmEffectInstance(rollValue, effect, rarity);
    }

    public CharmEffectRarity getEffectRarity() {
        return effectRarity;
    }

    private String unit() {
        return effect.isPercent ? "%" : "";
    }

    private String fmt(double value) {
        return FormatUtil.fmtDouble(value) + unit();
    }

    private Component effectText() {
        return literal(effect.modifier, rarityColor);
    }

    private Component modText() {
        return literal(fmt(value), rarityColor);
    }

    public String monumentaText() {
        return fmt(value) + " " + effect().ability.displayName + " " + effect.modifier;
    }

    private Component modDetailText() {
        return join(
            literal(fmt(baseValue), rarityColor),
            effect.variance == 0 ?
                literal("+0" + unit(), ChatFormatting.DARK_GRAY) :
                literal(fmtDoubleDelta(twoDecimal(delta)) + unit(), rollColor)
        );
    }

    private Component rarityText() {
        return literal(effectRarity.getShorthand(), rarityColor);
    }

    private Component rollText() {
        return literal(fmtDouble(twoDecimal(displayRollValue * 100), false) + "%", rollColor);
    }

    public CharmEffectType effect() {
        return effect;
    }

    public boolean canUpgrade(CharmRarity charmRarity, int remainingBudget) {
        return effectRarity.canUpgrade(charmRarity, remainingBudget) &&
            effect.rarityValue(effectRarity.upgrade().charmRarity) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharmEffectInstance that = (CharmEffectInstance) o;
        return Double.compare(rollValue, that.rollValue) == 0 &&
            effect == that.effect &&
            effectRarity == that.effectRarity;
    }
}
