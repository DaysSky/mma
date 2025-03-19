package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;

public class Waypoint implements AbstractModule<Waypoint.Config> {
    public static final class Config {
        public boolean enable;
    }

    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("fma-waypoint.json");
    private static final Codec<Map<String, List<BlockPos>>> CODEC = Codec.unboundedMap(
        Codec.STRING,
        BlockPos.CODEC.listOf()
    );

    // note: only access from main thread!
    private Map<String, List<BlockPos>> byWorld = new HashMap<>();
    private CompletableFuture<Void> currCompletionToken = CompletableFuture.completedFuture(null);
    private final Minecraft minecraft = Minecraft.getInstance();

    private void load(Path path) {
        Preconditions.checkState(minecraft.isSameThread());

        if (!Files.exists(path)) {
            return;
        }

        try (final var in = Files.newBufferedReader(path)) {
            final var obj = FMAClient.GSON.fromJson(in, JsonElement.class);
            byWorld = CODEC.decode(JsonOps.INSTANCE, obj)
                .result()
                .orElseThrow()
                .getFirst();
        } catch (Exception e) {
            FMAClient.LOGGER.warn(e);
        }
    }

    private void save() {
        Preconditions.checkState(minecraft.isSameThread());
        final var copy = new HashMap<>(byWorld);

        final var newToken = new CompletableFuture<Void>();

        currCompletionToken.thenRunAsync(() -> {
            try {
                final var json = CODEC.encodeStart(JsonOps.INSTANCE, copy)
                    .result()
                    .orElseThrow();

                try (final var w = Files.newBufferedWriter(Waypoint.PATH)) {
                    FMAClient.GSON.toJson(json, w);
                }
            } catch (Exception e) {
                FMAClient.LOGGER.warn(e);
            }
        }).thenRunAsync(() -> newToken.complete(null), Minecraft.getInstance());

        currCompletionToken = newToken;
    }

    @Override
    public Config getConfig(FMAConfig config) {
        return config.features.waypoint;
    }

    @Override
    public void init() {
        save();
    }

    @Override
    public void clientInit() {

    }

    @Override
    public void tick() {

    }

    @Override
    public void render(WorldRenderContext context) {
        final var outlineSource = new OutlineBufferSource((MultiBufferSource.BufferSource) context.consumers());
        final var consumer = outlineSource.getBuffer(RenderType.lines().outline().get());
        final var entries = byWorld.get(FMAClient.level().dimension().location().toString());

        for (final var entry : entries) {
            int x = entry.getX();
            int y = entry.getY();
            int z = entry.getZ();
            LevelRenderer.renderLineBox(context.matrixStack(), consumer, x, y, z, x + 1, y + 1, z + 1, 0, 1, 0, 1);
        }

        outlineSource.endOutlineBatch();
    }
}
