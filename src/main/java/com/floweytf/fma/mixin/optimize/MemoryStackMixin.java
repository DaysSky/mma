package com.floweytf.fma.mixin.optimize;

import com.floweytf.fma.FMAMixinConfigPlugin;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MemoryStack.class, remap = false)
public class MemoryStackMixin {
    @Shadow
    @Final
    private static ThreadLocal<MemoryStack> TLS;
    @Unique
    private static MemoryStack fma$minecraftThreadInstance;

    @Inject(
        method = "stackGet",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void fastPathStackGet(CallbackInfoReturnable<MemoryStack> cir) {
        if (FMAMixinConfigPlugin.shouldFastPathMemoryStack && Minecraft.getInstance().isSameThread()) {
            if (fma$minecraftThreadInstance == null) {
                fma$minecraftThreadInstance = TLS.get();
            }

            cir.setReturnValue(fma$minecraftThreadInstance);
        }
    }
}
