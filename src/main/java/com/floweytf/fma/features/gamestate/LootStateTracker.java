package com.floweytf.fma.features.gamestate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;

public class LootStateTracker implements StateTracker {
    private Set<BlockPos> blockPos = new HashSet<>();
    @Override
    public void onRender(WorldRenderContext context) {
        blockPos.forEach(pos -> {
            final var buffer = Objects.requireNonNull(context.consumers()).getBuffer(RenderType.lines());
            final var pose = context.matrixStack();
            LevelRenderer.renderLineBox(pose, buffer,
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                    1, 1, 1, 1
                    );
        });
    }
}
