package com.floweytf.fma.features.cz.data;

import net.minecraft.network.chat.Component;

import static com.floweytf.fma.util.FormatUtil.literal;

public enum CharmRarity {
    COMMON("Common", 0x9f929c, 2),
    UNCOMMON("Uncommon", 0x70bc6d, 3),
    RARE("Rare", 0x705eca, 4),
    EPIC("Epic", 0xcd5eca, 5),
    LEGENDARY("Legendary", 0xe49b20, 6);

    public final String displayName;
    public final int color;
    public final Component coloredText;
    public final int budgetMultiplier;

    CharmRarity(String displayName, int color, int budgetMultiplier) {
        this.displayName = displayName;
        this.color = color;
        this.budgetMultiplier = budgetMultiplier;
        coloredText = literal(displayName, color);
    }

    public CharmRarity upgrade() {
        if (this == LEGENDARY) {
            throw new UnsupportedOperationException("Can't upgrade legendary");
        }

        return values()[ordinal() + 1];
    }

    public boolean isGreater(CharmRarity other) {
        return ordinal() > other.ordinal();
    }
}
