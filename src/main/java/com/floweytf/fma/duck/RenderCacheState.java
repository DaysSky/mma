package com.floweytf.fma.duck;

import com.floweytf.fma.features.ItemOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class RenderCacheState {
    public long lastUpdateTick;
    public List<ItemOverlay.RenderOp> renderOps = new ArrayList<>();

    public void render(int ticks, Supplier<List<ItemOverlay.RenderOp>> renderOpsBuilder, GuiGraphics graphics,
                       Font font, int x, int y) {
        final var currTick = Minecraft.getInstance().clientTickCount;
        if (lastUpdateTick + ticks <= currTick) {
            lastUpdateTick = currTick;
            renderOps = null;
        }

        if (renderOps == null) {
            renderOps = renderOpsBuilder.get();
        }

        for (ItemOverlay.RenderOp renderOp : renderOps) {
            renderOp.render(graphics, font, x, y);
        }
    }
}
