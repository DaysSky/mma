package com.floweytf.fma.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class HoverControlHandler {
    private ItemStack stackInstance = null;
    private boolean wasControlDown = false;
    private boolean shouldDisplayAdvanced = false;

    private void update(ItemStack stack) {
        if (this.stackInstance != stack) {
            this.stackInstance = stack;
            this.shouldDisplayAdvanced = false;
        }

        boolean isControlDown = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 341);
        if (this.wasControlDown != isControlDown && isControlDown) {
            this.shouldDisplayAdvanced = !this.shouldDisplayAdvanced;
        }

        this.wasControlDown = isControlDown;
    }

    public boolean isEnabled(ItemStack stack) {
        this.update(stack);
        return this.shouldDisplayAdvanced || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340);
    }
}
