package com.dayssky.mma.features.gamestate;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.util.ChatUtil;
import com.dayssky.mma.util.FormatUtil;
import com.dayssky.mma.util.StatsUtil;
import com.dayssky.mma.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HexfallStateTracker implements StateTracker {
    private final AABB arenaBox = new AABB(281, 146, 159, 356, 170, 85);
    private final long startTime;
    private final Data data;
    private long rutenFirstStartTime;
    private long rutenStartTime;
    private long hyceneaFirstStartTime;
    private long hyceneaStartTime;
    private boolean hasBeatRuten = false;
    private boolean hasBeatHycenea = false;
    private List<String> inFightPlayerUUIDS = Collections.emptyList();
    private List<String> aliveInFightPlayerUUIDS = Collections.emptyList();

    public int reincarnationsLeft = 0;
    public boolean selfInFight = false;
    public String totemElement = null;

    public HexfallStateTracker() {
        this.data = new Data();
        this.startTime = Util.now();
    }

    private int logTime(String key, boolean send, long start, long deltaBegin, long deltaEnd, long... entries) {
        return StatsUtil.logTime("timer.mma.hexfall." + key, send, start, deltaBegin, deltaEnd, entries);
    }

    public static class Data {
        // All
        public int chestCount = 0;
        @StatsUtil.Time
        public int clearTime = -1;
        public int currentRevivesUsed = 0;

        // Ruten
        @StatsUtil.Detail
        @StatsUtil.Time
        public int rutenTime = -1;

        // Hycenea
        @StatsUtil.Detail
        @StatsUtil.Time
        public int hyceneaTime = -1;

        public void send() {
            StatsUtil.dumpStats("stat.mma.hexfall", this);
        }
    }

    @Override
    public void onTitle(Component message) {
//        String raw = message.getString();
//        if (raw.contains("Ru'Ten") && this.rutenFirstStartTime != 0) {
//            this.rutenFirstStartTime = Util.now();
//            MMAClient.LOGGER.info("Ruten time set");
//        } else if (raw.contains("Hycenea") && this.hyceneaFirstStartTime != 0) {
//            this.hyceneaFirstStartTime = Util.now();
//            MMAClient.LOGGER.info("Hycenea time set");
//        }
    }

    @Override
    public void onTick() {

    }

    @Override
    public void onWorldTick() {
//        MMAClient.LOGGER.info("servertick awa");

//        if (this.hyceneaStartTime >= 5 && this.fightPlayers.isEmpty()) {
//        }
    }

    @Override
    public void onChatMessage(Component message) {
        String raw = message.getString();
        // Return on player messages
        if (raw.isEmpty() || (raw.charAt(0) == '<')) return;

        // Hycenea start
        if (raw.contains("[Hycenea] Now I... will break thee. Link by pitiful link.")) {
            MMAClient.LOGGER.info("Hycenea started");

            if (hyceneaFirstStartTime == 0) this.hyceneaFirstStartTime = Util.now();

            this.hyceneaStartTime = Util.now();
            this.inFightPlayerUUIDS = MMAClient.level().getEntitiesOfClass(Player.class, arenaBox).stream().map(Entity::getStringUUID).toList();
            this.aliveInFightPlayerUUIDS = new ArrayList<>(this.inFightPlayerUUIDS);
            this.reincarnationsLeft = inFightPlayerUUIDS.size();

            MMAClient.LOGGER.info("Players: {}", this.inFightPlayerUUIDS);

            // Return if self is not in fight
            boolean isSelfInFight = this.inFightPlayerUUIDS.contains(MMAClient.player().getStringUUID());
            this.selfInFight = isSelfInFight;
            if (!isSelfInFight) return;
            MMAClient.LOGGER.info("Self is in fight");
        }

        // Ruten Start

        // Reincarnation
        if (raw.contains("has Reincarnated!") && this.selfInFight) {
            String deadPlayerName = raw.split(" has Reincarnated!")[0];
            Player deadPlayer = null;

            for (Player p : MMAClient.level().players()) {
                if (p.getName().getString().equals(deadPlayerName)) {
                    deadPlayer = p;
                }
            }
            MMAClient.LOGGER.info("Reincarnated: {}", deadPlayer.getStringUUID());
            this.reincarnationsLeft -= 1;
            MMAClient.LOGGER.info("Reincarnations left: {}", this.reincarnationsLeft);

            // TODO: Make a config option for custom msg
            Component reincarnationMsg = Component.translatable("stat.mma.hexfall.reincarnations_left", FormatUtil.numeric(this.reincarnationsLeft));

            // TODO: Make a config option for both of these
            ChatUtil.send(reincarnationMsg);
            Minecraft.getInstance().gui
                    .setOverlayMessage(FormatUtil.join(FormatUtil.colored(MMAClient.config().appearance.textColor).append(FormatUtil.join(reincarnationMsg))), false);
        }

        // Totemic destruction
        if (raw.contains("energy pierces your very being, making you more vulnerable to a similar attack.")) {
            String totem = raw.split(" energy pierces your very being")[0];
            MMAClient.LOGGER.info("Totem element: {}", totem);
            this.totemElement = totem;
        }
        if (raw.contains("Your vulnerability to")) {
            String element = raw.split("Your vulnerability to ")[1].split(" fades...")[0];
            MMAClient.LOGGER.info("Element {} Fades", element);
            if (Objects.equals(this.totemElement, element)) totemElement = null;
        }
    }

    @Override
    public void onPlayerDeath(Player player) {
        if (!this.selfInFight) return;

        if (player == null) return;
        MMAClient.LOGGER.info("Player: {}", player.getStringUUID());

        boolean playerWasInFight = this.inFightPlayerUUIDS.contains(player.getStringUUID());
        MMAClient.LOGGER.info("Player was in fight: {}", playerWasInFight);
        if (!playerWasInFight) return;

        this.aliveInFightPlayerUUIDS.remove(player.getStringUUID());
        MMAClient.LOGGER.info("Alive in fight: {}", this.aliveInFightPlayerUUIDS);

        if (this.aliveInFightPlayerUUIDS.isEmpty()) {
            MMAClient.LOGGER.info("No players left in fight, resetting data");
            this.inFightPlayerUUIDS = Collections.emptyList();
            this.aliveInFightPlayerUUIDS = Collections.emptyList();
            this.reincarnationsLeft = 0;
            this.hyceneaStartTime = 0;
            this.totemElement = null;
            this.selfInFight = false;
        }
    }

    @Override
    public void onActionBar(Component message) {
        if (MMAClient.config().features.enableTimerAndStats) {
            String raw = message.getString();
            if (raw.contains("total chests")) {
                this.data.chestCount = Integer.parseInt(raw.split(" ")[0]);
            }
        }
    }
}
