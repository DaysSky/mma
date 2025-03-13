package com.floweytf.fma.features.cz;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.features.cz.data.CharmEffectRarity;
import com.floweytf.fma.features.cz.data.CharmEffectType;
import com.floweytf.fma.features.cz.data.CharmRarity;
import com.floweytf.fma.util.HoverControlHandler;
import com.floweytf.fma.util.NBTUtil;
import com.google.common.collect.Lists;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import static com.floweytf.fma.util.FormatUtil.literal;
import static net.minecraft.network.chat.Component.empty;

public class CharmItemManager {
    public static final String ZENITH_CHARM_TIER = "zenithcharm";

    public static final String CHARM_EFFECTS_KEY = "DEPTHS_CHARM_EFFECT";
    public static final String CHARM_ACTIONS_KEY = "DEPTHS_CHARM_ACTIONS";
    public static final String CHARM_ROLLS_KEY = "DEPTHS_CHARM_ROLLS";
    public static final String CHARM_UUID_KEY = "DEPTHS_CHARM_UUID";
    public static final String CHARM_RARITY_KEY = "DEPTHS_CHARM_RARITY";
    public static final String HAS_USED_KEY = "CELESTIAL_GEM_USED";
    public static final String TARGET_BUDGET_KEY = "DEPTHS_CHARM_BUDGET";
    public static final String CHARM_TYPE_KEY = "DEPTHS_CHARM_TYPE_ROLL";

    // Kill me nyow...
    // TODO: make the following logic good
    private static final HoverControlHandler charmHoverHandler = new HoverControlHandler();

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (!FMAClient.config().zenith.enableCustomCharmInfo) {
                return;
            }

            try {
                // TODO: i18n
                getCharm(stack).ifPresent(charm -> {
                    if (FMAClient.config().zenith.disableMonumentaLore && !FMAClient.features().enableDebug) {
                        lines.subList(1, lines.size()).clear();
                    } else {
                        lines.add(empty());
                        lines.add(literal("- Custom tooltip -"));
                        lines.add(empty());
                    }

                    charm.buildLore(Minecraft.getInstance().fontFilterFishy, lines, charmHoverHandler.isEnabled(stack));

                    // somewhat important because I don't want people using outdated flowey mod, which might have bugs
                    final var currVersionString = FMAClient.MOD.getMetadata().getVersion().getFriendlyString();
                    lines.add(literal("Mod version: ").append(literal(currVersionString,
                        switch (FMAClient.VERSION_CHECK.getVersionInfo().state()) {
                            case OUTDATED -> ChatFormatting.RED;
                            case NOT_AVAILABLE, NOT_READY, DISABLED -> ChatFormatting.GRAY;
                            case LATEST -> ChatFormatting.GREEN;
                            case DEV_BUILD -> ChatFormatting.YELLOW;
                        }
                    )));
                });
            } catch (Exception e) {
                lines.add(literal("* Failed to parse charm data *", ChatFormatting.RED));
                lines.add(literal("* See logs *", ChatFormatting.RED));
                FMAClient.LOGGER.error("uh oh", e);
            }
        });
    }

    private static Supplier<IllegalStateException> ise(String text) {
        return () -> new IllegalStateException(text);
    }

    public static Optional<Charm> getCharm(ItemStack item) {
        // TODO: this probably won't port well to 1.20.5, but that's too far in the future... Perhaps I'm
        //  foot-gunning myself...
        final var monumenta = NBTUtil.getMonumenta(item);
        final var tier = NBTUtil.getTier(item);
        final var charmPower = NBTUtil.getCharmPower(item);

        if (tier.isEmpty() || charmPower.isEmpty() || !tier.get().equals(ZENITH_CHARM_TIER) || monumenta.isEmpty()) {
            return Optional.empty();
        }

        // meow meow meow meow meow meow
        final var charmDataOptional = NBTUtil.getPlayerModified(item);
        if (charmDataOptional.isEmpty()) {
            return Optional.empty();
        }

        final var charmData = charmDataOptional.get();

        final var uuid = charmData.getLong(CHARM_UUID_KEY);
        final var rarity = CharmRarity.values()[charmData.getInt(CHARM_RARITY_KEY) - 1];

        final var effectsCount = charmData.getAllKeys().stream()
            .filter(x -> x.startsWith(CHARM_EFFECTS_KEY))
            .map(x -> x.substring(CHARM_EFFECTS_KEY.length()))
            .map(Integer::parseInt)
            .reduce(Integer::max);

        if (effectsCount.isEmpty()) {
            throw new IllegalStateException("unable to obtain charm effect list");
        }

        final var budgetArr = new int[]{0};

        final var effects = IntStream.range(1, effectsCount.get() + 1).mapToObj(i -> {
            final var roll = NBTUtil.getDouble(charmData, CHARM_ROLLS_KEY + i)
                .orElseThrow(ise("CHARM_ROLLS_KEY must be double"));

            final var effectKey = NBTUtil.getString(charmData, CHARM_EFFECTS_KEY + i)
                .orElseThrow(ise("CHARM_EFFECTS_KEY can't be found"));

            final var effectType = CharmEffectType.byName(effectKey)
                .orElseThrow(ise("CHARM_EFFECTS_KEY can't be matched: " + effectKey));

            final var effectRarityOpt = NBTUtil.getString(charmData, CHARM_ACTIONS_KEY + (i - 1))
                .map(x -> CharmEffectRarity.byName(x).orElseThrow(ise("CHARM_ACTIONS_KEY can't be found")));

            final var effectRarity = effectRarityOpt
                .orElse(CharmEffectRarity.byCharmRarity(rarity));

            effectRarityOpt.map(x -> budgetArr[0] -= x.budget);

            return new CharmEffectInstance(roll, effectType, effectRarity);
        }).toList();


        return Optional.of(new Charm(
            charmPower.get(),
            uuid,
            rarity,
            effects,
            budgetArr[0],
            charmData.getInt(TARGET_BUDGET_KEY),
            charmData.getInt(CHARM_TYPE_KEY),
            charmData.getBoolean(HAS_USED_KEY),
            Lists.transform(NBTUtil.getLore(item).orElseThrow(), Tag::getAsString)
        ));
    }
}
