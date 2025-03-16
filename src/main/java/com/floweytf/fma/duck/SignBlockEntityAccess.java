package com.floweytf.fma.duck;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

public interface SignBlockEntityAccess {
    default void fma$updateCaches(Minecraft minecraft, Font font) {
        throw new AbstractMethodError();
    }

    default int[] fma$getCachedFrontTextWidth() {
        throw new AbstractMethodError();
    }

    default int[] fma$getCachedBackTextWidth() {
        throw new AbstractMethodError();
    }
}
