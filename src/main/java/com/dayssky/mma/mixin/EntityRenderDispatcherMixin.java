package com.dayssky.mma.mixin;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.features.HpIndicator;
import com.dayssky.mma.util.SafeExceptionLogger;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin {
    @Unique
    private static final SafeExceptionLogger mma$EH = new SafeExceptionLogger("EntityRenderDispatcherMixin");

    @ModifyArgs(
            method = {"renderHitbox"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLineBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/AABB;FFFF)V",
                    ordinal = 0
            )
    )
    private static void modifyHitboxColor(Args args, @Local(argsOnly = true) Entity entity) {
        mma$EH.runSafely(() -> {
            if (MMAClient.features().enableHpIndicators) {
                if (MMAClient.config().hpIndicator.enableHitboxColoring) {
                    if (entity instanceof LivingEntity livingEntity) {
                        int color = HpIndicator.computeEntityHealthColor(livingEntity);
                        args.set(3, ARGB32.red(color) / 255.0F);
                        args.set(4, ARGB32.green(color) / 255.0F);
                        args.set(5, ARGB32.blue(color) / 255.0F);
                    }
                }
            }
        }, () -> "entity: " + entity.toString());
    }
}
