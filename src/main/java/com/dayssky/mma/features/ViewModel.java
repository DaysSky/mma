package com.dayssky.mma.features;

import com.dayssky.mma.MMAClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.shedaniel.autoconfig.annotation.ConfigEntry.BoundedDiscrete;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public class ViewModel {

    public static void applyItemTransform(PoseStack poseStack, InteractionHand interactionHand) {
        Config config = MMAClient.features().viewModel;
        if (interactionHand == InteractionHand.MAIN_HAND) {
            applyTransform(poseStack, config.posX, config.posY, config.posZ, config.rotX, config.rotY, config.rotZ, config.scale);
        } else if (interactionHand == InteractionHand.OFF_HAND) {
            applyTransform(poseStack, config.offPosX, config.offPosY, config.offPosZ,  config.offRotX, config.offRotY, config.offRotZ, config.offScale);
        }
    }

    public static float overrideAttackStrengthScale(float original) {
        return MMAClient.features().viewModel.cancelReEquip ? 1.0F : original;
    }

    public static boolean applyEquipOffset(PoseStack poseStack, HumanoidArm humanoidArm) {
        if (!MMAClient.features().viewModel.cancelReEquip) {
            return false;
        }

        int direction = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(direction * 0.56F, -0.52F, -0.72F);
        return true;
    }

    public static boolean shouldCancelEatTransform() {
        return MMAClient.features().viewModel.rotationlessDrink;
    }

    public static boolean shouldHideEmptyHand(ItemStack itemStack) {
        return MMAClient.features().viewModel.hideEmptyHand && itemStack.isEmpty();
    }

    public static boolean shouldRemoveSwing() {
        return MMAClient.features().viewModel.removeSwing;
    }

    public static float getSwingAnchorX(InteractionHand interactionHand) {
        Config config = MMAClient.features().viewModel;
        return (interactionHand == InteractionHand.OFF_HAND ? config.offPosX : config.posX) / 100.0F;
    }

    public static float getSwingAnchorY(InteractionHand interactionHand) {
        Config config = MMAClient.features().viewModel;
        return (interactionHand == InteractionHand.OFF_HAND ? config.offPosY : config.posY) / 100.0F;
    }

    public static float getSwingAnchorZ(InteractionHand interactionHand) {
        Config config = MMAClient.features().viewModel;
        return (interactionHand == InteractionHand.OFF_HAND ? config.offPosZ : config.posZ) / 100.0F;
    }

    public static int scaleSwingDuration(int originalDuration) {
        if (originalDuration <= 0) {
            return originalDuration;
        }

        float swingSpeed = Mth.clamp(MMAClient.features().viewModel.swingSpeed, 0.01F, 5F);
        return Math.max(1, Math.round(originalDuration / swingSpeed));
    }

    private static void applyTransform(PoseStack poseStack, int posX, int posY, int posZ, int rotX, int rotY, int rotZ, float scale) {
        poseStack.translate(posX / 100.0F, posY / 100.0F, posZ / 100.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
        poseStack.scale(scale, scale, scale);
    }

    public static class Config {
        @BoundedDiscrete(min = -50L, max = 50L)
        public int posX = 0;
        @BoundedDiscrete(min = -50L, max = 50L)
        public int posY = 0;
        @BoundedDiscrete(min = -50L, max = 50L)
        public int posZ = 0;
        @BoundedDiscrete(min = -180L, max = 180L)
        public int rotX = 0;
        @BoundedDiscrete(min = -180L, max = 180L)
        public int rotY = 0;
        @BoundedDiscrete(min = -180L, max = 180L)
        public int rotZ = 0;
        public float scale = 1.0F;
        @BoundedDiscrete(min = -50L, max = 50L)
        public int offPosX = 0;
        @BoundedDiscrete(min = -50L, max = 50L)
        public int offPosY = 0;
        @BoundedDiscrete(min = -50L, max = 50L)
        public int offPosZ = 0;
        @BoundedDiscrete(min = -180L, max = 180L)
        public int offRotX = 0;
        @BoundedDiscrete(min = -180L, max = 180L)
        public int offRotY = 0;
        @BoundedDiscrete(min = -180L, max = 180L)
        public int offRotZ = 0;
        public float offScale = 1.0F;
        public boolean cancelReEquip = false;
        public boolean rotationlessDrink = true;
        public boolean hideEmptyHand = false;
        public boolean removeSwing = false;
        public float swingSpeed = 1.0F;
    }
}
