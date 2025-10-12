package com.dayssky.mma.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface ClientRespawnEvent {
    Event<ClientRespawnEvent> EVENT = EventFactory.createArrayBacked(ClientRespawnEvent.class, listeners -> () -> {
        for (ClientRespawnEvent listener : listeners) {
            listener.onRespawn();
        }
    });

    void onRespawn();
}
