package com.dayssky.mma.features.cz;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.MMAConfig.Zenith;
import com.dayssky.mma.features.cz.data.Charm;
import com.dayssky.mma.features.cz.data.CharmDataRegistries;
import com.dayssky.mma.features.cz.data.CharmEffectRarity;
import com.dayssky.mma.features.cz.data.CharmEffectType;
import com.dayssky.mma.features.cz.data.CharmRarity;
import com.dayssky.mma.util.ChatUtil;
import com.dayssky.mma.util.CommandUtil;
import com.dayssky.mma.util.FormatUtil;
import com.dayssky.mma.util.HoverControlHandler;
import com.dayssky.mma.util.NBTUtil;
import com.dayssky.mma.util.SafeExceptionLogger;
import com.dayssky.mma.util.Access;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import it.unimi.dsi.fastutil.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ZenithModule {
    private static final KeyMapping copyStatsBinding = new KeyMapping("key.mma.cz_copy_stats", Type.KEYSYM, 73, "category.mma");
    private static final SystemToastId COPIED_DATA_TOAST = new SystemToastId(1500L);
    private static final SystemToastId SKIPPED_COPY_DATA_TOAST = new SystemToastId(1500L);
    private static final SafeExceptionLogger EH = new SafeExceptionLogger("Zenith");
    public static final String ZENITH_CHARM_TIER = "zenithcharm";
    public static final String CHARM_EFFECTS_KEY = "DEPTHS_CHARM_EFFECT";
    public static final String CHARM_ACTIONS_KEY = "DEPTHS_CHARM_ACTIONS";
    public static final String CHARM_ROLLS_KEY = "DEPTHS_CHARM_ROLLS";
    public static final String CHARM_UUID_KEY = "DEPTHS_CHARM_UUID";
    public static final String CHARM_RARITY_KEY = "DEPTHS_CHARM_RARITY";
    public static final String HAS_USED_KEY = "CELESTIAL_GEM_USED";
    public static final String TARGET_BUDGET_KEY = "DEPTHS_CHARM_BUDGET";
    public static final String CHARM_TYPE_KEY = "DEPTHS_CHARM_TYPE_ROLL";
    private static final HoverControlHandler CHARM_HOVER_HANDLER = new HoverControlHandler();
    private static final Map<Long, Pair<Charm, ItemStack>> CLIPBOARD_QUEUE = new HashMap<>();
    private static boolean lastCopyStatsState = false;

    private static boolean isCopyStateDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), copyStatsBinding.key.getValue());
    }

    private static void onItemTooltip(ItemStack stack, TooltipFlag context, List<Component> lines) {
        if (!lastCopyStatsState && isCopyStateDown()) {
            getCharm(NBTUtil.access(stack), CharmDataRegistries.getMain())
                    .ifPresent(
                            charm -> {
                                if (!CLIPBOARD_QUEUE.containsKey(charm.getUuid())) {
                                    CLIPBOARD_QUEUE.put(charm.getUuid(), Pair.of(charm, stack.copy()));
                                    Minecraft.getInstance()
                                            .getToasts()
                                            .addToast(
                                                    new SystemToast(
                                                            COPIED_DATA_TOAST,
                                                            Component.translatable("text.mma.toast.add_charm_data.title"),
                                                            Component.translatable("text.mma.toast.add_charm_data.message")
                                                    )
                                            );
                                } else {
                                    Minecraft.getInstance()
                                            .getToasts()
                                            .addToast(
                                                    new SystemToast(
                                                            SKIPPED_COPY_DATA_TOAST,
                                                            Component.translatable("text.mma.toast.add_charm_data_fail.title"),
                                                            Component.translatable("text.mma.toast.add_charm_data_fail.message")
                                                    )
                                            );
                                }
                            }
                    );
        }

        lastCopyStatsState = isCopyStateDown();
        if (MMAClient.config().zenith.enableCustomCharmInfo) {
            try {
                render(stack, lines);
            } catch (Exception var4) {
                lines.add(FormatUtil.literal("* Failed to parse charm data *", ChatFormatting.RED));
                lines.add(FormatUtil.literal("* See logs *", ChatFormatting.RED));
                EH.onException(var4, "charm = " + stack.save(new CompoundTag()).getAsString());
            }
        }
    }

    public static void init() {
        KeyBindingHelper.registerKeyBinding(copyStatsBinding);
        ItemTooltipCallback.EVENT.register((ItemTooltipCallback) (stack, context, lines) -> EH.runSafely(() -> onItemTooltip(stack, context, lines)));
        ClientCommandRegistrationCallback.EVENT
                .register(
                        (ClientCommandRegistrationCallback) (dispatcher, ctx) -> dispatcher.register(
                                CommandUtil.lit(
                                        "mma",
                                        CommandUtil.lit(
                                                "copycharmdata",
                                                context -> {
                                                    String clipboardData = String.join(
                                                            "\n", CLIPBOARD_QUEUE.values().stream().map(x -> ((Charm) x.first()).dumpForOptimizer((ItemStack) x.second())).toList()
                                                    );
                                                    Minecraft.getInstance().keyboardHandler.setClipboard(clipboardData);
                                                    ChatUtil.send(
                                                            Component.translatable("text.mma.cz.copied_charm_data")
                                                                    .withStyle(
                                                                            s -> {
                                                                                HoverEvent event = new HoverEvent(
                                                                                        Action.SHOW_TEXT,
                                                                                        FormatUtil.buildTooltip(CLIPBOARD_QUEUE.values().stream().map(x -> ((ItemStack) x.second()).getHoverName()).toList())
                                                                                );
                                                                                return s.withHoverEvent(event);
                                                                            }
                                                                    )
                                                    );
                                                    CLIPBOARD_QUEUE.clear();
                                                    return 0;
                                                }
                                        )
                                )
                        )
                );
    }

    public static Optional<Charm> getCharm(Access access, CharmDataRegistries registrySet) {
        Optional<String> tier = access.getTier();
        Optional<Integer> charmPower = access.getCharmPower();
        Optional<CompoundTag> charmDataOptional = access.getPlayerModified();
        if (!tier.isEmpty() && !charmPower.isEmpty() && tier.get().equals("zenithcharm") && !charmDataOptional.isEmpty()) {
            CompoundTag charmData = charmDataOptional.get();
            long uuid = charmData.getLong("DEPTHS_CHARM_UUID");
            CharmRarity rarity = CharmRarity.values()[charmData.getInt("DEPTHS_CHARM_RARITY") - 1];
            Optional<Integer> effectsCount = charmData.getAllKeys()
                    .stream()
                    .filter(x -> x.startsWith("DEPTHS_CHARM_EFFECT"))
                    .map(x -> x.substring("DEPTHS_CHARM_EFFECT".length()))
                    .map(Integer::parseInt)
                    .reduce(Integer::max);
            if (effectsCount.isEmpty()) {
                throw new IllegalStateException("unable to obtain charm effect list");
            } else {
                int[] budgetArr = new int[]{0};
                List<CharmEffect> effects = IntStream.range(1, effectsCount.get() + 1)
                        .mapToObj(
                                i -> {
                                    double roll = charmData.getDouble("DEPTHS_CHARM_ROLLS" + i);
                                    String effectKey = charmData.getString("DEPTHS_CHARM_EFFECT" + i);
                                    CharmEffectType effectType = registrySet.charmEffectType
                                            .byName(effectKey)
                                            .orElseThrow(() -> new IllegalStateException("CHARM_EFFECTS_KEY can't be matched: " + effectKey));
                                    CharmEffectRarity effectRarity;
                                    if (charmData.contains("DEPTHS_CHARM_ACTIONS" + (i - 1))) {
                                        effectRarity = CharmEffectRarity.byName(charmData.getString("DEPTHS_CHARM_ACTIONS" + (i - 1)));
                                        budgetArr[0] -= effectRarity.budget;
                                    } else {
                                        effectRarity = CharmEffectRarity.byCharmRarity(rarity);
                                    }

                                    return new CharmEffect(roll, effectType, effectRarity);
                                }
                        )
                        .toList();
                return Optional.of(
                        new Charm(
                                charmPower.get(),
                                uuid,
                                rarity,
                                effects,
                                budgetArr[0],
                                charmData.getInt("DEPTHS_CHARM_BUDGET"),
                                charmData.getInt("DEPTHS_CHARM_TYPE_ROLL"),
                                charmData.getBoolean("CELESTIAL_GEM_USED"),
                                access.getPlainLore()
                        )
                );
            }
        } else {
            return Optional.empty();
        }
    }

    private static void render(ItemStack stack, List<Component> lines) {
        getCharm(NBTUtil.access(stack), CharmDataRegistries.getMain())
                .ifPresent(
                        charm -> {
                            if (MMAClient.config().zenith.disableMonumentaLore && !MMAClient.features().enableDebug) {
                                lines.subList(1, lines.size()).clear();
                            } else {
                                lines.add(Component.empty());
                                lines.add(FormatUtil.literal("- Custom tooltip -"));
                                lines.add(Component.empty());
                            }

                            Zenith config = MMAClient.config().zenith;
                            CharmLoreRenderer renderer = (CharmLoreRenderer) (config.peliCompatibilityMode
                                    ? new CharmLorePeliRenderer(config)
                                    : new CharmLoreTabularRenderer(config, Minecraft.getInstance().fontFilterFishy));
                            charm.render(config, renderer, lines, CHARM_HOVER_HANDLER.isEnabled(stack));
                            String currVersionString = MMAClient.MOD.getMetadata().getVersion().getFriendlyString();

                            lines.add(
                                    FormatUtil.literal("Mod version: ", ChatFormatting.DARK_GRAY)
                                            .append(
                                                    FormatUtil.literal(
                                                            currVersionString,
                                                            switch (MMAClient.VERSION_CHECK.getVersionInfo().state()) {
                                                                case OUTDATED -> ChatFormatting.RED;
                                                                case NOT_AVAILABLE, DISABLED, NOT_READY ->
                                                                        ChatFormatting.GRAY;
                                                                case LATEST -> ChatFormatting.GREEN;
                                                                case DEV_BUILD -> ChatFormatting.YELLOW;
                                                            }
                                                    )
                                            )
                            );
                        }
                );
    }
}
