package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig;
import com.floweytf.fma.util.NBTUtil;
import com.floweytf.fma.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import static com.floweytf.fma.util.FormatUtil.join;
import static com.floweytf.fma.util.FormatUtil.literal;
import static com.floweytf.fma.util.FormatUtil.numeric;
import static com.floweytf.fma.util.FormatUtil.withColor;
import static net.minecraft.network.chat.Component.translatable;

public class SideBarManager {
    private static final Map<String, String> IP_TO_SHORTHAND = Map.of(
        "monumenta-12.playmonumenta.com", "playmonumenta.com (m12)",
        "monumenta-13.playmonumenta.com", "playmonumenta.com (m13)",
        "monumenta-17.playmonumenta.com", "playmonumenta.com (m17)"
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
    private final Component title;
    private final int textColor;
    private final int altColor;
    private final int errorColor;
    private final List<Component> situationalText = new ArrayList<>();
    private List<Component> builtinText = List.of();
    private static int lastBlockedTicks;

    public static void updateGuardTimer(Player player) {
        if (player.getMainHandItem().getItem().equals(Items.SHIELD)) {
            lastBlockedTicks = 6 * 20;
        } else if (player.getOffhandItem().getItem().equals(Items.SHIELD)) {
            lastBlockedTicks = 4 * 20;
        }
    }

    public SideBarManager(FMAConfig config) {
        title = join(
            literal("     "),
            withColor("[", config.appearance.bracketColor),
            withColor(config.appearance.tagText, config.appearance.tagColor).withStyle(ChatFormatting.BOLD),
            withColor("] ", config.appearance.bracketColor),
            literal("     ")
        );
        textColor = config.appearance.textColor;
        altColor = config.appearance.altTextColor;
        errorColor = config.appearance.errorColor;

        // Simulate a tick to avoid bug
        onTick(Minecraft.getInstance());
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
        return (int) player.level().getEntities(player, AABB.ofSize(
            player.position().subtract(radius, radius, radius),
            2 * radius, 2 * radius, 2 * radius
        )).stream().filter(entity -> {
            if (IGNORED_ENTITIES.contains(entity.getType())) {
                return false;
            }

            return entity.position().distanceToSqr(player.position()) <= radius * radius;
        }).count();
    }

    private void updateSituational(Player player) {
        // obtain gear data
        final var enchants = NBTUtil.getAllEnchants(player);
        final var reflexesLevel = enchants.getOrDefault("Reflexes", 0);
        final var etherealLevel = enchants.getOrDefault("Ethereal", 0);
        final var tempoLevel = enchants.getOrDefault("Tempo", 0);
        final var poiseLevel = enchants.getOrDefault("Poise", 0);
        final var steadFastLevel = enchants.getOrDefault("Steadfast", 0);
        final var secondWindLevel = enchants.getOrDefault("Second Wind", 0);
        final var cloakedLevel = enchants.getOrDefault("Cloaked", 0);
        final var guardLevel = enchants.getOrDefault("Guard", 0);

        final var playerHpPerc = player.getHealth() / player.getMaxHealth();

        if (reflexesLevel > 0) {
            if (countEnemyInRadius(player, 8) >= 4) {
                situationalText.add(translatable(
                    "hud.fma.sidebar.reflexes_active",
                    numeric(20 * etherealLevel)
                ));
            } else {
                situationalText.add(translatable(
                    "hud.fma.sidebar.reflexes_inactive",
                    withColor("Inactive", errorColor)
                ));
            }
        }

        if (etherealLevel > 0) {
            if (lastHitTicks < 2 * 20) {
                situationalText.add(translatable(
                    "hud.fma.sidebar.ethereal_active",
                    numeric(20 * etherealLevel)
                ));
            } else {
                situationalText.add(translatable(
                    "hud.fma.sidebar.ethereal_inactive",
                    withColor("Inactive", errorColor)
                ));
            }
        }

        if (tempoLevel > 0) {
            if (lastHitTicks > 4 * 20) {
                situationalText.add(translatable(
                    "hud.fma.sidebar.tempo_active",
                    numeric(20 * tempoLevel)
                ));
            } else {
                situationalText.add(translatable(
                    "hud.fma.sidebar.tempo_inactive",
                    withColor("Inactive", errorColor)
                ));
            }
        }

        if (poiseLevel > 0) {
            if (player.getHealth() > 0.9 * player.getMaxHealth()) {
                situationalText.add(translatable(
                    "hud.fma.sidebar.poise_active",
                    numeric(20 * poiseLevel)
                ));
            } else if (player.getHealth() > 0.7 * player.getMaxHealth()) {
                situationalText.add(translatable(
                    "hud.fma.sidebar.poise_active",
                    numeric(10 * poiseLevel)
                ));
            } else {
                situationalText.add(translatable(
                    "hud.fma.sidebar.poise_inactive",
                    withColor("Inactive", errorColor)
                ));
            }
        }

        if (steadFastLevel > 0) {
            situationalText.add(translatable(
                "hud.fma.sidebar.steadfast_active",
                numeric(String.format("%.3f", Mth.clamp((1 - playerHpPerc) * 0.33, 0, 20)))
            ));
        }

        if (secondWindLevel > 0) {
            if (player.getHealth() < 0.5 * player.getMaxHealth()) {
                situationalText.add(translatable(
                    "hud.fma.sidebar.second_wind_active",
                    numeric(String.format("%.3f", 100 - 100 * Math.pow(0.9, secondWindLevel)))
                ));
            } else {
                situationalText.add(translatable(
                    "hud.fma.sidebar.second_wind_inactive",
                    withColor("Inactive", errorColor)
                ));
            }
        }

        if (cloakedLevel > 0) {
            if (countEnemyInRadius(player, 5) <= 2) {
                situationalText.add(translatable(
                    "hud.fma.sidebar.cloaked_active",
                    numeric(20 * etherealLevel)
                ));
            } else {
                situationalText.add(translatable(
                    "hud.fma.sidebar.cloaked_inactive",
                    withColor("Inactive", errorColor)
                ));
            }
        }

        if (guardLevel > 0) {
            if (lastBlockedTicks > 0) {
                situationalText.add(translatable(
                    "hud.fma.sidebar.guard_active",
                    numeric(guardLevel * 20)
                ));
            } else {
                situationalText.add(translatable(
                    "hud.fma.sidebar.guard_inactive",
                    withColor("Inactive", errorColor)
                ));
            }
        }
    }

    public void onTick(Minecraft mc) {
        final var config = FMAClient.features();

        if (mc.player != null) {
            updateHitTimer(mc.player);
            situationalText.clear();
            if (config.sidebarToggles.enable && config.sidebarToggles.situationals) {
                updateSituational(mc.player);
            }
        }

        builtinText = new ArrayList<>();

        final var raw = Optional.ofNullable(mc.gui.getTabList().header).map(Component::getString).orElse("");
        final var parts = Util.match(MATCH_ANGLE_BRACKET, raw, 1);

        final var shard = parts.size() >= 2 ? parts.get(1) : null;

        if (config.sidebarToggles.enableProxy) {
            final var text = parts.isEmpty() ? withColor("unknown", errorColor) : withColor(parts.get(0), altColor);
            builtinText.add(translatable("hud.fma.sidebar.proxy", text));
        }

        if (config.sidebarToggles.enableShard) {
            final var text = parts.size() < 2 ? withColor("unknown", errorColor) : withColor(shard, altColor);
            builtinText.add(translatable("hud.fma.sidebar.shard", text));
        }

        if (config.sidebarToggles.enableIp) {
            final var conn = Minecraft.getInstance().getConnection();

            if (conn != null && conn.getServerData() != null) {
                var ip = conn.getServerData().ip;

                if (config.sidebarToggles.enableIpElision) {
                    ip = IP_TO_SHORTHAND.getOrDefault(ip, ip);
                }

                builtinText.add(translatable("hud.fma.sidebar.ip", withColor(ip, altColor)));
            }
        }
    }

    public void render(Minecraft mc, GuiGraphics graphics) {
        final var font = mc.fontFilterFishy;
        final var lines = Stream.of(
            builtinText.stream(),
            situationalText.stream(),
            FMAClient.GAME_STATE.getAdditionalSidebarText().stream()
        ).flatMap(x -> x).toList();

        final var width = Math.max(font.width(title), lines.stream().mapToInt(font::width).max().orElse(0));
        // okay render everything, very stupid blah blah blah
        final int height = font.lineHeight * (lines.size() + 1);

        final int sw = mc.getWindow().getGuiScaledWidth();
        final int sh = mc.getWindow().getGuiScaledHeight();
        final int startX = sw - width - 2;
        final int startY = (sh - height) / 2;

        graphics.fill(startX - 2, startY - 2, sw, startY + height + 2, mc.options.getBackgroundColor(0.3F));

        graphics.drawCenteredString(font, title, startX + width / 2, startY, textColor);
        int textY = startY + font.lineHeight;

        for (final Component line : lines) {
            graphics.drawString(font, line, startX, textY, textColor);
            textY += font.lineHeight;
        }
    }
}
