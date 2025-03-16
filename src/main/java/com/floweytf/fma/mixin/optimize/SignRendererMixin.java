package com.floweytf.fma.mixin.optimize;

import com.floweytf.fma.duck.SignBlockEntityAccess;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignRenderer.class)
public class SignRendererMixin {
    @Shadow
    @Final
    private Font font;
    @Unique
    private int[] fma$widths;

    @Redirect(
        method = "renderSignText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/util/FormattedCharSequence;)I"
        )
    )
    // this injection is pretty fragile
    private int loadCachedWidth(Font instance, FormattedCharSequence text, @Local(ordinal = 7) int index) {
        return fma$widths[index];
    }

    @Inject(
        method = "renderSignWithText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/SignRenderer;renderSignText" +
                "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SignText;" +
                "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIIZ)V",
            ordinal = 0
        )
    )
    private void initializeCache(
        SignBlockEntity signEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay,
        BlockState state, SignBlock signBlock, WoodType woodType, Model model, CallbackInfo ci
    ) {
        signEntity.fma$updateCaches(Minecraft.getInstance(), font);
        fma$widths = signEntity.fma$getCachedFrontTextWidth();
    }

    @Inject(
        method = "renderSignWithText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/SignRenderer;renderSignText" +
                "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SignText;" +
                "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIIZ)V",
            ordinal = 1
        )
    )
    private void setWidthBack(
        SignBlockEntity signEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay,
        BlockState state, SignBlock signBlock, WoodType woodType, Model model, CallbackInfo ci
    ) {
        fma$widths = signEntity.fma$getCachedBackTextWidth();
    }
}
