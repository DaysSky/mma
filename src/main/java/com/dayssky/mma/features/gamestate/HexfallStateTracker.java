package com.dayssky.mma.features.gamestate;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.util.StatsUtil;
import com.dayssky.mma.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.List;

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
    private int reincarnationsUsed;
    public boolean selfInFight = false;

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
            inFightPlayerUUIDS = MMAClient.level().getEntitiesOfClass(Player.class, arenaBox).stream().map(Entity::getStringUUID).toList();
            MMAClient.LOGGER.info("Players: {}", inFightPlayerUUIDS);
            // Return if self is not in fight
            boolean isSelfInFight = inFightPlayerUUIDS.contains(MMAClient.player().getStringUUID());
            this.selfInFight = isSelfInFight;
            if (!isSelfInFight) return;
            MMAClient.LOGGER.info("Self is in fight");
        }
        // Ruten Start

        // Reincarnation
        if (raw.contains("has Reincarnated!")) {
            String deadPlayerName = raw.split(" has Reincarnated!")[0];
            Player deadPlayer = null;

            for (Player p : MMAClient.level().players()) {
                if (p.getName().getString().equals(deadPlayerName)) {
                    deadPlayer = p;
                }
            }

            MMAClient.LOGGER.info("Reincarnated: {}", deadPlayer.getStringUUID());
        }
    }

    @Override
    public void onPlayerDeath(int playerId, Component deathMessage) {
        MMAClient.LOGGER.info("Player ID Died: {}", playerId);
        MMAClient.LOGGER.info("Death message: {}", deathMessage);
        Player playerEntity = (Player) MMAClient.level().getEntity(playerId);
        MMAClient.LOGGER.info("Player: {}", playerEntity.getStringUUID());
        boolean playerWasInFight = inFightPlayerUUIDS.contains(playerEntity.getStringUUID());
        MMAClient.LOGGER.info("Player was in fight: {}", playerWasInFight);
    }

    @Override
    public void onActionBar(Component message) {
        if (MMAClient.features().enableTimerAndStats) {
            String raw = message.getString();
            if (raw.contains("total chests")) {
                this.data.chestCount = Integer.parseInt(raw.split(" ")[0]);
            }
        }
    }
}
