package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig;
import com.floweytf.fma.FMAConfig.FeatureToggles;
import com.floweytf.fma.mixin.MultiPlayerGameModeMixin;
import com.floweytf.fma.util.ChatUtil;
import com.floweytf.fma.util.FormatUtil;
import com.floweytf.fma.util.NBTUtil;
import com.floweytf.fma.util.SafeExceptionLogger;
import com.floweytf.fma.util.Util;
import com.mojang.datafixers.types.Type.Continue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class SideBarManager {
    private static final SafeExceptionLogger EXCEPTION_LOGGER = new SafeExceptionLogger("SideBarManager");
    private static final Map<String, String> IP_TO_SHORTHAND = Map.of(
            "monumenta-12.playmonumenta.com",
            "playmonumenta.com (m12)",
            "monumenta-13.playmonumenta.com",
            "playmonumenta.com (m13)",
            "monumenta-17.playmonumenta.com",
            "playmonumenta.com (m17)"
    );
    private static final Pattern MATCH_ANGLE_BRACKET = Pattern.compile("<([a-z0-9-]+)>");
    private static final List<EntityType<?>> IGNORED_ENTITIES = List.of(
            EntityType.AXOLOTL,
            EntityType.GLOW_ITEM_FRAME,
            EntityType.VILLAGER,
            EntityType.ARMOR_STAND,
            EntityType.POTION,
            EntityType.PAINTING,
            EntityType.EXPERIENCE_ORB,
            EntityType.ITEM,
            EntityType.ARROW,
            EntityType.SNOWBALL,
            EntityType.TRIDENT,
            EntityType.ITEM_FRAME,
            EntityType.PARROT,
            EntityType.FOX
    );
    private static float lastTickHp;
    private static float lastTickAbsorb;
    private static int lastHitTicks;
    private static int lastBlockedTicks;
    private final Component title;
    private final int textColor;
    private final int altColor;
    private final int errorColor;
    private final List<Component> situationalText = new ArrayList<>();
    private List<Component> builtinText = List.of();
    private static double playerX;
    private static double playerY;
    private static double playerZ;
    private static boolean isInZenithArea;

    @Nullable
    public static String currentShard = null;

    public SideBarManager(FMAConfig config) {
        this.title = FormatUtil.join(
                FormatUtil.literal("     "),
                FormatUtil.withColor("[", config.appearance.bracketColor),
                FormatUtil.withColor(config.appearance.tagText, config.appearance.tagColor).withStyle(ChatFormatting.BOLD),
                FormatUtil.withColor("] ", config.appearance.bracketColor),
                FormatUtil.literal("     ")
        );
        this.textColor = config.appearance.textColor;
        this.altColor = config.appearance.altTextColor;
        this.errorColor = config.appearance.errorColor;
        this.onTick(Minecraft.getInstance());
    }

    public static void updateGuardTimer(Player player) {
        if (player.getMainHandItem().getItem().equals(Items.SHIELD)) {
            lastBlockedTicks = 120;
        } else if (player.getOffhandItem().getItem().equals(Items.SHIELD)) {
            lastBlockedTicks = 80;
        }
    }

    private void updateHitTimer(@Nullable Player player) {
        if (player == null) {
            lastHitTicks = 0;
            lastBlockedTicks = 0;
        } else {
            lastHitTicks++;
            lastBlockedTicks--;
            if (lastTickAbsorb > player.getAbsorptionAmount() || lastTickHp > player.getHealth()) {
                lastHitTicks = 0;
            }

            lastTickHp = player.getHealth();
            lastTickAbsorb = player.getAbsorptionAmount();
        }
    }

    private int countEnemyInRadius(Player player, float radius) {
        return (int) player.level()
                .getEntities(player, AABB.ofSize(player.position().subtract(radius, radius, radius), 2.0F * radius, 2.0F * radius, 2.0F * radius))
                .stream()
                .filter(entity -> IGNORED_ENTITIES.contains(entity.getType()) ? false : entity.position().distanceToSqr(player.position()) <= radius * radius)
                .count();
    }

    private void updateSituational(Player player) {
        Map<String, Integer> enchants = NBTUtil.getAllEnchants(player);
        Integer reflexesLevel = enchants.getOrDefault("Reflexes", 0);
        Integer etherealLevel = enchants.getOrDefault("Ethereal", 0);
        Integer tempoLevel = enchants.getOrDefault("Tempo", 0);
        Integer poiseLevel = enchants.getOrDefault("Poise", 0);
        Integer steadFastLevel = enchants.getOrDefault("Steadfast", 0);
        Integer secondWindLevel = enchants.getOrDefault("Second Wind", 0);
        Integer cloakedLevel = enchants.getOrDefault("Cloaked", 0);
        Integer guardLevel = enchants.getOrDefault("Guard", 0);
        float playerHpPerc = player.getHealth() / player.getMaxHealth();
        if (reflexesLevel > 0) {
            if (this.countEnemyInRadius(player, 8.0F) >= 4) {
                this.situationalText.add(Component.translatable("hud.fma.sidebar.reflexes_active", new Object[]{FormatUtil.numeric(20 * etherealLevel)}));
            } else {
                this.situationalText
                        .add(Component.translatable("hud.fma.sidebar.reflexes_inactive", new Object[]{FormatUtil.withColor("Inactive", this.errorColor)}));
            }
        }

        if (etherealLevel > 0) {
            if (lastHitTicks < 40) {
                this.situationalText.add(Component.translatable("hud.fma.sidebar.ethereal_active", new Object[]{FormatUtil.numeric(20 * etherealLevel)}));
            } else {
                this.situationalText
                        .add(Component.translatable("hud.fma.sidebar.ethereal_inactive", new Object[]{FormatUtil.withColor("Inactive", this.errorColor)}));
            }
        }

        if (tempoLevel > 0) {
            if (lastHitTicks > 80) {
                this.situationalText.add(Component.translatable("hud.fma.sidebar.tempo_active", new Object[]{FormatUtil.numeric(20 * tempoLevel)}));
            } else {
                this.situationalText.add(Component.translatable("hud.fma.sidebar.tempo_inactive", new Object[]{FormatUtil.withColor("Inactive", this.errorColor)}));
            }
        }

        if (poiseLevel > 0) {
            if (player.getHealth() > 0.9 * player.getMaxHealth()) {
                this.situationalText.add(Component.translatable("hud.fma.sidebar.poise_active", new Object[]{FormatUtil.numeric(20 * poiseLevel)}));
            } else if (player.getHealth() > 0.7 * player.getMaxHealth()) {
                this.situationalText.add(Component.translatable("hud.fma.sidebar.poise_active", new Object[]{FormatUtil.numeric(10 * poiseLevel)}));
            } else {
                this.situationalText.add(Component.translatable("hud.fma.sidebar.poise_inactive", new Object[]{FormatUtil.withColor("Inactive", this.errorColor)}));
            }
        }

        if (steadFastLevel > 0) {
            this.situationalText
                    .add(
                            Component.translatable(
                                    "hud.fma.sidebar.steadfast_active",
                                    new Object[]{FormatUtil.numeric(String.format("%.3f", Mth.clamp((1.0F - playerHpPerc) * 0.33, 0.0, 20.0)))}
                            )
                    );
        }

        if (secondWindLevel > 0) {
            if (player.getHealth() < 0.5 * player.getMaxHealth()) {
                this.situationalText
                        .add(
                                Component.translatable(
                                        "hud.fma.sidebar.second_wind_active",
                                        new Object[]{FormatUtil.numeric(String.format("%.3f", 100.0 - 100.0 * Math.pow(0.9, secondWindLevel.intValue())))}
                                )
                        );
            } else {
                this.situationalText
                        .add(Component.translatable("hud.fma.sidebar.second_wind_inactive", new Object[]{FormatUtil.withColor("Inactive", this.errorColor)}));
            }
        }

        if (cloakedLevel > 0) {
            if (this.countEnemyInRadius(player, 5.0F) <= 2) {
                this.situationalText.add(Component.translatable("hud.fma.sidebar.cloaked_active", new Object[]{FormatUtil.numeric(20 * etherealLevel)}));
            } else {
                this.situationalText
                        .add(Component.translatable("hud.fma.sidebar.cloaked_inactive", new Object[]{FormatUtil.withColor("Inactive", this.errorColor)}));
            }
        }

        if (guardLevel > 0) {
            if (lastBlockedTicks > 0) {
                this.situationalText.add(Component.translatable("hud.fma.sidebar.guard_active", new Object[]{FormatUtil.numeric(guardLevel * 20)}));
            } else {
                this.situationalText.add(Component.translatable("hud.fma.sidebar.guard_inactive", new Object[]{FormatUtil.withColor("Inactive", this.errorColor)}));
            }
        }
    }

    public void onTick(Minecraft mc) {
        EXCEPTION_LOGGER.runSafely(() -> {
            FeatureToggles config = FMAClient.features();
            if (mc.player != null) {
                this.updateHitTimer(mc.player);
                this.situationalText.clear();
                if (config.sidebarToggles.enable && config.sidebarToggles.situationals) {
                    this.updateSituational(mc.player);
                }
                this.updateXYZ(mc.player);
            }

            this.builtinText = new ArrayList<>();
            String raw = Optional.ofNullable(mc.gui.getTabList().header).<String>map(Component::getString).orElse("");
            List<String> parts = Util.match(MATCH_ANGLE_BRACKET, raw, 1);
            String shard = parts.size() >= 2 ? parts.get(1) : null;
            currentShard = shard;
            if (config.sidebarToggles.enableProxy) {
                MutableComponent text = parts.isEmpty() ? FormatUtil.withColor("unknown", this.errorColor) : FormatUtil.withColor(parts.get(0), this.altColor);
                this.builtinText.add(Component.translatable("hud.fma.sidebar.proxy", new Object[]{text}));
            }

            if (config.sidebarToggles.enableShard) {
                MutableComponent text = parts.size() < 2 ? FormatUtil.withColor("unknown", this.errorColor) : FormatUtil.withColor(shard, this.altColor);
                this.builtinText.add(Component.translatable("hud.fma.sidebar.shard", new Object[]{text}));
            }

            if (config.sidebarToggles.enableIp) {
                ClientPacketListener conn = Minecraft.getInstance().getConnection();
                if (conn != null && conn.getServerData() != null) {
                    String ip = conn.getServerData().ip;
                    if (config.sidebarToggles.enableIpElision) {
                        ip = IP_TO_SHORTHAND.getOrDefault(ip, ip);
                    }

                    this.builtinText.add(Component.translatable("hud.fma.sidebar.ip", new Object[]{FormatUtil.withColor(ip, this.altColor)}));
                }
            }
            if (config.contractCheck) {
                final boolean newZenith = inZenithArea();
                if (isInZenithArea != newZenith) {
                    isInZenithArea = newZenith;
                    if (isInZenithArea && mc.player != null && mc.player.experienceLevel <= FMAClient.config().features.czContractThreshold) {

                        mc.level.playSound(mc.player, mc.player, SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 2.0f, 0.1f);
                        ChatUtil.sendWarn(Component.literal(new FMAConfig().features.contractCheckText));
                    }
                }
            }
        });
    }

    private void updateXYZ(Player player) {
        playerX = player.getX();
        playerY = player.getY();
        playerZ = player.getZ();
    }

    private static boolean inZenithArea() {
        double minX = Math.min(71, 33);
        double maxX = Math.max(71, 33);
        double minY = Math.min(14, 30);
        double maxY = Math.max(14, 30);
        double minZ = Math.min(-1396, -1410);
        double maxZ = Math.max(-1396, -1410);

        return playerX >= minX && playerX <= maxX && playerY >= minY && playerY <= maxY && playerZ >= minZ && playerZ <= maxZ;

    }

    public void render(Minecraft mc, GuiGraphics graphics) {
        EXCEPTION_LOGGER.runSafely(
                () -> {
                    Font font = mc.fontFilterFishy;
                    List<Component> lines = Stream.of(
                                    this.builtinText.stream(), this.situationalText.stream(), FMAClient.GAME_STATE.getAdditionalSidebarText().stream()
                            )
                            .flatMap(x -> (Stream<Component>) (Stream<?>) x)
                            .toList();
                    int width = Math.max(font.width(this.title), lines.stream().mapToInt(font::width).max().orElse(0));
                    int height = 9 * (lines.size() + 1);
                    int sw = mc.getWindow().getGuiScaledWidth();
                    int sh = mc.getWindow().getGuiScaledHeight();
                    int startX = sw - width - 2;
                    int startY = (sh - height) / 2;
                    graphics.fill(startX - 2, startY - 2, sw, startY + height + 2, mc.options.getBackgroundColor(0.3F));
                    graphics.drawCenteredString(font, this.title, startX + width / 2, startY, this.textColor);
                    int textY = startY + 9;

                    for (Component line : lines) {
                        graphics.drawString(font, line, startX, textY, this.textColor);
                        textY += 9;
                    }
                }
        );
    }
}
