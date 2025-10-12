package com.floweytf.fma;

import com.floweytf.fma.FMAConfig.Appearance;
import com.floweytf.fma.FMAConfig.FeatureToggles;
import com.floweytf.fma.debug.Debug;
import com.floweytf.fma.events.EntityShieldDisabledEvent;
import com.floweytf.fma.features.Commands;
import com.floweytf.fma.features.Keybinds;
import com.floweytf.fma.features.LeaderboardUtils;
import com.floweytf.fma.features.SideBarManager;
import com.floweytf.fma.features.Waypoint;
import com.floweytf.fma.features.cz.ZenithModule;
import com.floweytf.fma.features.cz.data.CharmDataRegistries;
import com.floweytf.fma.features.gamestate.GameState;
import com.floweytf.fma.util.SafeExceptionLogger;
import com.floweytf.fma.util.TickScheduler;
import com.floweytf.fma.util.Util;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
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

public class FMAClient implements ClientModInitializer {
   public static final Gson GSON = new Gson();
   public static final Logger LOGGER = LogManager.getLogger();
   public static final TickScheduler SCHEDULER = new TickScheduler();
   public static final LeaderboardUtils LEADERBOARD = new LeaderboardUtils();
   public static final GameState GAME_STATE = new GameState();
   public static final ModContainer MOD = (ModContainer)FabricLoader.getInstance().getModContainer("fma").orElseThrow();
   public static final Waypoint WAYPOINT = new Waypoint();
   public static final SafeExceptionLogger GLOBAL_SAFE_EH = new SafeExceptionLogger("GlobalExceptionHandler");
   public static SideBarManager SIDEBAR;
   public static ConfigHolder<FMAConfig> CONFIG;
   public static VersionChecker VERSION_CHECK;

   public static Player player() {
      return Objects.requireNonNull(Minecraft.getInstance().player);
   }

   public static ClientLevel level() {
      return (ClientLevel)player().level();
   }

   public static String playerName() {
      return player().getScoreboardName();
   }

   public static void reload() {
      FMAConfig config = (FMAConfig)CONFIG.get();
      SIDEBAR = new SideBarManager(config);
   }

   public static FMAConfig config() {
      return (FMAConfig)CONFIG.get();
   }

   public static Appearance appearance() {
      return ((FMAConfig)CONFIG.get()).appearance;
   }

   public static FeatureToggles features() {
      return ((FMAConfig)CONFIG.get()).features;
   }

   public void onInitializeClient() {
      try {
         CharmDataRegistries.init();
      } catch (IOException var2) {
         Util.sneakyThrow(var2);
      }

      CONFIG = FMAConfig.register();
      Keybinds.init();
      ClientLifecycleEvents.CLIENT_STARTED.register((ClientStarted)minecraft -> GLOBAL_SAFE_EH.runSafely(this::initializeAfterMC));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> GLOBAL_SAFE_EH.runSafely(() -> {
         SIDEBAR.onTick(mc);
         Keybinds.tick();
         WAYPOINT.tick();
      }));
      Debug.init();
      Commands.init();
      ZenithModule.init();
      WAYPOINT.init();
      EntityShieldDisabledEvent.EVENT.register((EntityShieldDisabledEvent)entity -> GLOBAL_SAFE_EH.runSafely(() -> {
         if (SIDEBAR != null) {
            SideBarManager.updateGuardTimer(entity);
         }
      }));
      WorldRenderEvents.BEFORE_DEBUG_RENDER.register((DebugRender)context -> GLOBAL_SAFE_EH.runSafely(() -> {
         PoseStack stack = context.matrixStack();
         stack.pushPose();
         stack.translate(-context.camera().getPosition().x, -context.camera().getPosition().y, -context.camera().getPosition().z);
         WAYPOINT.render(context);
         stack.popPose();
      }));
      VERSION_CHECK = new VersionChecker((FMAConfig)CONFIG.get());
      VERSION_CHECK.init();
   }

   private void initializeAfterMC() {
      WAYPOINT.clientInit();
      reload();
   }
}
