package com.dayssky.mma.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;

public interface ClientPlayerDeathEvent {
    Event<ClientPlayerDeathEvent> EVENT = EventFactory.createArrayBacked(
            ClientPlayerDeathEvent.class,
            listeners -> player -> {
                for (ClientPlayerDeathEvent listener : listeners) {
                    listener.onDeath(player);
                }
            }
    );

    void onDeath(Player player);
}