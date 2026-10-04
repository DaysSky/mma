package com.dayssky.mma.features.gamestate;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.util.StatsUtil;
import com.dayssky.mma.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.List;

public class HexfallStateTracker implements StateTracker {
    private AABB arenaBox = new AABB(281, 146, 159, 356, 170, 85);
    private final long startTime;
    private final HexfallStateTracker.Data data;
    private long rutenStartTime;
    private long hyceneaStartTime;
    private boolean hasBeatRuten = false;
    private boolean hasBeatHycenea = false;
    private List<String> fightPlayers = Collections.emptyList();

    public HexfallStateTracker() {
        this.data = new HexfallStateTracker.Data();
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
        String raw = message.getString();
        if (raw.contains("Ru'Ten")) {
            this.rutenStartTime = Util.now();
            MMAClient.LOGGER.info("Ruten start");
        } else if (raw.contains("Hycenea")) {
            this.hyceneaStartTime = Util.now();
            MMAClient.LOGGER.info("Hycenea start");
        }
    }

    @Override
    public void onTick() {

    }

    @Override
    public void onWorldTick() {
//        MMAClient.LOGGER.info("servertick awa");

        if (this.hyceneaStartTime >= 5 && this.fightPlayers.isEmpty()) {
            MMAClient.LOGGER.info("Checking players");
            fightPlayers = MMAClient.level().getEntitiesOfClass(Player.class, arenaBox).stream().map(Entity::getStringUUID).toList();
            MMAClient.LOGGER.info("Players: {}", fightPlayers);
        }
    }

    @Override
    public void onChatMessage(Component message) {
        String raw = message.getString();
        if (raw.isEmpty() || (raw.charAt(0) == '<')) {
            return;
        }
//            AABB boundingBox = MMAClient.player().getBoundingBox().inflate(3);
//            Stream<String> players = MMAClient.level().getEntitiesOfClass(Player.class, boundingBox).stream().map(Entity::getStringUUID);
//            MMAClient.LOGGER.info("Players in range: {}", players.toList());
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
