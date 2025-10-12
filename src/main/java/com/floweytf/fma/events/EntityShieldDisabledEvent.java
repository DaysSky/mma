package com.floweytf.fma.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.player.LocalPlayer;

@FunctionalInterface
public interface EntityShieldDisabledEvent {
   Event<EntityShieldDisabledEvent> EVENT = EventFactory.createArrayBacked(EntityShieldDisabledEvent.class, listeners -> entity -> {
      for (EntityShieldDisabledEvent listener : listeners) {
         listener.onShieldDisabled(entity);
      }
   });

   void onShieldDisabled(LocalPlayer var1);
}
