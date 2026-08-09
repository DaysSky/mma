package com.dayssky.mma.features.cz;

import com.dayssky.mma.MMAConfig.Zenith;
import com.dayssky.mma.util.FormatUtil;
import com.google.common.collect.Streams;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public class CharmLoreTabularRenderer implements CharmLoreRenderer {
    private final Zenith config;
    private final Font font;

    public CharmLoreTabularRenderer(Zenith config, Font font) {
        this.config = config;
        this.font = font;
    }

    private List<? extends Component> getHeader(boolean includeAbility) {
        ArrayList<String> header = new ArrayList<>();
        header.add("Mod");
        if (includeAbility) {
            header.add("Ability");
        }

        header.add("Effect");
        if (this.config.enableStatBreakdown) {
            header.add("Mod Detail");
        }

        if (this.config.displayEffectRarity) {
            header.add("Rarity");
        }

        if (this.config.displayRollValue) {
            header.add("Roll");
        }

        return header.stream().map(x -> FormatUtil.literal(x, ChatFormatting.GRAY, ChatFormatting.UNDERLINE)).toList();
    }

    private Component makeEntry(CharmEffect self, @Nullable CharmEffect upgrade, Function<CharmEffect, Component> get) {
        if (upgrade == null) {
            return get.apply(self);
        } else {
            return (Component) (this.config.compactUpgrade
                    ? get.apply(upgrade)
                    : FormatUtil.join(get.apply(self), FormatUtil.literal(" -> ", ChatFormatting.GRAY), get.apply(upgrade)));
        }
    }

    private List<Component> fmtEntry(boolean includeAbility, CharmEffect self, @Nullable CharmEffect upgrade) {
        ArrayList<Component> parts = new ArrayList<>();
        if (self.equals(upgrade)) {
            upgrade = null;
        }

        parts.add(this.makeEntry(self, upgrade, CharmEffect::modText));
        if (includeAbility) {
            parts.add(self.effect().ability.coloredName);
        }

        parts.add(self.effectText());
        if (this.config.enableStatBreakdown) {
            parts.add(this.makeEntry(self, upgrade, CharmEffect::modDetailText));
        }

        if (this.config.displayEffectRarity) {
            parts.add(this.makeEntry(self, upgrade, CharmEffect::rarityText));
        }

        if (this.config.displayRollValue) {
            parts.add(self.rollText());
        }

        return parts;
    }

    @Override
    public List<? extends Component> render(List<CharmEffect> effects, @Nullable List<CharmEffect> upgradedEffects, boolean includeAbility) {
        List<MutableComponent> result;
        if (upgradedEffects == null) {
            result = FormatUtil.tabulate(
                    this.font,
                    Stream.concat(Stream.of(this.getHeader(includeAbility)), effects.stream().map(entry -> this.fmtEntry(includeAbility, entry, null))).toList()
            );
        } else {
            result = FormatUtil.tabulate(
                    this.font,
                    Stream.concat(
                                    Stream.of(this.getHeader(includeAbility)),
                                    Streams.zip(effects.stream(), upgradedEffects.stream(), (entry, upgrade) -> this.fmtEntry(includeAbility, entry, upgrade))
                            )
                            .toList()
            );
        }

        for (int i = 0; i < effects.size(); i++) {
            if (this.config.enableIgnoredAbilities && this.config.ignoredAbilities.contains(effects.get(i).effect)) {
                result.get(i + 1).withStyle(ChatFormatting.STRIKETHROUGH);
            }
        }

        return result;
    }
}
