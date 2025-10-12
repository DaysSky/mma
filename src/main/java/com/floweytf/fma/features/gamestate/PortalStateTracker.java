package com.floweytf.fma.features.gamestate;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig;
import com.floweytf.fma.Graphics;
import com.floweytf.fma.util.ChatUtil;
import com.floweytf.fma.util.FormatUtil;
import com.floweytf.fma.util.StatsUtil;
import com.floweytf.fma.util.Util;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PortalStateTracker implements StateTracker {
    private final long startTime;
    private final PortalStateTracker.Data data;
    private long nodesSplit;
    private long startBossSplit;
    private long phase1Split;
    private long phase2Split;
    private boolean enteredBoss = false;
    private boolean isCubeAlive = false;
    private boolean hasWon = false;

    public PortalStateTracker() {
        this.data = new PortalStateTracker.Data();
        this.startTime = Util.now();
    }

    private int logTime(String key, boolean send, long start, long deltaBegin, long deltaEnd, long... entries) {
        return StatsUtil.logTime("timer.fma.portal." + key, send, start, deltaBegin, deltaEnd, entries);
    }

    @Override
    public void onLeave() {
        if (FMAClient.features().enableTimerAndStats) {
            if (!this.hasWon) {
                ChatUtil.send(Component.translatable("stat.fma.portal.fail"));
                this.data.send();
            }
        }
    }

    @Override
    public void onChatMessage(Component message) {
        if (FMAClient.features().enableTimerAndStats) {
            FMAConfig.Portal portalCfg = FMAClient.config().portal;
            String raw = message.getString();
            if (raw.length() >= 12) {
                String var4 = raw.substring(0, 12);
                switch (var4) {
                    case "Doorway prot":
                        this.nodesSplit = Util.now();
                        this.data.nodesSplit = this.logTime("nodes", portalCfg.nodeSplit, this.startTime, this.startTime, this.nodesSplit);
                        break;
                    case "[Iota] INTRU":
                        this.enteredBoss = true;
                        this.startBossSplit = Util.now();
                        this.data.startBossSplit = this.logTime("startBoss", portalCfg.startBossSplit, this.startTime, this.nodesSplit, this.startBossSplit);
                        break;
                    case "[Iota] DAMAG":
                        this.phase1Split = Util.now();
                        this.data.phase1Split = this.logTime("phase1", portalCfg.phase1Split, this.startTime, this.startBossSplit, this.phase1Split);
                        break;
                    case "[Iota] DAMA9":
                        this.phase2Split = Util.now();
                        this.data.phase2Split = this.logTime("phase2", portalCfg.phase2Split, this.startTime, this.phase1Split, this.phase2Split);
                        break;
                    case "[Iota] DESTR":
                        long phase3Split = Util.now();
                        this.data.phase3Split = this.logTime("phase3", portalCfg.phase3Split, this.startTime, this.phase2Split, phase3Split);
                        this.data.totalTime = (int) (phase3Split - this.startTime);
                        this.data.bossSplit = this.logTime(
                                "boss", portalCfg.bossSplit, this.startTime, this.startBossSplit, phase3Split, this.phase1Split, this.phase2Split, phase3Split
                        );
                        this.data.send();
                        this.hasWon = true;
                }
            }
        }
    }

    @Override
    public void onActionBar(Component message) {
        if (FMAClient.features().enableTimerAndStats) {
            String raw = message.getString();
            if (raw.contains("S.O.U.L. Collected")) {
                String[] parts = raw.split(" : ");
                if (parts.length != 2) {
                    ChatUtil.sendDebug("soul count parsing fail");
                    return;
                }

                String counterText = parts[1];
                if (!counterText.contains("/")) {
                    ChatUtil.sendDebug("soul count parsing fail");
                    return;
                }

                this.data.soulCount = Integer.parseInt(counterText.substring(0, counterText.indexOf("/")));
                if (this.data.soulCount >= 350 && this.data.soulTime == -1) {
                    this.data.soulTime = this.logTime("souls", FMAClient.config().portal.soulsSplit, this.startTime, this.startTime, Util.now());
                }
            } else if (raw.contains("total chests")) {
                this.data.chestCount = Integer.parseInt(raw.split(" ")[0]);
            }
        }
    }

    @Override
    public void onTick() {
        if (this.enteredBoss) {
            this.isCubeAlive = false;
            FMAClient.level()
                    .entitiesForRendering()
                    .forEach(
                            entity -> {
                                if (FMAClient.config().portal.enableIotaFix
                                        && entity.getName().getString().contains("Iota")
                                        && !entity.isInvisible()
                                        && entity.getPosition(0.0F).y < 88.0) {
                                    entity.moveTo(entity.getX(), 89.0, entity.getZ());
                                }

                                if (entity instanceof ArmorStand armorStand) {
                                    ItemStack slot = armorStand.getItemBySlot(EquipmentSlot.HEAD);
                                    if (slot.isEmpty()) {
                                        return;
                                    }

                                    if (slot.getItem() != Items.PLAYER_HEAD) {
                                        return;
                                    }

                                    if (slot.getOrCreateTag().toString().contains("eyJ0ZXh0dXJlcyI")) {
                                        this.isCubeAlive = true;
                                    }
                                }
                            }
                    );
        }
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (FMAClient.config().portal.enablePortalButtonIndicator) {
            int r = this.isCubeAlive ? 1 : 0;
            int g = this.isCubeAlive ? 0 : 1;
            VertexConsumer buffer = Objects.requireNonNull(context.consumers()).getBuffer(Graphics.OUTLINE_BOX);
            PoseStack pose = context.matrixStack();
            if (this.enteredBoss) {
                LevelRenderer.renderLineBox(pose, buffer, 1057.0, 87.0, 1388.0, 1058.0, 88.0, 1389.0, r, g, 0.0F, 1.0F);
                LevelRenderer.renderLineBox(pose, buffer, 1005.0, 87.0, 1388.0, 1006.0, 88.0, 1389.0, r, g, 0.0F, 1.0F);
            }
        }
    }

    @Override
    public List<Component> getAdditionalSidebarText() {
        ArrayList<Component> parts = new ArrayList<>();
        parts.add(Component.translatable("hud.fma.sidebar.timer", new Object[]{FormatUtil.timestamp(Util.now() - this.startTime)}));
        parts.add(Component.translatable("hud.fma.sidebar.portal.chests", new Object[]{FormatUtil.numeric(this.data.chestCount)}));
        parts.add(Component.translatable("hud.fma.sidebar.portal.souls", new Object[]{FormatUtil.numeric(this.data.soulCount)}));
        return parts;
    }

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
}
