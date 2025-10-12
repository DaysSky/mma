package com.dayssky.mma.mixin;

import com.dayssky.mma.features.ItemOverlay;
import com.dayssky.mma.util.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiGraphics.class})
public abstract class GuiGraphicsMixin {
    @Inject(
            method = {"renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                    shift = Shift.AFTER
            )}
    )
    private void renderItemOverlay(Font font, ItemStack stack, int x, int y, String string, CallbackInfo ci) {
        ItemOverlay.renderItemOverlay(Util.c(this), font, stack, x, y);
    }
}
