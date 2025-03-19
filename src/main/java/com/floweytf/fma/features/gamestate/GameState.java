package com.floweytf.fma.features.gamestate;

import com.floweytf.fma.events.ClientReceiveSystemChatEvent;
import com.floweytf.fma.events.ClientSetTitleEvent;
import com.floweytf.fma.events.EventResult;
import com.floweytf.fma.util.ChatUtil;
import it.unimi.dsi.fastutil.Pair;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class GameState {
    private static final List<Pair<String, Supplier<StateTracker>>> GAME_STATE_BY_DIMENSION = List.of(
        Pair.of("monumenta:portal", PortalStateTracker::new),
        Pair.of("monumenta:ruin", RuinStateTracker::new)
        // Pair.of("monumenta:verdant", VerdantStateTracker::new),
        // Pair.of("monumenta:sanctum", SanctumStateTracker::new)
        // Pair.of("monumenta:mist", RuinStateTracker::new)
        // Pair.of("monumenta:remorse", RuinStateTracker::new)
    );

    private String dimensionName = null;
    @Nullable
    private StateTracker currentStateTracker = null;

    public GameState() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            updateLevel(mc.level);

            if (currentStateTracker != null && Minecraft.getInstance().player != null) {
                currentStateTracker.onTick();
            }
        });

        ClientReceiveSystemChatEvent.EVENT.register(text -> {
            if (currentStateTracker != null && Minecraft.getInstance().player != null) {
                currentStateTracker.onChatMessage(text);
            }
            return EventResult.CONTINUE;
        });

        ClientSetTitleEvent.TITLE.register(text -> {
            if (currentStateTracker != null && Minecraft.getInstance().player != null) {
                currentStateTracker.onTitle(text);
            }
            return EventResult.CONTINUE;
        });

        ClientSetTitleEvent.SUBTITLE.register(text -> {
            if (currentStateTracker != null && Minecraft.getInstance().player != null) {
                currentStateTracker.onSubtitle(text);
            }
            return EventResult.CONTINUE;
        });

        ClientSetTitleEvent.ACTIONBAR.register(text -> {
            if (currentStateTracker != null && Minecraft.getInstance().player != null) {
                currentStateTracker.onActionBar(text);
            }
            return EventResult.CONTINUE;
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (currentStateTracker == null || Minecraft.getInstance().player == null) {
                return;
            }

            final var stack = context.matrixStack();
            stack.pushPose();
            stack.translate(
                -context.camera().getPosition().x,
                -context.camera().getPosition().y,
                -context.camera().getPosition().z
            );

            currentStateTracker.onRender(context);
            stack.popPose();
        });

        ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (currentStateTracker != null) {
                currentStateTracker.onLeave();
            }

            currentStateTracker = null;
        });
    }

    public List<Component> getAdditionalSidebarText() {
        return currentStateTracker == null ? List.of() : currentStateTracker.getAdditionalSidebarText();
    }

    private void updateLevel(ClientLevel level) {
        if (level == null)
            return;

        final var newDimensionName = level.dimension().location().toString();

        // Edge-triggered
        if (Objects.equals(dimensionName, newDimensionName)) {
            return;
        }

        // Trigger leave event...
        if (currentStateTracker != null) {
            currentStateTracker.onLeave();
            currentStateTracker = null;
        }

        dimensionName = newDimensionName;

        final var worldFilterRes = GAME_STATE_BY_DIMENSION.stream()
            .filter(entry -> dimensionName.startsWith(entry.first()))
            .findFirst();

        if (worldFilterRes.isEmpty()) {
            ChatUtil.sendDebug("unknown dimension '" + newDimensionName + "'");
            return;
        }

        currentStateTracker = worldFilterRes.get().second().get();
    }
}
