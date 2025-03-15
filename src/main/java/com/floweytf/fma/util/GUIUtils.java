package com.floweytf.fma.util;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class GUIUtils {
    public static String getClassDisplayInfo(Container container) {
        ItemStack itemStack = getItem(container, 6, 3);
        final List<String> plainLore = NBTUtil.access(itemStack).getPlainLore();
        return String.join("\n", plainLore);
    }

    public static ItemStack getItem(Container container, int row, int column) {
        return container.getItem((row - 1) * 9 + column - 1);
    }
}
