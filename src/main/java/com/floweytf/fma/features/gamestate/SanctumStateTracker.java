package com.floweytf.fma.features.gamestate;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.util.ChatUtil;
import com.floweytf.fma.util.StatsUtil;
import net.minecraft.network.chat.Component;

import static com.floweytf.fma.util.Util.now;

public class SanctumStateTracker implements StateTracker {
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

    private final long startTime;
    private final Data data;
    private long startBossSplit;
    private long daggersSplit;
    private boolean hasWon = false;

    public SanctumStateTracker() {
        data = new Data();
        startTime = now();
    }

    private int logTime(String key, boolean send, long start, long deltaBegin, long deltaEnd, long... entries) {
        return StatsUtil.logTime("timer.fma.portal." + key, send, start, deltaBegin, deltaEnd, entries);
    }

    @Override
    public void onLeave() {
        if (!FMAClient.features().enableTimerAndStats) {
            return;
        }

        if (!hasWon) {
            ChatUtil.send(Component.translatable("stat.fma.ruin.fail"));
            data.send();
        }
    }

    @Override
    public void onChatMessage(Component message) {
        if (!FMAClient.features().enableTimerAndStats) {
            return;
        }

        final var ruinCfg = FMAClient.config().ruin;

        final var raw = message.getString();

        if (raw.length() < 12)
            return;

        switch (raw.substring(0, 12)) {
        case "[Samwell] We":
            startBossSplit = now();
            data.startBossSplit = logTime("startBoss", ruinCfg.startBossSplit, startTime, startTime, startBossSplit);
            break;
        case "[Samwell] I'":
            daggersSplit = now();
            data.daggersSplit = logTime("dagger", ruinCfg.daggerSplit, startTime, startBossSplit, daggersSplit);
            break;
        case "[Samwell] I.":
            long dpsSplit = now();
            data.dpsSplit = logTime("dps", ruinCfg.dpsSplit, startTime, daggersSplit, dpsSplit);
            data.totalTime = (int) (dpsSplit - startTime);
            data.bossSplit = logTime(
                "boss",
                ruinCfg.bossSplit,
                startTime,
                startBossSplit,
                dpsSplit,
                daggersSplit,
                dpsSplit
            );
            hasWon = true;
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
        if (raw.contains("Masked Killed")) {
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
}
