package com.floweytf.fma.features;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig;
import com.floweytf.fma.debug.Debug;
import com.floweytf.fma.util.ChatUtil;
import com.floweytf.fma.util.CommandUtil;
import static com.floweytf.fma.util.CommandUtil.arg;
import static com.floweytf.fma.util.CommandUtil.lit;
import com.floweytf.fma.util.FormatUtil;
import static com.floweytf.fma.util.FormatUtil.join;
import com.floweytf.fma.util.Util;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public class Commands {
    private static LiteralArgumentBuilder<FabricClientCommandSource> alias(String name, String target) {
        return lit(name,
            // forward
            context -> {
                ChatUtil.sendCommand(target);
                return 0;
            }
        );
    }

    private static long timerMs = -1;

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            final var fma = dispatcher.register(lit("fma",
                lit("debug_test", ignored -> {
                    ChatUtil.send(":3");
                    return 0;
                }, ignored -> FMAClient.config().features.enableDebug),
                lit("debug_reload", ignored -> {
                    FMAClient.reload();
                    return 0;
                }, ignored -> FMAClient.config().features.enableDebug),
                lit("debug_e", ignored -> {
                    Debug.ENTITY_DEBUG = !Debug.ENTITY_DEBUG;
                    ChatUtil.send("Entity Debug: " + Debug.ENTITY_DEBUG);
                    return 0;
                }, ignored -> FMAClient.config().features.enableDebug),
                lit("debug_b", ignored -> {
                    Debug.BLOCK_DEBUG = !Debug.BLOCK_DEBUG;
                    ChatUtil.send("Block Debug: " + Debug.ENTITY_DEBUG);
                    return 0;
                }, ignored -> FMAClient.config().features.enableDebug),
                lit("dump_e", context -> {
                    FMAClient.level().entitiesForRendering().forEach(e -> {
                        if (e.getEyePosition().distanceTo(FMAClient.player().getEyePosition()) < 10) {
                            Debug.dumpEntityInfo(e);
                        }
                    });
                    return 0;
                }, ignored -> FMAClient.config().features.enableDebug),
                lit("dumpnbt", context -> {
                    ChatUtil.send(join(
                        Component.literal("Data: "),
                        NbtUtils.toPrettyComponent(FMAClient.player().getItemInHand(InteractionHand.MAIN_HAND).getTag())
                    ));
                    return 0;
                }, ignored -> FMAClient.config().features.enableDebug),
                lit("help", ignored -> {
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
                lit("version", ignored -> {
                    ChatUtil.send(FMAClient.MOD.getMetadata().getVersion().getFriendlyString());
                    return 0;
                }),
                lit("lb",
                    arg("lb_name", StringArgumentType.word(), context -> {
                        final var lbName = StringArgumentType.getString(context, "lb_name");
                        FMAClient.LEADERBOARD.beginListen(lbName, (position, count) -> {
                            ChatUtil.send(Component.translatable(
                                "commands.fma.leaderboard",
                                FormatUtil.altText(lbName),
                                FormatUtil.numeric(position),
                                FormatUtil.playerNameText(FMAClient.playerName()),
                                FormatUtil.numeric(count)
                            ));
                        });

                        return 0;
                    }, (context, builder) -> SharedSuggestionProvider.suggest(List.of(
                        "Portal"
                    ), builder))
                ),
                lit("config", context -> {
                    FMAClient.SCHEDULER.schedule(0,
                        minecraft -> minecraft.setScreen(AutoConfig.getConfigScreen(FMAConfig.class,
                            minecraft.screen).get()));
                    return 0;
                })
            ));

            dispatcher.register(lit("omw",
                // forward
                context -> {
                    ChatUtil.sendCommand("lfg omw");
                    return 0;
                },
                arg("text", StringArgumentType.greedyString(), context -> {
                    final var arg = StringArgumentType.getString(context, "text");
                    ChatUtil.sendCommand(String.format("lfg omw %s", arg));
                    return 0;
                })
            ));

            dispatcher.register(lit("omw",
                // forward
                context -> {
                    ChatUtil.sendCommand("lfg omw");
                    return 0;
                },
                arg("text", StringArgumentType.greedyString(), context -> {
                    final var arg = StringArgumentType.getString(context, "text");
                    ChatUtil.sendCommand(String.format("lfg omw %s", arg));
                    return 0;
                })
            ));

            dispatcher.register(lit("compass", context -> {
                final var pos = FMAClient.player().level().getSharedSpawnPos();
                ChatUtil.send("Position: %s, %s, %s".formatted(pos.getX(), pos.getY(), pos.getZ()));
                return 0;
            }));

            dispatcher.register(lit("timer", context -> {
                if(timerMs == -1) {
                    timerMs = Util.now();
                    ChatUtil.send(Component.translatable("text.fma.timer_start"));
                } else {
                    final var delta = Util.now() - timerMs;
                    ChatUtil.send(Component.translatable("text.fma.timer_end", FormatUtil.timestamp(delta)));
                    timerMs = -1;
                }
                return 0;
            }));

            dispatcher.register(CommandUtil.<FabricClientCommandSource>lit("lb").redirect(fma.getChild("lb")));
        });
    }
}
