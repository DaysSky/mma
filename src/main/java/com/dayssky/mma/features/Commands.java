package com.dayssky.mma.features;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.MMAConfig;
import com.dayssky.mma.debug.Debug;
import com.dayssky.mma.util.ChatUtil;
import com.dayssky.mma.util.CommandUtil;
import com.dayssky.mma.util.FormatUtil;
import com.dayssky.mma.util.Util;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

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
                        (ClientCommandRegistrationCallback) (dispatcher, registryAccess) -> {
                            LiteralCommandNode<FabricClientCommandSource> mma = dispatcher.register(
                                    CommandUtil.lit(
                                            "mma",
                                            CommandUtil.<FabricClientCommandSource>litPred(
                                                    "debug",
                                                    ignored -> MMAClient.config().features.enableDebug,
                                                    CommandUtil.lit("test", ignored -> {
                                                        ChatUtil.send(":3");
                                                        return 0;
                                                    }),
                                                    CommandUtil.lit("re", ignored -> {
                                                        MMAClient.reload();
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
                                                        MMAClient.level().entitiesForRendering().forEach(e -> {
                                                            if (e.getEyePosition().distanceTo(MMAClient.player().getEyePosition()) < 10.0) {
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
                                                                                NbtUtils.toPrettyComponent(MMAClient.player().getItemInHand(InteractionHand.MAIN_HAND).getTag())
                                                                        )
                                                                );
                                                                return 0;
                                                            }
                                                    ),
                                                    CommandUtil.lit("fakecrash", context -> {
                                                        MMAClient.GLOBAL_SAFE_EH.onException(new Exception(), "test");
                                                        return 0;
                                                    })
                                            ),
                                            CommandUtil.lit("help", ignored -> {
                                                ChatUtil.send(Component.literal("Command Help").withStyle(ChatFormatting.BOLD));
                                                ChatUtil.send("/cc - clear chat");
                                                ChatUtil.send("/omw - shorthand for /lfg omw");
                                                ChatUtil.send("/mma debug - dumps internal state, don't use this unless something breaks");
                                                ChatUtil.send("/mma lb [leaderboard] - show your leaderboard position");
                                                ChatUtil.send("/mma config - opens the config");
                                                ChatUtil.send("/mma help - prints this message");
                                                ChatUtil.send("/mma version - displays version info");
                                                ChatUtil.send("/lb -> /mma lb");
                                                return 0;
                                            }),
                                            CommandUtil.lit("version", ignored -> {
                                                ChatUtil.send(MMAClient.MOD.getMetadata().getVersion().getFriendlyString());
                                                return 0;
                                            }),
                                            CommandUtil.lit("config", context -> {
                                                MMAClient.SCHEDULER
                                                        .schedule(0, minecraft -> minecraft.setScreen((Screen) AutoConfig.getConfigScreen(MMAConfig.class, minecraft.screen).get()));
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
                                BlockPos pos = MMAClient.player().level().getSharedSpawnPos();
                                ChatUtil.send("Position: %s, %s, %s".formatted(pos.getX(), pos.getY(), pos.getZ()));
                                return 0;
                            }));
                            dispatcher.register(CommandUtil.lit("timer", context -> {
                                if (timerMs == -1L) {
                                    timerMs = Util.now();
                                    ChatUtil.send(Component.translatable("text.mma.timer_start"));
                                } else {
                                    long delta = Util.now() - timerMs;
                                    ChatUtil.send(Component.translatable("text.mma.timer_end", new Object[]{FormatUtil.timestamp(delta)}));
                                    timerMs = -1L;
                                }

                                return 0;
                            }));
                            dispatcher.register(CommandUtil.lit("lb",
                                    CommandUtil.arg(
                                            "lb_name",
                                            StringArgumentType.word(),
                                            context -> {
                                                String lbName = LeaderboardUtils.resolve(StringArgumentType.getString(context, "lb_name"));
                                                ChatUtil.sendCommand(String.format("leaderboard @s %s true 1", lbName));
                                                return 0;
                                            },
                                            (context, builder) -> SharedSuggestionProvider.suggest(LeaderboardUtils.getKeys(), builder),
                                            CommandUtil.arg(
                                                    "arg",
                                                    StringArgumentType.word(),
                                                    context -> {
                                                        String lbName = LeaderboardUtils.resolve(StringArgumentType.getString(context, "lb_name"));
                                                        String arg = StringArgumentType.getString(context, "arg");
                                                        ChatUtil.sendCommand(String.format("leaderboard @s %s true %s", lbName, arg));
                                                        return 0;
                                                    }
                                            )
                                    )
                            ));
                        }
                );
    }
}
