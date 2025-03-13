package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.events.ClientReceiveSystemChatEvent;
import com.floweytf.fma.events.EventResult;
import com.floweytf.fma.util.ChatUtil;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;

public class LeaderboardUtils {
    private enum State {
        WAIT_START,
        WAIT_PLAYER,
        WAIT_END,
        WAIT_REAL_END
    }

    @Nullable
    private String currLeaderboard = null;
    private BiConsumer<Integer, Integer> leaderboardConsumer = null;
    private State state = null;

    public LeaderboardUtils() {
        ClientReceiveSystemChatEvent.EVENT.register(text -> {
            if (currLeaderboard == null) {
                return EventResult.CONTINUE;
            }

            final var raw = text.getString();

            try {
                // Handle start token
                if (state == State.WAIT_REAL_END) {
                    System.out.println("THIS STATE");
                    if (!raw.trim().isEmpty()) {
                        reset();
                        return EventResult.CONTINUE;
                    }
                    // Assume the leaderboard is correct because why not?
                } else if (raw.startsWith(" Leaderboard - ")) {
                    if (state != State.WAIT_START) {
                        ChatUtil.sendDebug("illegal state (" + state + ", START_TOKEN)");
                        state = State.WAIT_END;
                    } else {
                        state = State.WAIT_PLAYER;
                    }
                } else if (raw.startsWith("--==--") && raw.endsWith("--==--")) {
                    switch (state) {
                    case WAIT_PLAYER:
                        ChatUtil.sendWarn("unknown/empty leaderboard " + currLeaderboard);
                        state = State.WAIT_REAL_END;
                        break;
                    case WAIT_END:
                        state = State.WAIT_REAL_END;
                        break;
                    case WAIT_START:
                        ChatUtil.sendDebug("illegal state (WAIT_START, END_TOKEN)");
                        reset();
                        break;
                    }
                } else if (state == State.WAIT_PLAYER) {
                    final var parts = raw.split("\\s*-\\s*");

                    if (parts.length == 3) {
                        if (parts[1].equals(FMAClient.playerName())) {
                            leaderboardConsumer.accept(Integer.parseInt(parts[0]), Integer.parseInt(parts[2]));
                            state = State.WAIT_END;
                        }
                    } else if (parts.length > 3) {
                        ChatUtil.sendDebug("too many parts?");
                    }
                }

                return EventResult.CANCEL_CONTINUE;
            } catch (Exception e) {
                reset();
                ChatUtil.sendDebug("parsing failed, check logs");
                FMAClient.LOGGER.error("parse fail '{}': {}", raw, e);
            }

            return EventResult.CONTINUE;
        });
    }

    private void reset() {
        currLeaderboard = null;
        leaderboardConsumer = null;
        state = null;
    }

    public void beginListen(String leaderboard, BiConsumer<Integer, Integer> handler) {
        if (currLeaderboard != null) {
            ChatUtil.sendWarn("leaderboard read is current ongoing, (executed commands too fast)");
        }

        currLeaderboard = leaderboard;
        leaderboardConsumer = handler;
        state = State.WAIT_START;

        ChatUtil.sendCommand(String.format("leaderboard %s %s true 1", FMAClient.playerName(), leaderboard));

        // 20 ticks = 1s
        FMAClient.SCHEDULER.schedule(20, minecraft -> {
            if (currLeaderboard == null)
                return;

            if (state == State.WAIT_REAL_END) {
                reset();
                return;
            }

            ChatUtil.sendWarn("server is lagging, leaderboard command did not respond (bug? " + state + ")");
            currLeaderboard = null;
            leaderboardConsumer = null;
        });
    }
}
