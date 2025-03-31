package com.floweytf.fma.mixin.sodium;

import com.bawnorton.mixinsquared.TargetHandler;
import com.floweytf.fma.FMAClient;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BakedGlyph.class, priority = 1001)
public class BakedGlyphMixin {
    @Unique
    private static MemoryStack fma$MS;

    @TargetHandler(
        mixin = "me.jellysquid.mods.sodium.mixin.features.render.gui.font.GlyphRendererMixin",
        name = "drawFast"
    )
    @Redirect(
        method = "@MixinSquared:Handler",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/system/MemoryStack;stackPush()Lorg/lwjgl/system/MemoryStack;",
            remap = false
        ),
        require = 0
    )
    private static MemoryStack fastStackPush() {
        if (fma$MS == null) {
            FMAClient.LOGGER.info("fast-path stack push: loading cache with MemoryStack.stackGet()");
            fma$MS = MemoryStack.stackGet();
        }

        return fma$MS.push();
    }
}
