package com.floweytf.fma.mixin.optimize;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SignRenderer.class)
public class SignRendererMixin {
    @Redirect(
        method = "renderSignText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/util/FormattedCharSequence;)I"
        )
    )
    // this injection is pretty fragile
    private int loadCachedWidth(
        Font instance, FormattedCharSequence text,
        @Local(argsOnly = true) SignText signText, @Local(ordinal = 7) int index
    ) {
        return signText.fma$getWidth(instance, index);
    }
}
