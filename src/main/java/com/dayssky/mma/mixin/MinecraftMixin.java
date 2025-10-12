package com.dayssky.mma.mixin;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.util.ChatUtil;
import com.dayssky.mma.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({Minecraft.class})
public abstract class MinecraftMixin extends ReentrantBlockableEventLoop<Runnable> {
    @Shadow
    @Nullable
    public LocalPlayer field_1724;

    public MinecraftMixin(String name) {
        super(name);
    }

    public void doRunTask(Runnable task) {
        if (MMAClient.config().features.enableDebug) {
            long start = Util.now();
            super.doRunTask(task);
            long end = Util.now();
            if (end - start > 5L && this.field_1724 != null) {
                ChatUtil.sendWarn("Scheduled executable: " + task + " took too long!");
            }
        } else {
            super.doRunTask(task);
        }
    }
}
