package com.dayssky.mma;

import com.dayssky.mma.MMAConfig.Appearance;
import com.dayssky.mma.MMAConfig.FeatureToggles;
import com.dayssky.mma.debug.Debug;
import com.dayssky.mma.events.EntityShieldDisabledEvent;
import com.dayssky.mma.features.Commands;
import com.dayssky.mma.features.ContractCheck;
import com.dayssky.mma.features.Keybinds;
import com.dayssky.mma.features.LeaderboardUtils;
import com.dayssky.mma.features.SideBarManager;
import com.dayssky.mma.features.Waypoint;
import com.dayssky.mma.features.cz.ZenithModule;
import com.dayssky.mma.features.cz.data.CharmDataRegistries;
import com.dayssky.mma.features.gamestate.GameState;
import com.dayssky.mma.util.SafeExceptionLogger;
import com.dayssky.mma.util.TickScheduler;
import com.dayssky.mma.util.Util;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStarted;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.DebugRender;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MMAClient implements ClientModInitializer {
    public static final Gson GSON = new Gson();
    public static final Logger LOGGER = LogManager.getLogger();
    public static final TickScheduler SCHEDULER = new TickScheduler();
    public static final GameState GAME_STATE = new GameState();
    public static final ModContainer MOD = (ModContainer) FabricLoader.getInstance().getModContainer("mma").orElseThrow();
    public static final Waypoint WAYPOINT = new Waypoint();
    public static final SafeExceptionLogger GLOBAL_SAFE_EH = new SafeExceptionLogger("GlobalExceptionHandler");
    public static SideBarManager SIDEBAR;
    public static ConfigHolder<MMAConfig> CONFIG;
    public static VersionChecker VERSION_CHECK;

    public static Player player() {
        return Objects.requireNonNull(Minecraft.getInstance().player);
    }

    public static ClientLevel level() {
        return (ClientLevel) player().level();
    }

    public static String playerName() {
        return player().getScoreboardName();
    }

    public static void reload() {
        MMAConfig config = (MMAConfig) CONFIG.get();
        SIDEBAR = new SideBarManager(config);
    }

    public static MMAConfig config() {
        return (MMAConfig) CONFIG.get();
    }

    public static Appearance appearance() {
        return ((MMAConfig) CONFIG.get()).appearance;
    }

    public static FeatureToggles features() {
        return ((MMAConfig) CONFIG.get()).features;
    }

    public void onInitializeClient() {
        try {
            CharmDataRegistries.init();
        } catch (IOException var2) {
            Util.sneakyThrow(var2);
        }

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("mma.json");
        Path oldConfigPath = FabricLoader.getInstance().getConfigDir().resolve("fma.json");
        if (!Files.exists(configPath) && Files.exists(oldConfigPath)) {
            try {
                Files.move(oldConfigPath, configPath);
                LOGGER.info("Migrated config from fma.json to mma.json");
            } catch (Exception e) {
                LOGGER.warn("Failed to migrate config from fma.json", e);
            }
        }

        CONFIG = MMAConfig.register();
        Keybinds.init();
        ClientLifecycleEvents.CLIENT_STARTED.register((ClientStarted) minecraft -> GLOBAL_SAFE_EH.runSafely(this::initializeAfterMC));
        ClientTickEvents.END_CLIENT_TICK.register((EndTick) mc -> GLOBAL_SAFE_EH.runSafely(() -> {
            SIDEBAR.onTick(mc);
            Keybinds.tick();
            WAYPOINT.tick();
            ContractCheck.tick();
        }));
        Debug.init();
        Commands.init();
        ZenithModule.init();
        LeaderboardUtils.init();
        WAYPOINT.init();
        EntityShieldDisabledEvent.EVENT.register((EntityShieldDisabledEvent) entity -> GLOBAL_SAFE_EH.runSafely(() -> {
            if (SIDEBAR != null) {
                SideBarManager.updateGuardTimer(entity);
            }
        }));
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register((DebugRender) context -> GLOBAL_SAFE_EH.runSafely(() -> {
            PoseStack stack = context.matrixStack();
            stack.pushPose();
            stack.translate(-context.camera().getPosition().x, -context.camera().getPosition().y, -context.camera().getPosition().z);
            WAYPOINT.render(context);
            stack.popPose();
        }));
        VERSION_CHECK = new VersionChecker((MMAConfig) CONFIG.get());
        VERSION_CHECK.init();
    }

    private void initializeAfterMC() {
        WAYPOINT.clientInit();
        reload();
    }


}
