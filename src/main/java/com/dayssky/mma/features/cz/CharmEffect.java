package com.dayssky.mma.features.cz;

import com.dayssky.mma.features.cz.data.CharmEffectRarity;
import com.dayssky.mma.features.cz.data.CharmEffectType;
import com.dayssky.mma.features.cz.data.CharmRarity;
import com.dayssky.mma.util.FormatUtil;
import com.dayssky.mma.util.Util;

import java.util.function.UnaryOperator;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class CharmEffect {
    public final double rollValue;
    public final CharmEffectType effect;
    public final CharmEffectRarity effectRarity;
    public final double baseValue;
    public final double delta;
    public final double value;
    public final double displayRollValue;
    public final Style rarityColor;
    public final Style rollColor;

    public CharmEffect(double rollValue, CharmEffectType effect, CharmEffectRarity rarity) {
        this.rollValue = rollValue;
        this.effect = effect;
        this.effectRarity = rarity;
        double rawBaseValue = effect.rarityValue(rarity.charmRarity);
        double baseValue = rawBaseValue;
        double delta = effect.variance * (2.0 * rollValue - 1.0);
        double value = rawBaseValue;
        if (effect.variance != 0.0) {
            value = rawBaseValue + delta;
            if (rawBaseValue >= 5.0) {
                value = Math.round(value);
            } else {
                value = FormatUtil.twoDecimal(value);
            }
        }

        if (rarity.isNegative) {
            value = -value;
            baseValue = -rawBaseValue;
            delta = -delta;
        }

        this.rarityColor = Style.EMPTY.withColor(rarity.color);
        this.displayRollValue = rawBaseValue < 0.0 == rarity.isNegative ? rollValue : 1.0 - rollValue;
        this.rollColor = Style.EMPTY.withColor(Util.colorRange((float) this.displayRollValue));
        this.baseValue = baseValue;
        this.delta = delta;
        this.value = value;
    }

    public CharmEffect withRarity(UnaryOperator<CharmEffectRarity> rarityMod) {
        return new CharmEffect(this.rollValue, this.effect, rarityMod.apply(this.effectRarity));
    }

    public CharmEffectRarity getEffectRarity() {
        return this.effectRarity;
    }

    public CharmEffectType effect() {
        return this.effect;
    }

    public boolean canUpgrade(CharmRarity charmRarity, int remainingBudget) {
        return this.effectRarity.canUpgrade(charmRarity, remainingBudget)
                && this.effectRarity.compareTo(this.effect.maxRarity) < 0
                && this.effect.rarityValue(this.effectRarity.upgrade().charmRarity) != 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            CharmEffect that = (CharmEffect) o;
            return Double.compare(this.rollValue, that.rollValue) == 0 && this.effect == that.effect && this.effectRarity == that.effectRarity;
        } else {
            return false;
        }
    }

    String unit() {
        return this.effect.isPercent ? "%" : "";
    }

    String fmt(double value) {
        return FormatUtil.fmtDouble(value) + this.unit();
    }

    Component effectText() {
        return FormatUtil.literal(this.effect.modifier, this.rarityColor);
    }

    Component modText() {
        return FormatUtil.literal(this.fmt(this.value), this.rarityColor);
    }

    public String monumentaText() {
        return this.fmt(this.value) + " " + this.effect().ability.name + " " + this.effect.modifier;
    }

    Component modDetailText() {
        return FormatUtil.join(
                FormatUtil.literal(this.fmt(this.baseValue), this.rarityColor),
                this.effect.variance == 0.0
                        ? FormatUtil.literal("+0" + this.unit(), ChatFormatting.DARK_GRAY)
                        : FormatUtil.literal(FormatUtil.fmtDoubleDelta(FormatUtil.twoDecimal(this.delta)) + this.unit(), this.rollColor)
        );
    }

    Component rarityText() {
        return FormatUtil.literal(this.effectRarity.getShorthand(), this.rarityColor);
    }

    Component rollText() {
        return FormatUtil.literal(FormatUtil.fmtDouble(FormatUtil.twoDecimal(this.displayRollValue * 100.0), false) + "%", this.rollColor);
    }
}
