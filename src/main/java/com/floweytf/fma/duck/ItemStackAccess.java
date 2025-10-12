package com.floweytf.fma.duck;

public interface ItemStackAccess {
    default RenderCacheState fma$getOverlayRenderCache() {
        throw new AbstractMethodError();
    }
}
