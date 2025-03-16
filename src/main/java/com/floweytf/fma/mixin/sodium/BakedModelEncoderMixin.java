package com.floweytf.fma.mixin.sodium;

import com.floweytf.fma.FMAClient;
import me.jellysquid.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BakedModelEncoder.class)
public class BakedModelEncoderMixin {
    @Unique
    private static MemoryStack fma$MS;

    @Redirect(
        method = "writeQuadVertices*",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/system/MemoryStack;stackPush()Lorg/lwjgl/system/MemoryStack;",
            remap = false
        )
    )
    private static MemoryStack fastStackPush() {
        if (fma$MS == null) {
            FMAClient.LOGGER.info("fast-path stack push: loading cache with MemoryStack.stackGet()");
            fma$MS = MemoryStack.stackGet();
        }

        return fma$MS.push();
    }
}
