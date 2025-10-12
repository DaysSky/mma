package com.floweytf.fma.features.gamestate;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig;
import com.floweytf.fma.util.ChatUtil;
import com.floweytf.fma.util.StatsUtil;
import com.floweytf.fma.util.Util;
import net.minecraft.network.chat.Component;

public class VerdantStateTracker implements StateTracker {
    private final long startTime;
    private final VerdantStateTracker.Data data;
    private long startBossSplit;
    private long daggersSplit;
    private boolean hasWon = false;

    public VerdantStateTracker() {
        this.data = new VerdantStateTracker.Data();
        this.startTime = Util.now();
    }

    private int logTime(String key, boolean send, long start, long deltaBegin, long deltaEnd, long... entries) {
        return StatsUtil.logTime("timer.fma.portal." + key, send, start, deltaBegin, deltaEnd, entries);
    }

    @Override
    public void onLeave() {
        if (FMAClient.features().enableTimerAndStats) {
            if (!this.hasWon) {
                ChatUtil.send(Component.translatable("stat.fma.ruin.fail"));
                this.data.send();
            }
        }
    }

    @Override
    public void onChatMessage(Component message) {
        if (FMAClient.features().enableTimerAndStats) {
            FMAConfig.Ruin ruinCfg = FMAClient.config().ruin;
            String raw = message.getString();
            if (raw.length() >= 12) {
                String var4 = raw.substring(0, 12);
                switch (var4) {
                    case "[Samwell] We":
                        this.startBossSplit = Util.now();
                        this.data.startBossSplit = this.logTime("startBoss", ruinCfg.startBossSplit, this.startTime, this.startTime, this.startBossSplit);
                        break;
                    case "[Samwell] I'":
                        this.daggersSplit = Util.now();
                        this.data.daggersSplit = this.logTime("dagger", ruinCfg.daggerSplit, this.startTime, this.startBossSplit, this.daggersSplit);
                        break;
                    case "[Samwell] I.":
                        long dpsSplit = Util.now();
                        this.data.dpsSplit = this.logTime("dps", ruinCfg.dpsSplit, this.startTime, this.daggersSplit, dpsSplit);
                        this.data.totalTime = (int) (dpsSplit - this.startTime);
                        this.data.bossSplit = this.logTime("boss", ruinCfg.bossSplit, this.startTime, this.startBossSplit, dpsSplit, this.daggersSplit, dpsSplit);
                        this.hasWon = true;
                        this.data.send();
                }
            }
        }
    }

    @Override
    public void onActionBar(Component message) {
        if (FMAClient.features().enableTimerAndStats) {
            String raw = message.getString();
            if (raw.contains("Masked Killed")) {
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
        public int startBossSplit = -1;
        @StatsUtil.Detail
        @StatsUtil.Time
        public int bossSplit = -1;
        @StatsUtil.Detail
        @StatsUtil.Time
        public int daggersSplit = -1;
        @StatsUtil.Detail
        @StatsUtil.Time
        public int dpsSplit = -1;

        public void send() {
            StatsUtil.dumpStats("stat.fma.ruin", this);
        }
    }
}
