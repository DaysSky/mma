package com.floweytf.fma.features.gamestate;

import com.floweytf.fma.util.StatsUtil;
import net.minecraft.network.chat.Component;

import static com.floweytf.fma.util.Util.now;

/**
 * Hexfall State Diagram
 * <pre>
 * {@code
 * digraph G {
 *     START -> RUTEN_CUTSCENE [label="msg.cutscene.ruten"]
 *     RUTEN_CUTSCENE -> RUTEN_START [label="title.ruten"]
 *     START -> RUTEN_START [label="title.ruten"]
 *     RUTEN_START -> RUTEN_SURGING_1 [label="msg.surging.1"]
 *     RUTEN_SURGING_1 -> RUTEN_SURGING_2 [label="msg.surging.2"]
 *     RUTEN_SURGING_2 -> RUTEN_WON [label="msg.death.ruten"]
 *     RUTEN_WON -> HYC_CUTSCENE [label="msg.cutscene.hyc"]
 *     HYC_CUTSCENE -> HYC_START [label="title.hyc"]
 *     START -> HYC_START [label="title.hyc"]
 *     HYC_START -> HYC_VOODOO [label="msg.voodoo"]
 *     HYC_VOODOO -> HYC_TOTEMIC [label="msg.totemic"]
 *     HYC_TOTEMIC -> HYC_PLATFORM [label="msg.platform"]
 *     HYC_PLATFORM -> HYC_MINI_1 [label="msg.mini.1"]
 *     HYC_MINI_1 -> HYC_CASCADING [label="msg.cascading"]
 *     HYC_CASCADING -> HYC_CHAINS [label="msg.chains"]
 *     HYC_CHAINS -> HYC_PLATFORM_VOODOO [label="msg.platform_voodoo"]
 *     HYC_PLATFORM_VOODOO -> HYC_MINI_2 [label="msg.mini.2"]
 *     HYC_MINI_2 -> HYC_8_VOODOO [label="msg.8_voodoo"]
 *
 *     HYC_ANY -> WIN [label="msg.win"]
 * }
 * }</pre>
 */
public class HexfallStateTracker implements StateTracker {
    public static class Data {
        public int chestCount = 0;

        @StatsUtil.Time
        public int clearTime = -1;

        // Ru'Ten
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

    private long rutenStartTime;

    @Override
    public void onTitle(Component message) {
        final var raw = message.getString();

        if (raw.contains("Ru'Ten")) {
            rutenStartTime = now();
        }
    }
}
