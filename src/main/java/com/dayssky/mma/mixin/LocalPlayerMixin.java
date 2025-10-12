package com.dayssky.mma.mixin;

import com.dayssky.mma.events.EntityShieldDisabledEvent;
import com.dayssky.mma.util.SafeExceptionLogger;
import com.dayssky.mma.util.Util;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class LocalPlayerMixin {
    @Unique
    private static final SafeExceptionLogger mma$EH = new SafeExceptionLogger("ShieldDisableEvents");

    @Inject(
            method = {"handleEntityEvent"},
            at = {@At(
                    value = "HEAD",
                    target = "Lnet/minecraft/world/entity/Entity;handleEntityEvent(B)V"
            )}
    )
    private void fireShieldDisableEvent(byte status, CallbackInfo ci) {
        mma$EH.runSafely(() -> {
            if (status == 30) {
                ((EntityShieldDisabledEvent) EntityShieldDisabledEvent.EVENT.invoker()).onShieldDisabled(Util.c(this));
            }
        });
    }
}
