package com.floweytf.fma.features;

import com.floweytf.fma.FMAConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public interface AbstractModule<T> {
    T getConfig(FMAConfig config);

    void init();

    void clientInit();

    void tick();

    void render(WorldRenderContext context);
}
