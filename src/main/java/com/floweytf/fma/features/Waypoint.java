package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig;
import com.floweytf.fma.Graphics;
import com.floweytf.fma.util.ChatUtil;
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

import static com.floweytf.fma.util.CommandUtil.lit;

public class Waypoint implements AbstractModule<Waypoint.Config> {
    public static final class Config {
        public boolean enable = false;
        public boolean recordChests = false;
        public boolean disableInPlots = true;
        public boolean skipBrokenChests = false;
        public List<String> disabledWorlds = new ArrayList<>();
    }

    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("fma-waypoint.json");
    private static final Codec<Map<ResourceLocation, List<BlockPos>>> CODEC = Codec.unboundedMap(
        ResourceLocation.CODEC,
        BlockPos.CODEC.listOf()
    );

    // note: only access from main thread!
    private Map<ResourceLocation, Set<BlockPos>> byWorld = new HashMap<>();
    private CompletableFuture<Void> currCompletionToken = CompletableFuture.completedFuture(null);
    private final Minecraft minecraft = Minecraft.getInstance();
    private final KeyMapping keybind = new KeyMapping(
        "key.fma.toggleChestWP",
        InputConstants.Type.MOUSE,
        GLFW.GLFW_KEY_K,
        "category.fma"
    );

    private void load() {
        if (!Files.exists(Waypoint.PATH)) {
            return;
        }

        try (final var in = Files.newBufferedReader(Waypoint.PATH)) {
            final var obj = FMAClient.GSON.fromJson(in, JsonElement.class);
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
            FMAClient.LOGGER.warn(e);
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

        final var newToken = new CompletableFuture<Void>();

        currCompletionToken.thenRunAsync(() -> {
            try {
                final var json = CODEC.encodeStart(JsonOps.INSTANCE, copy)
                    .result()
                    .orElseThrow();

                try (final var w = Files.newBufferedWriter(Waypoint.PATH)) {
                    FMAClient.GSON.toJson(json, w);
                }
            } catch (Exception e) {
                FMAClient.LOGGER.warn(e);
            }
        }).thenRunAsync(() -> newToken.complete(null), Minecraft.getInstance());

        currCompletionToken = newToken;
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
    public Config readConfigFrom(FMAConfig config) {
        return config.features.waypoint;
    }

    @Override
    public void init() {
        load();

        KeyBindingHelper.registerKeyBinding(keybind);

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
            dispatcher.register(lit("fma", lit("waypoint", lit("clear", c -> {
                byWorld.remove(c.getSource().getWorld().dimension().location());
                save();
                return 0;
            }))));
        });
    }

    @Override
    public void clientInit() {

    }

    @Override
    public void tick() {
        if (keybind.consumeClick()) {
            config().recordChests = !config().recordChests;
            // TODO: i18n
            ChatUtil.send(Component.literal("chest break recording: " + (config().recordChests ? "enabled" : "disabled")));
        }
    }

    @Override
    public void render(WorldRenderContext context) {
        if (!config().enable) {
            return;
        }

        final var consumer = Objects.requireNonNull(context.consumers()).getBuffer(Graphics.LINES);
        final var entries = byWorld.getOrDefault(FMAClient.level().dimension().location(), Set.of());

        for (final var entry : entries) {
            if(config().skipBrokenChests && context.world().getBlockState(entry).getBlock() != Blocks.CHEST) {
                continue;
            }

            int x = entry.getX();
            int y = entry.getY();
            int z = entry.getZ();
            LevelRenderer.renderLineBox(context.matrixStack(), consumer, x, y, z, x + 1, y + 1, z + 1, 0, 1, 0, 1);
        }

        context.consumers();
    }
}
