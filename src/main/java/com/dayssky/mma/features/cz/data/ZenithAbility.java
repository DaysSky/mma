package com.dayssky.mma.features.cz.data;

import com.dayssky.mma.util.FormatUtil;
import net.minecraft.network.chat.Component;

public class ZenithAbility {
    public final String name;
    public final ZenithTree tree;
    public final Component coloredName;

    public ZenithAbility(String name, ZenithTree tree) {
        this.name = name;
        this.tree = tree;
        this.coloredName = FormatUtil.literal(name, tree.color);
    }
}
