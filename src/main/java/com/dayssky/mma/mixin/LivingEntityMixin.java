package com.dayssky.mma.mixin;

import com.dayssky.mma.features.ViewModel;
import com.dayssky.mma.util.SafeExceptionLogger;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin {
    @Unique
    private static final SafeExceptionLogger mma$EH = new SafeExceptionLogger("ViewModel");

    @ModifyReturnValue(
            method = {"getCurrentSwingDuration"},
            at = @At("RETURN")
    )
    private int mma$onGetCurrentSwingDuration(int original) {
        if (!mma$isLocalPlayer()) {
            return original;
        }

        return mma$EH.<Integer>runSafely(() -> ViewModel.scaleSwingDuration(original)).orElse(original);
    }

    @Unique
    private boolean mma$isLocalPlayer() {
        return (Object) this instanceof LocalPlayer;
    }
}
