package com.dayssky.mma.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface ClientJoinServerEvent {
    Event<ClientJoinServerEvent> EVENT = EventFactory.createArrayBacked(ClientJoinServerEvent.class, listeners -> () -> {
        for (ClientJoinServerEvent listener : listeners) {
            listener.onJoin();
        }
    });

    void onJoin();
}
