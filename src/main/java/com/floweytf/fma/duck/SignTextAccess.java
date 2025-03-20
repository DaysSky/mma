package com.floweytf.fma.duck;

import net.minecraft.client.gui.Font;

public interface SignTextAccess {
    default int fma$getWidth(Font font, int index) {
        throw new AbstractMethodError();
    }
}
