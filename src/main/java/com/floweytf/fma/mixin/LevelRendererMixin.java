package com.floweytf.fma.mixin;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.features.HpIndicator;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @ModifyExpressionValue(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean modifyPlayerGlowingStatus(boolean original, @Local Entity entity) {
        if (!FMAClient.features().enableHpIndicators) {
            return original;
        }

        if (!FMAClient.config().hpIndicator.enableGlowingPlayer) {
            return original;
        }

        if (entity instanceof Player) {
            return true;
        }

        return original;
    }

    @ModifyExpressionValue(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"
        )
    )
    private int modifyPlayerGlowingColor(int original, @Local Entity entity) {
        if (!FMAClient.features().enableHpIndicators) {
            return original;
        }

        if (!FMAClient.config().hpIndicator.enableGlowingPlayer) {
            return original;
        }

        if (entity instanceof Player player) {
            return HpIndicator.computeEntityHealthColor(player);
        }

        return original;
    }

    /*
    @Inject(
        method = "renderLevel",
        at = @At(value = "CONSTANT", args = "stringValue=blockentities")
    )
    private void renderOutline(
        PoseStack stack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera,
        GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci,
        @Local(ordinal = 3) LocalBooleanRef booleanRef
        ) {
        stack.pushPose();
        stack.translate(
            -camera.getPosition().x,
            -camera.getPosition().y,
            -camera.getPosition().z
        );

        final var outlineSource = Minecraft.getInstance().renderBuffers().outlineBufferSource();
        outlineSource.setColor(255, 255, 255, 255);
        final var RT = RenderType.outline(Graphics.CHARM_RARITY_TO_TEXTURE.get(0));
        final var consumer = outlineSource.getBuffer(RT);
        int x = 10, y = 10, z = 10;

        consumer.vertex(stack.last().pose(), x, y, z).color(0xffffffff).uv(0, 0).endVertex();
        consumer.vertex(stack.last().pose(), x, y + 1, z).color(0xffffffff).uv(0, 1).endVertex();
        consumer.vertex(stack.last().pose(), x + 1, y + 1, z).color(0xffffffff).uv(1, 1).endVertex();
        consumer.vertex(stack.last().pose(), x + 1, y, z).color(0xffffffff).uv(1, 0).endVertex();
        booleanRef.set(true);
        stack.popPose();
    }*/
}
