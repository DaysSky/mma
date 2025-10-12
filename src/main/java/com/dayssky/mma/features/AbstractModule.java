package com.dayssky.mma.features;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.MMAConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public interface AbstractModule<T> {
    T readConfigFrom(MMAConfig var1);

    default T config() {
        return this.readConfigFrom(MMAClient.config());
    }

    void init();

    void clientInit();

    void tick();

    void render(WorldRenderContext var1);
}
