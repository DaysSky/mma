package com.floweytf.fma.features.cz.data;

import java.util.Arrays;

public enum CharmEffectRarity {
    N_LEGENDARY("Negative Legendary", CharmRarity.LEGENDARY, 15, true, 0xff5555),
    N_EPIC("Negative Epic", CharmRarity.EPIC, 13, true, 0xff5555),
    N_RARE("Negative Rare", CharmRarity.RARE, 10, true, 0xff5555),
    N_UNCOMMON("Negative Uncommon", CharmRarity.UNCOMMON, 7, true, 0xff5555),
    N_COMMON("Negative Common", CharmRarity.COMMON, 4, true, 0xff5555),
    COMMON("Common", CharmRarity.COMMON, -5, false),
    UNCOMMON("Uncommon", CharmRarity.UNCOMMON, -8, false),
    RARE("Rare", CharmRarity.RARE, -11, false),
    EPIC("Epic", CharmRarity.EPIC, -14, false),
    LEGENDARY("Legendary", CharmRarity.LEGENDARY, -16, false);

    public final String name;
    public final CharmRarity charmRarity;
    public final int budget;
    public final boolean isNegative;
    public final int color;

    CharmEffectRarity(String name, CharmRarity charmRarity, int budget, boolean isNegative) {
        this(name, charmRarity, budget, isNegative, charmRarity.color);
    }

    CharmEffectRarity(String name, CharmRarity charmRarity, int budget, boolean isNegative, int color) {
        this.name = name;
        this.charmRarity = charmRarity;
        this.budget = budget;
        this.isNegative = isNegative;
        this.color = color;
    }

    public static CharmEffectRarity byName(String actionName) {
        return Arrays.stream(values())
            .filter(x -> actionName.equalsIgnoreCase(x.name))
            .findFirst()
            .orElseThrow();
    }

    public static CharmEffectRarity byCharmRarity(CharmRarity rarity) {
        return switch (rarity) {
            case COMMON -> COMMON;
            case UNCOMMON -> UNCOMMON;
            case RARE -> RARE;
            case EPIC -> EPIC;
            case LEGENDARY -> LEGENDARY;
        };
    }

    public CharmEffectRarity upgrade() {
        if (this == LEGENDARY) {
            throw new UnsupportedOperationException("cannot upgrade legendary");
        }

        return values()[ordinal() + 1];
    }

    public int upgradeDelta() {
        return upgrade().budget - budget;
    }

    public String getShorthand() {
        return (isNegative ? "-" : "") + charmRarity.displayName;
    }

    public boolean canUpgrade(CharmRarity charmRarity, int remainingBudget) {
        return this != CharmEffectRarity.LEGENDARY && // we can't upgrade legendary
            !upgrade().charmRarity.isGreater(charmRarity) && // we can't upgrade a stat past the charm's rarity
            remainingBudget + upgradeDelta() >= 0; // we must have enough budget...

    }
}
