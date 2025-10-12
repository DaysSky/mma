package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.events.ClientReceiveSystemChatEvent;
import com.floweytf.fma.events.EventResult;
import com.floweytf.fma.util.ChatUtil;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;

public class LeaderboardUtils {
   @Nullable
   private String currLeaderboard = null;
   private BiConsumer<Integer, Integer> leaderboardConsumer = null;
   private LeaderboardUtils.State state = null;

   public LeaderboardUtils() {
      ClientReceiveSystemChatEvent.EVENT.register((ClientReceiveSystemChatEvent)text -> {
         if (this.currLeaderboard == null) {
            return EventResult.CONTINUE;
         } else {
            String raw = text.getString();

            try {
               if (this.state == LeaderboardUtils.State.WAIT_REAL_END) {
                  System.out.println("THIS STATE");
                  if (!raw.trim().isEmpty()) {
                     this.reset();
                     return EventResult.CONTINUE;
                  }
               } else if (raw.startsWith(" Leaderboard - ")) {
                  if (this.state != LeaderboardUtils.State.WAIT_START) {
                     ChatUtil.sendDebug("illegal state (" + this.state + ", START_TOKEN)");
                     this.state = LeaderboardUtils.State.WAIT_END;
                  } else {
                     this.state = LeaderboardUtils.State.WAIT_PLAYER;
                  }
               } else if (raw.startsWith("--==--") && raw.endsWith("--==--")) {
                  switch (this.state) {
                     case WAIT_START:
                        ChatUtil.sendDebug("illegal state (WAIT_START, END_TOKEN)");
                        this.reset();
                        break;
                     case WAIT_PLAYER:
                        ChatUtil.sendWarn("unknown/empty leaderboard " + this.currLeaderboard);
                        this.state = LeaderboardUtils.State.WAIT_REAL_END;
                        break;
                     case WAIT_END:
                        this.state = LeaderboardUtils.State.WAIT_REAL_END;
                  }
               } else if (this.state == LeaderboardUtils.State.WAIT_PLAYER) {
                  String[] parts = raw.split("\\s*-\\s*");
                  if (parts.length == 3) {
                     if (parts[1].equals(FMAClient.playerName())) {
                        this.leaderboardConsumer.accept(Integer.parseInt(parts[0]), Integer.parseInt(parts[2]));
                        this.state = LeaderboardUtils.State.WAIT_END;
                     }
                  } else if (parts.length > 3) {
                     ChatUtil.sendDebug("too many parts?");
                  }
               }

               return EventResult.CANCEL_CONTINUE;
            } catch (Exception var4) {
               this.reset();
               ChatUtil.sendDebug("parsing failed, check logs");
               FMAClient.LOGGER.error("parse fail '{}': {}", raw, var4);
               return EventResult.CONTINUE;
            }
         }
      });
   }

   private void reset() {
      this.currLeaderboard = null;
      this.leaderboardConsumer = null;
      this.state = null;
   }

   public void beginListen(String leaderboard, BiConsumer<Integer, Integer> handler) {
      if (this.currLeaderboard != null) {
         ChatUtil.sendWarn("leaderboard read is current ongoing, (executed commands too fast)");
      }

      this.currLeaderboard = leaderboard;
      this.leaderboardConsumer = handler;
      this.state = LeaderboardUtils.State.WAIT_START;
      ChatUtil.sendCommand(String.format("leaderboard %s %s true 1", FMAClient.playerName(), leaderboard));
      FMAClient.SCHEDULER.schedule(20, minecraft -> {
         if (this.currLeaderboard != null) {
            if (this.state == LeaderboardUtils.State.WAIT_REAL_END) {
               this.reset();
            } else {
               ChatUtil.sendWarn("server is lagging, leaderboard command did not respond (bug? " + this.state + ")");
               this.currLeaderboard = null;
               this.leaderboardConsumer = null;
            }
         }
      });
   }

   private static enum State {
      WAIT_START,
      WAIT_PLAYER,
      WAIT_END,
      WAIT_REAL_END;
   }
}
