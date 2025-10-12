package com.dayssky.mma.mixin;

import com.dayssky.mma.duck.ItemStackAccess;
import com.dayssky.mma.duck.RenderCacheState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({ItemStack.class})
public class ItemStackMixin implements ItemStackAccess {
    @Unique
    private final RenderCacheState mma$overlayState = new RenderCacheState();

    @Override
    public RenderCacheState mma$getOverlayRenderCache() {
        return this.mma$overlayState;
    }
}
