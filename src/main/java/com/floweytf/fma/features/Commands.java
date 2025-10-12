package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig;
import com.floweytf.fma.debug.Debug;
import com.floweytf.fma.util.ChatUtil;
import com.floweytf.fma.util.CommandUtil;
import com.floweytf.fma.util.FormatUtil;
import com.floweytf.fma.util.Util;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public class Commands {
   private static long timerMs = -1L;

   public static void init() {
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> {
               LiteralCommandNode<FabricClientCommandSource> fma = dispatcher.register(
                  CommandUtil.lit(
                     "fma",
                     CommandUtil.<FabricClientCommandSource>litPred(
                        "debug",
                        ignored -> FMAClient.config().features.enableDebug,
                        CommandUtil.lit("test", ignored -> {
                           ChatUtil.send(":3");
                           return 0;
                        }),
                        CommandUtil.lit("re", ignored -> {
                           FMAClient.reload();
                           return 0;
                        }),
                        CommandUtil.lit("entity", ignored -> {
                           Debug.ENTITY_DEBUG = !Debug.ENTITY_DEBUG;
                           ChatUtil.send("Entity Debug: " + Debug.ENTITY_DEBUG);
                           return 0;
                        }),
                        CommandUtil.lit("block", ignored -> {
                           Debug.BLOCK_DEBUG = !Debug.BLOCK_DEBUG;
                           ChatUtil.send("Block Debug: " + Debug.ENTITY_DEBUG);
                           return 0;
                        }),
                        CommandUtil.lit("dumpentity", context -> {
                           FMAClient.level().entitiesForRendering().forEach(e -> {
                              if (e.getEyePosition().distanceTo(FMAClient.player().getEyePosition()) < 10.0) {
                                 Debug.dumpEntityInfo(e);
                              }
                           });
                           return 0;
                        }),
                        CommandUtil.lit(
                           "dumpnbt",
                           context -> {
                              ChatUtil.send(
                                 FormatUtil.join(
                                    Component.literal("Data: "),
                                    NbtUtils.toPrettyComponent(FMAClient.player().getItemInHand(InteractionHand.MAIN_HAND).getTag())
                                 )
                              );
                              return 0;
                           }
                        ),
                        CommandUtil.lit("fakecrash", context -> {
                           FMAClient.GLOBAL_SAFE_EH.onException(new Exception(), "test");
                           return 0;
                        })
                     ),
                     CommandUtil.lit("help", ignored -> {
                        ChatUtil.send(Component.literal("Command Help").withStyle(ChatFormatting.BOLD));
                        ChatUtil.send("/cc - clear chat");
                        ChatUtil.send("/omw - shorthand for /lfg omw");
                        ChatUtil.send("/fma debug - dumps internal state, don't use this unless something breaks");
                        ChatUtil.send("/fma lb [leaderboard] - show your leaderboard position");
                        ChatUtil.send("/fma config - opens the config");
                        ChatUtil.send("/fma help - prints this message");
                        ChatUtil.send("/fma version - displays version info");
                        ChatUtil.send("/lb -> /fma lb");
                        return 0;
                     }),
                     CommandUtil.lit("version", ignored -> {
                        ChatUtil.send(FMAClient.MOD.getMetadata().getVersion().getFriendlyString());
                        return 0;
                     }),
                     CommandUtil.lit(
                        "lb",
                        CommandUtil.arg(
                           "lb_name",
                           StringArgumentType.word(),
                           context -> {
                              String lbName = StringArgumentType.getString(context, "lb_name");
                              FMAClient.LEADERBOARD
                                 .beginListen(
                                    lbName,
                                    (position, count) -> ChatUtil.send(
                                       Component.translatable(
                                          "commands.fma.leaderboard",
                                          new Object[]{
                                             FormatUtil.altText(lbName),
                                             FormatUtil.numeric(position),
                                             FormatUtil.playerNameText(FMAClient.playerName()),
                                             FormatUtil.numeric(count)
                                          }
                                       )
                                    )
                                 );
                              return 0;
                           },
                           (context, builder) -> SharedSuggestionProvider.suggest(List.of("Portal"), builder)
                        )
                     ),
                     CommandUtil.lit("config", context -> {
                        FMAClient.SCHEDULER
                           .schedule(0, minecraft -> minecraft.setScreen((Screen)AutoConfig.getConfigScreen(FMAConfig.class, minecraft.screen).get()));
                        return 0;
                     })
                  )
               );
               dispatcher.register(CommandUtil.lit("omw", context -> {
                  ChatUtil.sendCommand("lfg omw");
                  return 0;
               }, CommandUtil.arg("text", StringArgumentType.greedyString(), context -> {
                  String arg = StringArgumentType.getString(context, "text");
                  ChatUtil.sendCommand(String.format("lfg omw %s", arg));
                  return 0;
               })));
               dispatcher.register(CommandUtil.lit("omw", context -> {
                  ChatUtil.sendCommand("lfg omw");
                  return 0;
               }, CommandUtil.arg("text", StringArgumentType.greedyString(), context -> {
                  String arg = StringArgumentType.getString(context, "text");
                  ChatUtil.sendCommand(String.format("lfg omw %s", arg));
                  return 0;
               })));
               dispatcher.register(CommandUtil.lit("compass", context -> {
                  BlockPos pos = FMAClient.player().level().getSharedSpawnPos();
                  ChatUtil.send("Position: %s, %s, %s".formatted(pos.getX(), pos.getY(), pos.getZ()));
                  return 0;
               }));
               dispatcher.register(CommandUtil.lit("timer", context -> {
                  if (timerMs == -1L) {
                     timerMs = Util.now();
                     ChatUtil.send(Component.translatable("text.fma.timer_start"));
                  } else {
                     long delta = Util.now() - timerMs;
                     ChatUtil.send(Component.translatable("text.fma.timer_end", new Object[]{FormatUtil.timestamp(delta)}));
                     timerMs = -1L;
                  }

                  return 0;
               }));
               dispatcher.register((LiteralArgumentBuilder<FabricClientCommandSource>)CommandUtil.lit("lb").redirect((CommandNode)fma.getChild("lb")));
            }
         );
   }
}
