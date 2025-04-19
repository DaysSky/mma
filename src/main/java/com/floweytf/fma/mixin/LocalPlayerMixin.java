package com.floweytf.fma.mixin;

import com.floweytf.fma.events.EntityShieldDisabledEvent;
import com.floweytf.fma.util.Util;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.world.entity.EntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "handleEntityEvent", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/Entity;" +
        "handleEntityEvent(B)V"))
    private void fireShieldDisablEvent(byte status, CallbackInfo ci) {
        if (status == EntityEvent.SHIELD_DISABLED) {
            EntityShieldDisabledEvent.EVENT.invoker().onShieldDisabled(Util.c(this));
        }
    }
}
