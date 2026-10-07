package com.dayssky.mma.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;

public interface ClientPlayerDeathEvent {
    Event<ClientPlayerDeathEvent> EVENT = EventFactory.createArrayBacked(
            ClientPlayerDeathEvent.class,
            listeners -> (playerId, message) -> {
                for (ClientPlayerDeathEvent listener : listeners) {
                    listener.onDeath(playerId, message);
                }
            }
    );

    void onDeath(int playerId, Component deathMessage);
}