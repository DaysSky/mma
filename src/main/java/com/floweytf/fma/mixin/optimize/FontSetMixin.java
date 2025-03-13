package com.floweytf.fma.mixin.optimize;

import com.floweytf.fma.FMAClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.font.FontSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FontSet.class)
public class FontSetMixin {
    @ModifyExpressionValue(
        method = "getRandomGlyph",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/ints/IntList;size()I",
            remap = false
        )
    )
    private int capSize(int original) {
        if (FMAClient.features().performance.obfTextCharCap <= 64) {
            return original;
        }

        return Math.min(FMAClient.features().performance.obfTextCharCap, original);
    }
}
