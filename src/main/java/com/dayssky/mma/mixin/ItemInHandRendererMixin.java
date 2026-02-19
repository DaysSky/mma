package com.dayssky.mma.mixin;

import com.dayssky.mma.features.ViewModel;
import com.dayssky.mma.util.SafeExceptionLogger;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public class ItemInHandRendererMixin {
    @Unique
    private static final SafeExceptionLogger mma$EH = new SafeExceptionLogger("ViewModel");

    @Inject(
            method = {"renderArmWithItem"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )}
    )
    private void mma$applyHandViewModel(
            AbstractClientPlayer player,
            float tickDelta,
            float pitch,
            InteractionHand interactionHand,
            float swingProgress,
            ItemStack itemStack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int light,
            CallbackInfo ci
    ) {
        mma$EH.runSafely(() -> ViewModel.applyItemTransform(poseStack, interactionHand));
    }

    @ModifyExpressionValue(
            method = {"tick"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"
            )}
    )
    private float mma$overrideAttackStrengthScale(float original) {
        return mma$EH.<Float>runSafely(() -> ViewModel.overrideAttackStrengthScale(original)).orElse(original);
    }

    @Inject(
            method = {"applyItemArmTransform"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void mma$applyReequipOffset(PoseStack poseStack, HumanoidArm humanoidArm, float equipProgress, CallbackInfo ci) {
        if (mma$EH.<Boolean>runSafely(() -> ViewModel.applyEquipOffset(poseStack, humanoidArm)).orElse(false)) {
            ci.cancel();
        }
    }

    @Inject(
            method = {"applyEatTransform"},
            at = {@At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;pow(DD)D"
            )},
            cancellable = true
    )
    private void mma$cancelEatAnimation(PoseStack poseStack, float tickDelta, HumanoidArm humanoidArm, ItemStack itemStack, CallbackInfo ci) {
        if (mma$EH.<Boolean>runSafely(ViewModel::shouldCancelEatTransform).orElse(false)) {
            ci.cancel();
        }
    }

    @ModifyArgs(
            method = {"renderArmWithItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
                    ordinal = 3
            )
    )
    private void mma$removeCrossbowSwingTranslation(Args args) {
        mma$removeSwingTranslation(args);
    }

    @ModifyArgs(
            method = {"renderArmWithItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
                    ordinal = 12
            )
    )
    private void mma$removeRegularSwingTranslation(Args args) {
        mma$removeSwingTranslation(args);
    }

    @Unique
    private static void mma$removeSwingTranslation(Args args) {
        if (!mma$shouldRemoveSwing()) {
            return;
        }

        args.set(0, 0.0F);
        args.set(1, 0.0F);
        args.set(2, 0.0F);
    }

    @WrapOperation(
            method = {"renderArmWithItem"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"
            )}
    )
    private void mma$applySwingAroundAnchor(
            ItemInHandRenderer instance,
            PoseStack poseStack,
            HumanoidArm humanoidArm,
            float swingProgress,
            Operation<Void> original,
            AbstractClientPlayer player,
            float tickDelta,
            float pitch,
            InteractionHand interactionHand,
            float outerSwingProgress,
            ItemStack itemStack,
            float equipProgress,
            PoseStack matrixStack,
            MultiBufferSource multiBufferSource,
            int light
    ) {
        if (mma$shouldRemoveSwing()) {
            float posX = mma$EH.<Float>runSafely(() -> ViewModel.getSwingAnchorX(interactionHand)).orElse(0.0F);
            float posY = mma$EH.<Float>runSafely(() -> ViewModel.getSwingAnchorY(interactionHand)).orElse(0.0F);
            float posZ = mma$EH.<Float>runSafely(() -> ViewModel.getSwingAnchorZ(interactionHand)).orElse(0.0F);
            poseStack.translate(posX, posY, posZ);
            original.call(instance, poseStack, humanoidArm, swingProgress);
            poseStack.translate(-posX, -posY, -posZ);
            return;
        }

        original.call(instance, poseStack, humanoidArm, swingProgress);
    }

    @WrapOperation(
            method = {"renderArmWithItem"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V"
            )}
    )
    private void mma$skipEmptyHandRender(
            ItemInHandRenderer instance,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int light,
            float equipProgress,
            float swingProgress,
            HumanoidArm humanoidArm,
            Operation<Void> original,
            AbstractClientPlayer player,
            float tickDelta,
            float pitch,
            InteractionHand interactionHand,
            float outerSwingProgress,
            ItemStack itemStack,
            float outerEquipProgress,
            PoseStack matrixStack,
            MultiBufferSource outerMultiBufferSource,
            int outerLight
    ) {
        if (mma$EH.<Boolean>runSafely(() -> ViewModel.shouldHideEmptyHand(itemStack)).orElse(false)) {
            return;
        }
        original.call(instance, poseStack, multiBufferSource, light, equipProgress, swingProgress, humanoidArm);
    }

    @Unique
    private static boolean mma$shouldRemoveSwing() {
        return mma$EH.<Boolean>runSafely(ViewModel::shouldRemoveSwing).orElse(false);
    }
}
