package com.dayssky.mma.features.cz;

import com.dayssky.mma.MMAConfig.Zenith;
import com.dayssky.mma.util.FormatUtil;
import com.dayssky.mma.util.FormatUtil.ComponentJoiner;

import java.util.List;
import java.util.stream.IntStream;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public class CharmLorePeliRenderer implements CharmLoreRenderer {
    private final Zenith config;

    public CharmLorePeliRenderer(Zenith config) {
        this.config = config;
    }

    @Override
    public List<? extends Component> render(List<CharmEffect> effects, @Nullable List<CharmEffect> upgradedEffects, boolean includeAbility) {
        return IntStream.range(0, effects.size())
                .mapToObj(
                        i -> {
                            CharmEffect effect = effects.get(i);
                            CharmEffect upgradedEffect = upgradedEffects == null ? null : upgradedEffects.get(i);
                            ComponentJoiner builder = FormatUtil.joiner();
                            if (upgradedEffects == null || effect.equals(upgradedEffect)) {
                                builder.add(new Component[]{effect.modText()});
                            } else if (this.config.compactUpgrade) {
                                builder.add(new Component[]{upgradedEffect.modText()});
                            } else {
                                builder.add(new Component[]{effect.modText(), FormatUtil.literal(" -> ", ChatFormatting.GRAY), upgradedEffect.modText()});
                            }

                            MutableComponent component = builder.add(
                                            new Component[]{
                                                    FormatUtil.literal(" "),
                                                    effect.effect().ability.coloredName.copy().withStyle(effect.modText().getStyle()),
                                                    FormatUtil.literal(" "),
                                                    effect.effectText(),
                                                    FormatUtil.literal(" [", ChatFormatting.GRAY),
                                                    effect.rollText(),
                                                    FormatUtil.literal("]", ChatFormatting.GRAY)
                                            }
                                    )
                                    .build();
                            if (this.config.ignoredAbilities.contains(effect.effect)) {
                                component.withStyle(ChatFormatting.STRIKETHROUGH);
                            }

                            return component;
                        }
                )
                .toList();
    }
}
