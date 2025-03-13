package com.floweytf.fma.mixin.optimize;

import com.floweytf.fma.duck.FontManagerAccess;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontManager.class)
public class FontManagerMixin implements FontManagerAccess {
    @Unique
    private long fma$reloadCounter;

    @Inject(method = "reload", at = @At("HEAD"))
    private void incrementReloadCounter(
        PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager,
        ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor,
        Executor gameExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir
    ) {
        fma$reloadCounter++;
    }

    @Override
    public long fma$getReloadCounter() {
        return fma$reloadCounter;
    }
}
