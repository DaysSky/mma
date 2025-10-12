package com.dayssky.mma.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;

public interface ClientSetTitleEvent {
    Event<ClientSetTitleEvent> TITLE = EventFactory.createArrayBacked(ClientSetTitleEvent.class, listeners -> text -> {
        boolean flag = false;

        for (ClientSetTitleEvent listener : listeners) {
            switch (listener.onSetTitle(text)) {
                case CANCEL_NOW:
                    return EventResult.CANCEL_NOW;
                case CANCEL_CONTINUE:
                    flag = true;
                case CONTINUE:
            }
        }

        return flag ? EventResult.CANCEL_CONTINUE : EventResult.CONTINUE;
    });
    Event<ClientSetTitleEvent> SUBTITLE = EventFactory.createArrayBacked(ClientSetTitleEvent.class, listeners -> text -> {
        boolean flag = false;

        for (ClientSetTitleEvent listener : listeners) {
            switch (listener.onSetTitle(text)) {
                case CANCEL_NOW:
                    return EventResult.CANCEL_NOW;
                case CANCEL_CONTINUE:
                    flag = true;
                case CONTINUE:
            }
        }

        return flag ? EventResult.CANCEL_CONTINUE : EventResult.CONTINUE;
    });
    Event<ClientSetTitleEvent> ACTIONBAR = EventFactory.createArrayBacked(ClientSetTitleEvent.class, listeners -> text -> {
        boolean flag = false;

        for (ClientSetTitleEvent listener : listeners) {
            switch (listener.onSetTitle(text)) {
                case CANCEL_NOW:
                    return EventResult.CANCEL_NOW;
                case CANCEL_CONTINUE:
                    flag = true;
                case CONTINUE:
            }
        }

        return flag ? EventResult.CANCEL_CONTINUE : EventResult.CONTINUE;
    });

    EventResult onSetTitle(Component var1);
}
