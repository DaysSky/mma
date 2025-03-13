package com.floweytf.fma.mixin;

import com.floweytf.fma.FMAClient;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void breakChest(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (FMAClient.config().features.recordChestBreak &&
            Objects.requireNonNull(minecraft.level).getBlockState(pos).getBlock() == Blocks.CHEST) {
            // TODO:
        }
    }

    @Inject(method = "performUseItemOn", at = @At("HEAD"))
    private void rightClickChest(LocalPlayer player, InteractionHand hand, BlockHitResult result,
                                 CallbackInfoReturnable<InteractionResult> cir) {
        final var pos = result.getBlockPos();
        if (FMAClient.config().features.recordChestBreak &&
            Objects.requireNonNull(minecraft.level).getBlockState(pos).getBlock() == Blocks.CHEST) {
            // TODO:
        }
    }
}
