package com.floweytf.fma.features.gamestate;

import com.floweytf.fma.events.ClientReceiveSystemChatEvent;
import com.floweytf.fma.events.ClientSetTitleEvent;
import com.floweytf.fma.events.EventResult;
import com.floweytf.fma.util.ChatUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.Pair;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.DebugRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class GameState {
    private static final List<Pair<String, Supplier<StateTracker>>> GAME_STATE_BY_DIMENSION = List.of(
            Pair.of("monumenta:portal", PortalStateTracker::new),
            Pair.of("monumenta:ruin", RuinStateTracker::new),
            Pair.of("monumenta:hexfall", HexfallStateTracker::new)
    );
    private String dimensionName = null;
    @Nullable
    private StateTracker currentStateTracker = null;

    public GameState() {
        ClientTickEvents.END_CLIENT_TICK.register((EndTick) mc -> {
            this.updateLevel(mc.level);
            if (this.currentStateTracker != null && Minecraft.getInstance().player != null) {
                this.currentStateTracker.onTick();
            }
        });
        ClientReceiveSystemChatEvent.EVENT.register((ClientReceiveSystemChatEvent) text -> {
            if (this.currentStateTracker != null && Minecraft.getInstance().player != null) {
                this.currentStateTracker.onChatMessage(text);
            }

            return EventResult.CONTINUE;
        });
        ClientSetTitleEvent.TITLE.register((ClientSetTitleEvent) text -> {
            if (this.currentStateTracker != null && Minecraft.getInstance().player != null) {
                this.currentStateTracker.onTitle(text);
            }

            return EventResult.CONTINUE;
        });
        ClientSetTitleEvent.SUBTITLE.register((ClientSetTitleEvent) text -> {
            if (this.currentStateTracker != null && Minecraft.getInstance().player != null) {
                this.currentStateTracker.onSubtitle(text);
            }

            return EventResult.CONTINUE;
        });
        ClientSetTitleEvent.ACTIONBAR.register((ClientSetTitleEvent) text -> {
            if (this.currentStateTracker != null && Minecraft.getInstance().player != null) {
                this.currentStateTracker.onActionBar(text);
            }

            return EventResult.CONTINUE;
        });
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register((DebugRender) context -> {
            if (this.currentStateTracker != null && Minecraft.getInstance().player != null) {
                PoseStack stack = context.matrixStack();
                stack.pushPose();
                stack.translate(-context.camera().getPosition().x, -context.camera().getPosition().y, -context.camera().getPosition().z);
                this.currentStateTracker.onRender(context);
                stack.popPose();
            }
        });
        ClientLoginConnectionEvents.DISCONNECT.register((Disconnect) (handler, client) -> {
            if (this.currentStateTracker != null) {
                this.currentStateTracker.onLeave();
            }

            this.currentStateTracker = null;
        });
    }

    public List<Component> getAdditionalSidebarText() {
        return this.currentStateTracker == null ? List.of() : this.currentStateTracker.getAdditionalSidebarText();
    }

    private void updateLevel(ClientLevel level) {
        if (level != null) {
            String newDimensionName = level.dimension().location().toString();
            if (!Objects.equals(this.dimensionName, newDimensionName)) {
                if (this.currentStateTracker != null) {
                    this.currentStateTracker.onLeave();
                    this.currentStateTracker = null;
                }

                this.dimensionName = newDimensionName;
                Optional<Pair<String, Supplier<StateTracker>>> worldFilterRes = GAME_STATE_BY_DIMENSION.stream()
                        .filter(entry -> this.dimensionName.startsWith((String) entry.first()))
                        .findFirst();
                if (worldFilterRes.isEmpty()) {
                    ChatUtil.sendDebug("unknown dimension '" + newDimensionName + "'");
                } else {
                    this.currentStateTracker = (StateTracker) ((Supplier) worldFilterRes.get().second()).get();
                }
            }
        }
    }
}
