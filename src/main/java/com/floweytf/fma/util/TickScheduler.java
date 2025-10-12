package com.floweytf.fma.util;

import java.util.PriorityQueue;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;
import net.minecraft.client.Minecraft;

public class TickScheduler {
    private final PriorityQueue<Task> taskQueue = new PriorityQueue<>();
    private long tick;

    public TickScheduler() {
        ClientTickEvents.START_CLIENT_TICK.register((StartTick) client -> {
            this.tick++;

            while (!this.taskQueue.isEmpty() && this.taskQueue.peek().targetTick() < this.tick) {
                this.taskQueue.poll().handler().accept(client);
            }
        });
    }

    private void doCancel(Task task) {
        this.taskQueue.remove(task);
    }

    public TaskControl schedule(int delay, Consumer<Minecraft> handler) {
        Task task = new Task(this.tick + delay, handler);
        this.taskQueue.add(task);
        return () -> this.doCancel(task);
    }
}
