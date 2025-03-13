package com.floweytf.fma.features.cz.data;

import net.minecraft.network.chat.Component;

import static com.floweytf.fma.util.FormatUtil.literal;

public enum ZenithClass {
    DAWNBRINGER("Dawnbringer", 0xf0b326),
    EARTHBOUND("Earthbound", 0x6b3d2d),
    FLAMECALLER("Flamecaller", 0xf04e21),
    FROSTBORN("Frostborn", 0xa3cbe1),
    STEELSAGE("Steelsage", 0x929292),
    SHADOWDANCER("Shadowdancer", 0x7948af),
    WINDWALKER("Windwalker", 0xc0dea9);

    public final String displayName;
    public final int color;
    public final Component coloredName;
    public final Component coloredShortName;

    ZenithClass(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
        coloredName = literal(displayName, s -> s.withColor(color));
        coloredShortName = literal(displayName.substring(0, 2), s -> s.withColor(color));
    }
}
