package com.floweytf.fma.util;

import java.util.PriorityQueue;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public class TickScheduler {
    public interface TaskControl {
        void cancel();
    }

    private record Task(long targetTick, Consumer<Minecraft> handler) implements Comparable<Task> {
        @Override
        public int compareTo(@NotNull TickScheduler.Task task) {
            return (int) (targetTick - task.targetTick);
        }
    }

    private final PriorityQueue<Task> taskQueue = new PriorityQueue<>();
    private long tick;

    public TickScheduler() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            tick++;
            while (!taskQueue.isEmpty() && taskQueue.peek().targetTick < tick) {
                taskQueue.poll().handler.accept(client);
            }
        });
    }

    private void doCancel(Task task) {
        taskQueue.remove(task);
    }

    public TaskControl schedule(int delay, Consumer<Minecraft> handler) {
        final var task = new Task(tick + delay, handler);
        taskQueue.add(task);
        return () -> doCancel(task);
    }
}
