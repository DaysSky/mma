package com.floweytf.fma.duck;

import com.floweytf.fma.features.ItemOverlay;
import java.util.ArrayList;
import java.util.List;

public interface ItemStackAccess {
    class RenderCacheState {
        public long lastUpdateTick;
        public List<ItemOverlay.RenderOp> renderOps = new ArrayList<>();
    }

    default RenderCacheState fma$getRenderCache() {
        throw new AbstractMethodError();
    }
}
