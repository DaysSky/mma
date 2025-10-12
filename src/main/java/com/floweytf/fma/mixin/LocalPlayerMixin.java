package com.floweytf.fma.mixin;

import com.floweytf.fma.events.EntityShieldDisabledEvent;
import com.floweytf.fma.util.SafeExceptionLogger;
import com.floweytf.fma.util.Util;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class LocalPlayerMixin {
   @Unique
   private static final SafeExceptionLogger fma$EH = new SafeExceptionLogger("ShieldDisableEvents");

   @Inject(
      method = {"handleEntityEvent"},
      at = {@At(
         value = "HEAD",
         target = "Lnet/minecraft/world/entity/Entity;handleEntityEvent(B)V"
      )}
   )
   private void fireShieldDisableEvent(byte status, CallbackInfo ci) {
      fma$EH.runSafely(() -> {
         if (status == 30) {
            ((EntityShieldDisabledEvent)EntityShieldDisabledEvent.EVENT.invoker()).onShieldDisabled(Util.c(this));
         }
      });
   }
}
