package com.floweytf.fma.mixin;

import com.floweytf.fma.features.ItemOverlay;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.floweytf.fma.util.Util.c;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;" +
            "IILjava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"
        )
    )
    private void renderItemOverlay(Font font, ItemStack stack, int x, int y, String string, CallbackInfo ci) {
        ItemOverlay.renderItemOverlay(c(this), font, stack, x, y);
    }

    @Inject(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;" +
            "IILjava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isBarVisible()Z"
        )
    )
    private void renderVanityDurability(Font font, ItemStack stack, int x, int y, String string, CallbackInfo ci) {
        ItemOverlay.renderVanityDurability(c(this), stack, x, y);
    }
}
