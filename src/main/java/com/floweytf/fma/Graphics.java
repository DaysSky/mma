package com.floweytf.fma;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Graphics {
    public static final Map<String, ResourceLocation> RARITY_TO_TEXTURE = Map.of(
            "artifact", new ResourceLocation("fma:textures/inventory_overlay/artifact.png"),
            "epic", new ResourceLocation("fma:textures/inventory_overlay/epic.png"),
            "legendary", new ResourceLocation("fma:textures/inventory_overlay/legendary.png"),
            "rare", new ResourceLocation("fma:textures/inventory_overlay/rare.png"),
            "unique", new ResourceLocation("fma:textures/inventory_overlay/unique.png")
    );

    public static final List<ResourceLocation> CHARM_RARITY_TO_TEXTURE = List.of(
            new ResourceLocation("fma:textures/inventory_overlay/charm_common.png"),
            new ResourceLocation("fma:textures/inventory_overlay/charm_uncommon.png"),
            new ResourceLocation("fma:textures/inventory_overlay/charm_rare.png"),
            new ResourceLocation("fma:textures/inventory_overlay/charm_epic.png"),
            new ResourceLocation("fma:textures/inventory_overlay/charm_legendary.png")
    );
    public static final RenderType OUTLINE_BOX = RenderType.create(
            "fma:outline_box",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );

    public static void renderTexture(GuiGraphics graphics, int x, int y, float uOff, float vOff, int w, int h, int texW,
                                     int texH, ResourceLocation texture) {
        graphics.blit(texture, x, y, uOff, vOff, w, h, texW, texH);
    }

    public static void fill(GuiGraphics graphics, int sX, int sY, int eX, int eY, int color) {
        graphics.fill(sX, sY, eX, eY, color);
    }

    public static void drawString(GuiGraphics poseStack, Font font, Component text, int x, int y, int color) {
        poseStack.drawString(font, text, x, y, color);
    }

    public static void drawString(GuiGraphics graphics, Font font, Component text, int x, int y, int z, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, z);
        graphics.drawString(font, text, x, y, color);
        graphics.pose().popPose();
    }
}
