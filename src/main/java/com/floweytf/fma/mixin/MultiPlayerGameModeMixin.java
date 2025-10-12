package com.floweytf.fma.mixin;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.features.ContractCheck;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "setLocalMode(Lnet/minecraft/world/level/GameType;)V", at = @At("HEAD"))
    private void onChangeGameMode(GameType type, CallbackInfo ci) {
        if (FMAClient.config().features.contractCheck && type == GameType.SURVIVAL) {
            ContractCheck.onChangeGameMode();
        }
    }
}
