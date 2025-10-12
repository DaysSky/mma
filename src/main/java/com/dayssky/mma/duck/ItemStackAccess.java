package com.dayssky.mma.duck;

public interface ItemStackAccess {
    default RenderCacheState mma$getOverlayRenderCache() {
        throw new AbstractMethodError();
    }
}
