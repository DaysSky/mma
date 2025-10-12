package com.floweytf.fma.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;

public interface ClientReceiveSystemChatEvent {
   Event<ClientReceiveSystemChatEvent> EVENT = EventFactory.createArrayBacked(ClientReceiveSystemChatEvent.class, listeners -> text -> {
      boolean flag = false;

      for (ClientReceiveSystemChatEvent listener : listeners) {
         switch (listener.onMessage(text)) {
            case CANCEL_NOW:
               return EventResult.CANCEL_NOW;
            case CANCEL_CONTINUE:
               flag = true;
            case CONTINUE:
         }
      }

      return flag ? EventResult.CANCEL_CONTINUE : EventResult.CONTINUE;
   });

   EventResult onMessage(Component var1);
}
