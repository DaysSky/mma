package com.floweytf.fma.util;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

record Task(long targetTick, Consumer<Minecraft> handler) implements Comparable<Task> {
    public int compareTo(@NotNull Task task) {
        return (int)(this.targetTick - task.targetTick);
    }
}
