package com.floweytf.fma.features.gamestate;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.util.ChatUtil;
import com.floweytf.fma.util.FormatUtil;
import com.floweytf.fma.util.StatsUtil;
import java.util.ArrayList;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;

import static com.floweytf.fma.util.Util.now;

public class PortalStateTracker implements StateTracker {
    public static class Data {
        @StatsUtil.Detail
        public int soulCount = 0;

        public int chestCount = 0;

        @StatsUtil.Time
        public int totalTime = -1;

        @StatsUtil.Detail
        @StatsUtil.Time
        public int soulTime = -1;

        @StatsUtil.Detail
        @StatsUtil.Time
        public int nodesSplit = -1;

        @StatsUtil.Detail
        @StatsUtil.Time
        public int startBossSplit = -1;

        @StatsUtil.Detail
        @StatsUtil.Time
        public int bossSplit = -1;

        @StatsUtil.Detail
        @StatsUtil.Time
        public int phase1Split = -1;

        @StatsUtil.Detail
        @StatsUtil.Time
        public int phase2Split = -1;

        @StatsUtil.Detail
        @StatsUtil.Time
        public int phase3Split = -1;

        public void send() {
            StatsUtil.dumpStats("stat.fma.portal", this);
        }
    }

    private final long startTime;
    private final Data data;
    private long nodesSplit;
    private long startBossSplit;
    private long phase1Split;
    private long phase2Split;
    private boolean enteredBoss = false;
    private boolean isCubeAlive = false;

    public PortalStateTracker() {
        data = new Data();
        startTime = now();
    }

    private int logTime(String key, boolean send, long start, long deltaBegin, long deltaEnd, long... entries) {
        return StatsUtil.logTime("timer.fma.portal." + key, send, start, deltaBegin, deltaEnd, entries);
    }

    private void render() {
        final var parts = new ArrayList<Component>();
        parts.add(Component.translatable("hud.fma.sidebar.timer", FormatUtil.timestamp(now() - startTime)));
        parts.add(Component.translatable("hud.fma.sidebar.portal.chests", FormatUtil.numeric(data.chestCount)));
        parts.add(Component.translatable("hud.fma.sidebar.portal.souls", FormatUtil.numeric(data.soulCount)));
        FMAClient.SIDEBAR.setAdditionalText(parts);
    }

    @Override
    public void onChatMessage(Component message) {
        if (!FMAClient.features().enableTimerAndStats) {
            return;
        }

        final var portalCfg = FMAClient.config().portal;

        final var raw = message.getString();

        if (raw.length() < 12)
            return;

        switch (raw.substring(0, 12)) {
        case "Doorway prot":
            nodesSplit = now();
            data.nodesSplit = logTime("nodes", portalCfg.nodeSplit, startTime, startTime, nodesSplit);
            break;
        case "[Iota] INTRU":
            enteredBoss = true;
            startBossSplit = now();
            data.startBossSplit = logTime("startBoss", portalCfg.startBossSplit, startTime, nodesSplit, startBossSplit);
            break;
        case "[Iota] DAMAG":
            phase1Split = now();
            data.phase1Split = logTime("phase1", portalCfg.phase1Split, startTime, startBossSplit, phase1Split);
            break;
        case "[Iota] DAMA9":
            phase2Split = now();
            data.phase2Split = logTime("phase2", portalCfg.phase2Split, startTime, phase1Split, phase2Split);
            break;
        case "[Iota] DESTR":
            long phase3Split = now();
            data.phase3Split = logTime("phase3", portalCfg.phase3Split, startTime, phase2Split, phase3Split);
            data.totalTime = (int) (phase3Split - startTime);
            data.bossSplit = logTime(
                "boss",
                portalCfg.bossSplit,
                startTime,
                startBossSplit,
                phase3Split,
                phase1Split,
                phase2Split,
                phase3Split
            );
            data.send();
            break;
        }
    }

    @Override
    public void onActionBar(Component message) {
        if (!FMAClient.features().enableTimerAndStats) {
            return;
        }

        final var raw = message.getString();
        if (raw.contains("S.O.U.L. Collected")) {
            final var parts = raw.split(" : ");
            if (parts.length != 2) {
                ChatUtil.sendDebug("soul count parsing fail");
                return;
            }

            final var counterText = parts[1];

            if (!counterText.contains("/")) {
                ChatUtil.sendDebug("soul count parsing fail");
                return;
            }

            data.soulCount = Integer.parseInt(counterText.substring(0, counterText.indexOf("/")));

            if (data.soulCount >= 350 && data.soulTime == -1) {
                data.soulTime = logTime(
                    "souls",
                    FMAClient.config().portal.soulsSplit,
                    startTime,
                    startTime,
                    now()
                );
            }
        } else if (raw.contains("total chests")) {
            data.chestCount = Integer.parseInt(raw.split(" ")[0]);
        }
    }

    @Override
    public void onTick() {
        render();

        if (!enteredBoss) {
            return;
        }

        isCubeAlive = false;

        // Stupid IOTA bug!
        // I'm sure this code will work perfectly fine, right?!
        FMAClient.level().entitiesForRendering().forEach(entity -> {
            if (FMAClient.config().portal.enableIotaFix &&
                entity.getName().getString().contains("Iota") &&
                !entity.isInvisible() &&
                entity.getPosition(0).y < 88
            ) {
                entity.moveTo(entity.getX(), 89, entity.getZ());
            }

            if (entity instanceof ArmorStand armorStand) {
                final var slot = armorStand.getItemBySlot(EquipmentSlot.HEAD);
                if (slot.isEmpty()) {
                    return;
                }

                if (slot.getItem() != Items.PLAYER_HEAD) {
                    return;
                }

                if (slot.getOrCreateTag().toString().contains("eyJ0ZXh0dXJlcyI")) {
                    isCubeAlive = true;
                }
            }
        });
    }

    // Render a box around buttons
    @Override
    public void onRender(WorldRenderContext context) {
        if (!FMAClient.config().portal.enablePortalButtonIndicator) {
            return;
        }

        int r = isCubeAlive ? 1 : 0;
        int g = isCubeAlive ? 0 : 1;
        final var buffer = Objects.requireNonNull(context.consumers()).getBuffer(RenderType.lines());
        final var pose = context.matrixStack();

        if (enteredBoss) {
            LevelRenderer.renderLineBox(pose, buffer,
                1057, 87, 1388,  // button pos
                1057 + 1, 87 + 1, 1388 + 1,
                r, g, 0, 1
            );

            LevelRenderer.renderLineBox(pose, buffer,
                1005, 87, 1388,  // button pos
                1005 + 1, 87 + 1, 1388 + 1,
                r, g, 0, 1
            );
        }
    }
}
