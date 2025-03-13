package com.floweytf.fma.duck;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

public interface SignBlockEntityAccess {
    void fma$updateCaches(Minecraft minecraft, Font font);

    int[] fma$getCachedFrontTextWidth();

    int[] fma$getCachedBackTextWidth();
}
