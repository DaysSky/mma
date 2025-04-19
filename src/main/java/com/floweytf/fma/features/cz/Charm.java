package com.floweytf.fma.features.cz;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.features.cz.data.CharmEffectRarity;
import com.floweytf.fma.features.cz.data.CharmRarity;
import com.floweytf.fma.features.cz.data.ZenithAbility;
import com.floweytf.fma.features.cz.data.ZenithClass;
import com.floweytf.fma.util.Util;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import static com.floweytf.fma.util.FormatUtil.fmtDouble;
import static com.floweytf.fma.util.FormatUtil.join;
import static com.floweytf.fma.util.FormatUtil.joiner;
import static com.floweytf.fma.util.FormatUtil.literal;
import static com.floweytf.fma.util.FormatUtil.twoDecimal;
import static com.floweytf.fma.util.Util.colorRange;
import static net.minecraft.network.chat.Component.empty;

public final class Charm {
    public static final int[] BUDGET_BY_CP = {2, 4, 7, 11, 16};

    private final int charmPower;
    private final long uuid; // It's not a real UUID, but that's what monumenta calls it
    private final CharmRarity rarity;
    private final List<CharmEffectInstance> effects;
    private final List<ZenithClass> classes;
    private final List<ZenithAbility> abilities;
    @Nullable
    private final List<CharmEffectInstance> upgradedEffects;
    private final int upgradedBudget;
    private final int upgradedMaxBudget;

    private final int budget;
    private final int maxBudget;
    private final CharmType type;
    private final boolean hasUpgraded;
    private final boolean hasWarning;

    public Charm(final int charmPower, final long uuid, final CharmRarity rarity,
                 final List<CharmEffectInstance> effects,
                 final int budget, final int maxBudget, final int typeId, final boolean hasUpgraded,
                 final List<String> monumentaLore) {
        this.charmPower = charmPower;
        this.uuid = uuid;
        this.rarity = rarity;
        this.effects = new ArrayList<>(effects);
        this.hasUpgraded = hasUpgraded;

        classes = effects.stream()
            .map(x -> x.effect().ability.zenithClass)
            .collect(Collectors.toSet())
            .stream()
            .sorted(Comparator.comparingInt(Enum::ordinal))
            .toList();

        abilities = effects.stream()
            .map(x -> x.effect().ability)
            .collect(Collectors.toSet())
            .stream()
            .sorted(Comparator.comparingInt(Enum::ordinal))
            .toList();

        type = CharmType.byId(typeId);

        this.maxBudget = maxBudget;
        this.budget = budget;
        hasWarning = selfTest(monumentaLore);

        if (canUpgrade()) {
            final var upgradedCharmRarity = rarity.upgrade();
            final var newBudget = computeBudget(upgradedCharmRarity, charmPower, type);

            // NOTE: this relies on strict order of effects
            final var result = new ArrayList<>(effects);

            // Upgrade the first one - this has a guaranteed rarity
            Util.with(result, 0, firstEffect -> firstEffect.withRarity(CharmEffectRarity::upgrade));

            var remainingBudget = newBudget - budget;

            // upgrade each charm
            var hasUpgrade = false;
            do {
                hasUpgrade = false;

                for (int i = 1; i < effects.size(); i++) {
                    var effect = result.get(i);

                    if (effect.canUpgrade(upgradedCharmRarity, remainingBudget)) {
                        // perform the upgrade
                        remainingBudget += effect.getEffectRarity().upgradeDelta();
                        Util.with(result, i, e -> e.withRarity(CharmEffectRarity::upgrade));
                        hasUpgrade = true;
                    }
                }
            } while (hasUpgrade);

            // currBudget is the new budget, need to store this somewhere
            upgradedEffects = result;
            upgradedBudget = newBudget - remainingBudget;
            upgradedMaxBudget = newBudget;
        } else {
            upgradedEffects = null;
            upgradedBudget = -1;
            upgradedMaxBudget = -1;
        }

        if (!FMAClient.config().zenith.nbtOrder) {
            this.effects.sort(Comparator.comparingInt(x -> x.effect().ordinal()));
            if (upgradedEffects != null) {
                upgradedEffects.sort(Comparator.comparingInt(x -> x.effect().ordinal()));
            }
        }
    }

    private static int computeBudget(CharmRarity rarity, int cp, CharmType type) {
        return (int) (BUDGET_BY_CP[cp - 1] * rarity.budgetMultiplier * type.factor());
    }

    private boolean selfTest(List<String> rawLore) {
        var hasWarning = false;

        // We try to guess the max budget, to check
        final var computedBudget = computeBudget(rarity, charmPower, type);
        if (computedBudget != maxBudget) {
            hasWarning = true;
            FMAClient.LOGGER.error("Budget algo is likely bugged: flowey={}, monumenta={}", computedBudget, maxBudget);
        }

        for (final var effect : effects) {
            final var text = effect.monumentaText();
            if (!rawLore.contains(text)) {
                hasWarning = true;
                FMAClient.LOGGER.error("not found: {}", text);
            }
        }
        return hasWarning;
    }

    private boolean canUpgrade() {
        return !hasUpgraded && rarity != CharmRarity.LEGENDARY;
    }

    public void buildLore(Font font, List<Component> target, boolean showUpgrade) {
        target.add(join(
            literal("Architect's Ring : ", ChatFormatting.DARK_GRAY),
            literal("Zenith Charm", 0xFF9CF0)
        ));

        final var rarityText = hasUpgraded ? join(
            rarity.coloredText,
            literal(" "),
            literal("(❃)", 0x9374FF)
        ) : rarity.coloredText;

        final var budgetVal = (double) budget / maxBudget;

        target.add(join(
            literal("Charm Power : ", ChatFormatting.DARK_GRAY),
            literal("★".repeat(charmPower), style -> style.withColor(0xFFFA75)),
            literal(" - ", ChatFormatting.DARK_GRAY),
            rarityText
        ));

        final var mainLine = joiner().add(
            literal("Budget : ", ChatFormatting.DARK_GRAY),
            literal(budget + "/" + maxBudget, ChatFormatting.GRAY),
            literal(" [", ChatFormatting.GRAY),
            literal(fmtDouble(twoDecimal(budgetVal * 100), false) + "%", colorRange(budgetVal)),
            literal("]", ChatFormatting.GRAY)
        );

        if (showUpgrade && canUpgrade()) {
            final var upgradedBudgetVal = (double) upgradedBudget / upgradedMaxBudget;

            mainLine.add(
                literal(" -> ", ChatFormatting.GRAY),
                literal(upgradedBudget + "/" + upgradedMaxBudget, ChatFormatting.GRAY),
                literal(" [", ChatFormatting.GRAY),
                literal(fmtDouble(twoDecimal(upgradedBudgetVal * 100), false) + "%", colorRange(upgradedBudgetVal)),
                literal("]", ChatFormatting.GRAY)
            );
        }

        target.add(mainLine.build());

        if (FMAClient.config().zenith.displayUUID) {
            target.add(join(
                literal("UUID : ", ChatFormatting.DARK_GRAY),
                literal(Long.toHexString(uuid).toUpperCase(), ChatFormatting.GRAY)
            ));
        }

        if (classes.isEmpty() || abilities.isEmpty()) {
            throw new IllegalStateException("wtf? empty charm?");
        }

        target.add(switch (type) {
            case ABILITY -> join(
                literal("Ability", ChatFormatting.GRAY),
                literal(" - ", ChatFormatting.DARK_GRAY),
                abilities.get(0).coloredName,
                literal(" (", ChatFormatting.GRAY),
                classes.get(0).coloredName,
                literal(")", ChatFormatting.GRAY)
            );
            case TREE -> join(
                literal("Treelocked", ChatFormatting.GRAY),
                literal(" - ", ChatFormatting.DARK_GRAY),
                classes.get(0).coloredName
            );
            case WILDCARD -> {
                var classText = empty();

                for (int i = 0; i < classes.size() - 1; i++) {
                    classText = classText.append(classes.get(i).coloredShortName)
                        .append(literal(",", ChatFormatting.DARK_GRAY));
                }

                classText.append(classes.get(classes.size() - 1).coloredShortName);

                yield join(
                    literal("Wildcard", ChatFormatting.GRAY),
                    literal(" - ", ChatFormatting.DARK_GRAY),
                    classText
                );
            }
        });

        target.add(empty());
        target.addAll(CharmEffectInstance.format(font, effects, showUpgrade ? upgradedEffects : null,
            type != CharmType.ABILITY));

        if (canUpgrade()) {
            target.add(literal("Hold shift to see upgrade", ChatFormatting.DARK_GRAY));
            target.add(literal("Press ctrl to toggle upgrade", ChatFormatting.DARK_GRAY));
        }
        if (hasWarning) {
            target.add(literal("* WARNING - possible bug, report to Flowey *", ChatFormatting.RED));
            target.add(literal("* Charm display data may be wrong *", ChatFormatting.RED));
        }
    }
}
