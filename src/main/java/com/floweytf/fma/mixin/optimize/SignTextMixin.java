package com.floweytf.fma.mixin.optimize;

import com.floweytf.fma.duck.SignTextAccess;
import java.lang.ref.WeakReference;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SignText.class)
public class SignTextMixin implements SignTextAccess {
    @Unique
    private final FontManager fma$fm = Minecraft.getInstance().fontManager;
    @Shadow
    @Final
    private Component[] messages;
    @Unique
    private WeakReference<Font> fma$font;
    @Unique
    private long fma$cachedReloadCounter;
    @Unique
    private int[] fma$widths = null;

    @Override
    public int fma$getWidth(Font font, int index) {
        if (index == 0) {
            if (fma$font == null || fma$font.get() != font) {
                fma$font = new WeakReference<>(font);
                fma$widths = null;
            }

            if (fma$fm.fma$getReloadCounter() != fma$cachedReloadCounter) {
                fma$cachedReloadCounter = fma$fm.fma$getReloadCounter();
                fma$widths = null;
            }

            if (fma$widths == null) {
                fma$widths = new int[messages.length];
                for (int i = 0; i < messages.length; i++) {
                    fma$widths[i] = font.width(messages[i]);
                }
            }
        }

        return Objects.requireNonNull(fma$widths)[index];
    }
}
