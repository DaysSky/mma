package com.dayssky.mma.features.cz.data;

import com.dayssky.mma.util.FormatUtil;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.UnaryOperator;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public enum ZenithTree {
    DAWNBRINGER("Dawnbringer", 15774502),
    EARTHBOUND("Earthbound", 7028013),
    FLAMECALLER("Flamecaller", 15748641),
    FROSTBORN("Frostborn", 10734561),
    STEELSAGE("Steelsage", 9605778),
    SHADOWDANCER("Shadowdancer", 7948463),
    WINDWALKER("Windwalker", 12639913);

    public final String displayName;
    public final int color;
    public final Component coloredName;
    public final Component coloredShortName;

    private ZenithTree(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
        this.coloredName = FormatUtil.literal(displayName, (UnaryOperator<Style>) (s -> s.withColor(color)));
        this.coloredShortName = FormatUtil.literal(displayName.substring(0, 2), s -> s.withColor(color));
    }

    public static Optional<ZenithTree> byName(String name) {
        return Arrays.stream(values()).filter(x -> x.displayName.equals(name)).findFirst();
    }
}
