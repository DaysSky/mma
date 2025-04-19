package com.floweytf.fma.mixin;

import com.floweytf.fma.duck.ItemStackAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public class ItemStackMixin implements ItemStackAccess {
    @Unique
    private final RenderCacheState fma$overlayState = new RenderCacheState();

    @Override
    public RenderCacheState fma$getOverlayRenderCache() {
        return fma$overlayState;
    }
}
