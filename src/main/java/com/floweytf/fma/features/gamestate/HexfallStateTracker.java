package com.floweytf.fma.features.gamestate;

import com.floweytf.fma.util.StatsUtil;
import com.floweytf.fma.util.Util;
import net.minecraft.network.chat.Component;

public class HexfallStateTracker implements StateTracker {
    private long rutenStartTime;

    @Override
    public void onTitle(Component message) {
        String raw = message.getString();
        if (raw.contains("Ru'Ten")) {
            this.rutenStartTime = Util.now();
        }
    }

    public static class Data {
        public int chestCount = 0;
        @StatsUtil.Time
        public int clearTime = -1;
        @StatsUtil.Detail
        @StatsUtil.Time
        public int rutenTime = -1;
        @StatsUtil.Detail
        @StatsUtil.Time
        public int rutenPhase1Time = -1;
        @StatsUtil.Detail
        @StatsUtil.Time
        public int rutenPhase2Time = -1;
        @StatsUtil.Detail
        @StatsUtil.Time
        public int rutenPhase3Time = -1;
        public int reincarnMunched = 0;

        public void send() {
            StatsUtil.dumpStats("stat.fma.hexfall", this);
        }
    }
}
