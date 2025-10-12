package com.floweytf.fma.features.cz.data;

public class CharmEffectType {
    public final int ordinal;
    public final String name;
    public final String modifier;
    public final ZenithAbility ability;
    public final boolean isOnlyPositive;
    public final double variance;
    public final boolean isPercent;
    public final double effectCap;
    public final CharmEffectRarity maxRarity;
    private final double[] rarityValues;

    CharmEffectType(
            int ordinal,
            String effectName,
            ZenithAbility ability,
            boolean isOnlyPositive,
            boolean isPercent,
            double variance,
            double effectCap,
            CharmEffectRarity maxRarity,
            double... rarityValues
    ) {
        this.ordinal = ordinal;
        this.modifier = effectName;
        this.name = ability.name + " " + this.modifier;
        this.ability = ability;
        this.isOnlyPositive = isOnlyPositive;
        this.variance = variance;
        this.maxRarity = maxRarity;
        this.rarityValues = rarityValues;
        this.isPercent = isPercent;
        this.effectCap = effectCap;
    }

    public double rarityValue(CharmRarity rarity) {
        return this.rarityValues[rarity.ordinal()];
    }
}
