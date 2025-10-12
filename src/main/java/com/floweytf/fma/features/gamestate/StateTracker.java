package com.floweytf.fma.features.gamestate;

import java.util.List;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.network.chat.Component;

public interface StateTracker {
    default void onLeave() {
    }

    default void onChatMessage(Component message) {
    }

    default void onTitle(Component message) {
    }

    default void onSubtitle(Component message) {
    }

    default void onActionBar(Component message) {
    }

    default void onTick() {
    }

    default void onRender(WorldRenderContext context) {
    }

    default List<Component> getAdditionalSidebarText() {
        return List.of();
    }
}
