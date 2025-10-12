package com.floweytf.fma.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface ClientReceiveTabListCustomizationEvent {
    Event<ClientReceiveTabListCustomizationEvent> EVENT = EventFactory.createArrayBacked(
            ClientReceiveTabListCustomizationEvent.class, listeners -> (header, footer) -> {
                boolean flag = false;

                for (ClientReceiveTabListCustomizationEvent listener : listeners) {
                    switch (listener.onEvent(header, footer)) {
                        case CANCEL_NOW:
                            return EventResult.CANCEL_NOW;
                        case CANCEL_CONTINUE:
                            flag = true;
                        case CONTINUE:
                    }
                }

                return flag ? EventResult.CANCEL_CONTINUE : EventResult.CONTINUE;
            }
    );

    EventResult onEvent(@Nullable Component var1, @Nullable Component var2);
}
