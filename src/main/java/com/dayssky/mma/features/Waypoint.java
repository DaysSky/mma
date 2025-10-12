package com.dayssky.mma.features;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.MMAConfig;
import com.dayssky.mma.Graphics;
import com.dayssky.mma.features.Waypoint.Config;
import com.dayssky.mma.util.ChatUtil;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import me.shedaniel.autoconfig.annotation.ConfigEntry.ColorPicker;
import me.shedaniel.math.Color;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

import static com.dayssky.mma.util.CommandUtil.lit;

public class Waypoint implements AbstractModule<Config> {
    public static final class Config {
        public boolean enable = false;
        public boolean recordChests = false;
        public boolean disableInPlots = true;
        public boolean skipBrokenChests = false;
        @ColorPicker
        public int color = 0x00ff00;
        public List<String> disabledWorlds = new ArrayList<>();
    }

    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("mma-waypoint.json");
    private static final Codec<Map<ResourceLocation, List<BlockPos>>> CODEC = Codec.unboundedMap(
            ResourceLocation.CODEC,
            BlockPos.CODEC.listOf()
    );

    private final Minecraft minecraft = Minecraft.getInstance();

    private final KeyMapping toggleRecordingKey = new KeyMapping(
            "key.mma.toggleChestWaypointRecording",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.mma"
    );

    private final KeyMapping toggleKey = new KeyMapping(
            "key.mma.toggleChestWaypoint",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.mma"
    );

    // note: only access from main thread!
    private Map<ResourceLocation, Set<BlockPos>> byWorld = new HashMap<>();
    private CompletableFuture<Void> currCompletionToken = CompletableFuture.completedFuture(null);

    private void synchronize(Runnable runnable) {
        CompletableFuture<Void> newToken = new CompletableFuture<>();
        currCompletionToken.thenRunAsync(runnable).thenRunAsync(() -> newToken.complete(null), Minecraft.getInstance());
        currCompletionToken = newToken;
    }

    private void load() {
        if (!Files.exists(Waypoint.PATH)) {
            return;
        }

        try (final var in = Files.newBufferedReader(Waypoint.PATH)) {
            final var obj = MMAClient.GSON.fromJson(in, JsonElement.class);
            byWorld = CODEC.decode(JsonOps.INSTANCE, obj)
                    .result()
                    .orElseThrow()
                    .getFirst()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> new HashSet<>(e.getValue())
                    ));

        } catch (Exception e) {
            MMAClient.LOGGER.warn(e);
        }
    }

    private void save() {
        Preconditions.checkState(minecraft.isSameThread());
        final Map<ResourceLocation, List<BlockPos>> copy = byWorld.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ArrayList<>(e.getValue())
                ));

        synchronize(() -> {
            try {
                final var json = CODEC.encodeStart(JsonOps.INSTANCE, copy)
                        .result()
                        .orElseThrow();

                try (final var w = Files.newBufferedWriter(Waypoint.PATH)) {
                    MMAClient.GSON.toJson(json, w);
                }
            } catch (Exception e) {
                MMAClient.LOGGER.warn(e);
            }
        });
    }

    private void add(Level level, BlockPos pos) {
        if (!config().enable || !config().recordChests) {
            return;
        }

        if (level.getBlockState(pos).getBlock() != Blocks.CHEST) {
            return;
        }

        final var dimId = level.dimension().location();

        if (config().disableInPlots && dimId.getPath().startsWith("plot")) {
            return;
        }

        if (config().disabledWorlds.contains(dimId.toString())) {
            return;
        }

        byWorld.computeIfAbsent(dimId, ignored -> new HashSet<>()).add(pos);
        save();
    }

    @Override
    public Config readConfigFrom(MMAConfig config) {
        return config.features.waypoint;
    }

    @Override
    public void init() {
        load();

        KeyBindingHelper.registerKeyBinding(toggleRecordingKey);
        KeyBindingHelper.registerKeyBinding(toggleKey);

        AttackBlockCallback.EVENT.register((player, level, interactionHand, blockPos, direction) -> {
            if (!level.isClientSide()) {
                return InteractionResult.PASS;
            }

            add(level, blockPos);
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (player.isCrouching()) {
                final var entries = byWorld.get(world.dimension().location());
                if (entries != null) {
                    entries.remove(hitResult.getBlockPos());
                }
            } else {
                add(world, hitResult.getBlockPos());
            }

            return InteractionResult.PASS;
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(lit("mma", lit("waypoint", lit("clear", c -> {
                byWorld.remove(c.getSource().getWorld().dimension().location());
                save();
                return 0;
            }), lit("reload", c -> {
                load();
                return 0;
            }))));
        });
    }

    @Override
    public void clientInit() {

    }

    @Override
    public void tick() {
        if (toggleRecordingKey.consumeClick()) {
            config().recordChests = !config().recordChests;
            // TODO: i18n
            ChatUtil.send(Component.literal("chest break recording: " + (config().recordChests ? "enabled" : "disabled")));
        }

        if (toggleKey.consumeClick()) {
            config().enable = !config().enable;
            // TODO: i18n
            ChatUtil.send(Component.literal("chest waypoints: " + (config().enable ? "enabled" : "disabled")));
        }
    }

    @Override
    public void render(WorldRenderContext context) {
        if (!config().enable) {
            return;
        }

        final var consumer = Objects.requireNonNull(context.consumers()).getBuffer(Graphics.OUTLINE_BOX);
        final var entries = byWorld.getOrDefault(MMAClient.level().dimension().location(), Set.of());

        for (final var entry : entries) {
            if (config().skipBrokenChests && context.world().getBlockState(entry).getBlock() != Blocks.CHEST) {
                continue;
            }

            int x = entry.getX();
            int y = entry.getY();
            int z = entry.getZ();

            final var color = Color.ofOpaque(config().color);
            LevelRenderer.renderLineBox(
                    context.matrixStack(),
                    consumer,
                    x, y, z, x + 1, y + 1, z + 1,
                    color.getRed() / 255f,
                    color.getGreen() / 255f,
                    color.getBlue() / 255f,
                    1
            );
        }

        context.consumers();
    }
}
