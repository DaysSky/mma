package com.floweytf.fma.mixin.optimize;

import com.floweytf.fma.duck.FontManagerAccess;
import com.floweytf.fma.duck.SignBlockEntityAccess;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin implements SignBlockEntityAccess {
    @Shadow
    private SignText frontText;
    @Shadow
    private SignText backText;

    @Unique
    private int[] fma$cachedFrontTextWidth;
    @Unique
    private int[] fma$cachedBackTextWidth;
    @Unique
    private WeakReference<Font> fma$font;
    @Unique
    private long fma$cachedReloadCounter;
    @Unique
    private boolean fma$dirty = true;

    @Inject(method = "load", at = @At("RETURN"))
    private void clearOnLoad(CompoundTag tag, CallbackInfo ci) {
        fma$dirty = true;
    }

    @Inject(method = "setText", at = @At("RETURN"))
    private void onSetText(SignText text, boolean isFrontText, CallbackInfoReturnable<Boolean> cir) {
        fma$dirty = true;
    }

    @Override
    public void fma$updateCaches(Minecraft minecraft, Font font) {
        if (fma$font == null || fma$font.get() != font) {
            fma$font = new WeakReference<>(font);
            fma$dirty = true;
        }

        if (((FontManagerAccess) minecraft.fontManager).fma$getReloadCounter() != fma$cachedReloadCounter) {
            fma$cachedReloadCounter = ((FontManagerAccess) minecraft.fontManager).fma$getReloadCounter();
            fma$dirty = true;
        }

        if (fma$dirty) {
            fma$cachedFrontTextWidth = Arrays.stream(frontText.getMessages(false))
                .mapToInt(font::width)
                .toArray();

            fma$cachedBackTextWidth = Arrays.stream(backText.getMessages(false))
                .mapToInt(font::width)
                .toArray();
            fma$dirty = false;
        }
    }

    @Override
    public int[] fma$getCachedBackTextWidth() {
        return fma$cachedBackTextWidth;
    }

    @Override
    public int[] fma$getCachedFrontTextWidth() {
        return fma$cachedFrontTextWidth;
    }
}
