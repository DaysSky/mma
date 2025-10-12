package com.floweytf.fma.features.cz.data;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.FMAConfig.Zenith;
import com.floweytf.fma.features.cz.CharmEffect;
import com.floweytf.fma.features.cz.CharmLoreRenderer;
import com.floweytf.fma.util.FormatUtil;
import com.floweytf.fma.util.Util;
import com.floweytf.fma.util.FormatUtil.ComponentJoiner;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class Charm {
   public static final int[] BUDGET_BY_CP = new int[]{2, 4, 7, 11, 16};
   private final int charmPower;
   private final long uuid;
   private final CharmRarity rarity;
   private final List<CharmEffect> effects;
   private final List<ZenithTree> classes;
   private final List<ZenithAbility> abilities;
   @Nullable
   private final List<CharmEffect> upgradedEffects;
   private final int upgradedBudget;
   private final int upgradedMaxBudget;
   private final int budget;
   private final int maxBudget;
   private final CharmType type;
   private final boolean hasUpgraded;
   private final boolean hasWarning;

   public Charm(
      int charmPower,
      long uuid,
      CharmRarity rarity,
      List<CharmEffect> effects,
      int budget,
      int maxBudget,
      int typeId,
      boolean hasUpgraded,
      List<String> monumentaLore
   ) {
      this.charmPower = charmPower;
      this.uuid = uuid;
      this.rarity = rarity;
      this.effects = new ArrayList<>(effects);
      this.hasUpgraded = hasUpgraded;
      this.classes = effects.stream()
         .map(x -> x.effect().ability.tree)
         .collect(Collectors.toSet())
         .stream()
         .sorted(Comparator.comparingInt(Enum::ordinal))
         .toList();
      this.abilities = effects.stream().map(x -> x.effect().ability).collect(Collectors.toSet()).stream().sorted(Comparator.comparing(a -> a.name)).toList();
      this.type = CharmType.byId(typeId);
      this.maxBudget = maxBudget;
      this.budget = budget;
      this.hasWarning = this.selfTest(monumentaLore);
      if (this.canUpgrade()) {
         CharmRarity upgradedCharmRarity = rarity.upgrade();
         int newBudget = computeBudget(upgradedCharmRarity, charmPower, this.type);
         ArrayList<CharmEffect> result = new ArrayList<>(effects);
         Util.with(result, 0, firstEffect -> firstEffect.withRarity(CharmEffectRarity::upgrade));
         int remainingBudget = newBudget - budget;
         boolean hasUpgrade = false;

         do {
            hasUpgrade = false;

            for (int i = 1; i < effects.size(); i++) {
               CharmEffect effect = result.get(i);
               if (effect.canUpgrade(upgradedCharmRarity, remainingBudget)) {
                  remainingBudget += effect.getEffectRarity().upgradeDelta();
                  Util.with(result, i, e -> e.withRarity(CharmEffectRarity::upgrade));
                  hasUpgrade = true;
               }
            }
         } while (hasUpgrade);

         this.upgradedEffects = result;
         this.upgradedBudget = newBudget - remainingBudget;
         this.upgradedMaxBudget = newBudget;
      } else {
         this.upgradedEffects = null;
         this.upgradedBudget = -1;
         this.upgradedMaxBudget = -1;
      }

      this.effects.sort(Comparator.comparingInt(x -> x.effect().ordinal));
      if (this.upgradedEffects != null) {
         this.upgradedEffects.sort(Comparator.comparingInt(x -> x.effect().ordinal));
      }
   }

   private static int computeBudget(CharmRarity rarity, int cp, CharmType type) {
      return (int)(BUDGET_BY_CP[cp - 1] * rarity.budgetMultiplier * type.factor());
   }

   private boolean selfTest(List<String> rawLore) {
      boolean hasWarning = false;
      int computedBudget = computeBudget(this.rarity, this.charmPower, this.type);
      if (computedBudget != this.maxBudget) {
         hasWarning = true;
         FMAClient.LOGGER.error("Budget algo is likely bugged: flowey={}, monumenta={}", computedBudget, this.maxBudget);
      }

      for (CharmEffect effect : this.effects) {
         String text = effect.monumentaText();
         if (!rawLore.contains(text)) {
            hasWarning = true;
            FMAClient.LOGGER.error("not found: {}", text);
         }
      }

      return hasWarning;
   }

   private boolean canUpgrade() {
      return !this.hasUpgraded && this.rarity != CharmRarity.LEGENDARY;
   }

   private void renderHeader(List<Component> target) {
      target.add(FormatUtil.join(FormatUtil.literal("Architect's Ring : ", ChatFormatting.DARK_GRAY), FormatUtil.literal("Zenith Charm", 16751856)));
      Component rarityText = (Component)(this.hasUpgraded
         ? FormatUtil.join(this.rarity.coloredText, FormatUtil.literal(" "), FormatUtil.literal("(❃)", 9663743))
         : this.rarity.coloredText);
      target.add(
         FormatUtil.join(
            FormatUtil.literal("Charm Power : ", ChatFormatting.DARK_GRAY),
            FormatUtil.literal("★".repeat(this.charmPower), style -> style.withColor(16775797)),
            FormatUtil.literal(" - ", ChatFormatting.DARK_GRAY),
            rarityText
         )
      );
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private void renderCompactHeader(List<Component> target) {
      Component rarityText = (Component)(this.hasUpgraded
         ? FormatUtil.join(this.rarity.coloredText, FormatUtil.literal(" "), FormatUtil.literal("(❃)", 9663743))
         : this.rarity.coloredText);

      target.add(
         FormatUtil.join(
            FormatUtil.literal("Charm : ", ChatFormatting.DARK_GRAY),
            FormatUtil.literal("★".repeat(this.charmPower), style -> style.withColor(16775797)),
            FormatUtil.literal(" - ", ChatFormatting.DARK_GRAY),
            rarityText,
            FormatUtil.literal(" - ", ChatFormatting.DARK_GRAY),
            (Component)(switch (this.type) {
               case ABILITY -> this.abilities.get(0).coloredName;
               case TREE -> this.classes.get(0).coloredName;
               case WILDCARD -> {
                  MutableComponent classText = Component.empty();

                  for (int i = 0; i < this.classes.size() - 1; i++) {
                     classText = classText.append(this.classes.get(i).coloredShortName).append(FormatUtil.literal(",", ChatFormatting.DARK_GRAY));
                  }

                  classText.append(this.classes.get(this.classes.size() - 1).coloredShortName);
                  yield classText;
               }
            })
         )
      );
   }

   private void renderBudget(List<Component> target, boolean upgrade) {
      double budgetVal = (double)this.budget / this.maxBudget;
      ComponentJoiner budgetLine = FormatUtil.joiner()
         .add(
            new Component[]{
               FormatUtil.literal("Budget : ", ChatFormatting.DARK_GRAY),
               FormatUtil.literal(this.budget + "/" + this.maxBudget, ChatFormatting.GRAY),
               FormatUtil.literal(" [", ChatFormatting.GRAY),
               FormatUtil.literal(FormatUtil.fmtDouble(FormatUtil.twoDecimal(budgetVal * 100.0), false) + "%", Util.colorRange(budgetVal)),
               FormatUtil.literal("]", ChatFormatting.GRAY)
            }
         );
      if (upgrade && this.canUpgrade()) {
         double upgradedBudgetVal = (double)this.upgradedBudget / this.upgradedMaxBudget;
         budgetLine.add(
            new Component[]{
               FormatUtil.literal(" -> ", ChatFormatting.GRAY),
               FormatUtil.literal(this.upgradedBudget + "/" + this.upgradedMaxBudget, ChatFormatting.GRAY),
               FormatUtil.literal(" [", ChatFormatting.GRAY),
               FormatUtil.literal(FormatUtil.fmtDouble(FormatUtil.twoDecimal(upgradedBudgetVal * 100.0), false) + "%", Util.colorRange(upgradedBudgetVal)),
               FormatUtil.literal("]", ChatFormatting.GRAY)
            }
         );
      }

      target.add(budgetLine.build());
   }

   private void renderAverageRolls(List<Component> target) {
      double averageRollValue = this.effects.stream().mapToDouble(x -> x.displayRollValue).average().orElse(0.0);
      Style rollColor = Style.EMPTY.withColor(Util.colorRange((float)averageRollValue));
      target.add(
         FormatUtil.join(
            FormatUtil.literal("Average Roll : ", ChatFormatting.DARK_GRAY),
            FormatUtil.literal(FormatUtil.literal(FormatUtil.fmtDouble(FormatUtil.twoDecimal(averageRollValue * 100.0), false) + "%", rollColor))
         )
      );
   }

   private void renderUUID(List<Component> target) {
      target.add(
         FormatUtil.join(
            FormatUtil.literal("UUID : ", ChatFormatting.DARK_GRAY), FormatUtil.literal(Long.toHexString(this.uuid).toUpperCase(), ChatFormatting.GRAY)
         )
      );
   }

   private void renderTreeInfo(List<Component> target) {
      target.add(
         switch (this.type) {
            case ABILITY -> FormatUtil.join(
               FormatUtil.literal("Ability", ChatFormatting.GRAY),
               FormatUtil.literal(" - ", ChatFormatting.DARK_GRAY),
               this.abilities.get(0).coloredName,
               FormatUtil.literal(" (", ChatFormatting.GRAY),
               this.classes.get(0).coloredName,
               FormatUtil.literal(")", ChatFormatting.GRAY)
            );
            case TREE -> FormatUtil.join(
               FormatUtil.literal("Treelocked", ChatFormatting.GRAY), FormatUtil.literal(" - ", ChatFormatting.DARK_GRAY), this.classes.get(0).coloredName
            );
            case WILDCARD -> {
               MutableComponent classText = Component.empty();

               for (int i = 0; i < this.classes.size() - 1; i++) {
                  classText = classText.append(this.classes.get(i).coloredShortName).append(FormatUtil.literal(",", ChatFormatting.DARK_GRAY));
               }

               classText.append(this.classes.get(this.classes.size() - 1).coloredShortName);
               yield FormatUtil.join(FormatUtil.literal("Wildcard", ChatFormatting.GRAY), FormatUtil.literal(" - ", ChatFormatting.DARK_GRAY), classText);
            }
         }
      );
   }

   public void render(Zenith config, CharmLoreRenderer renderer, List<Component> target, boolean upgrade) {
      if (config.compactLore) {
         this.renderCompactHeader(target);
      } else {
         this.renderHeader(target);
      }

      if (!config.disableBudget) {
         this.renderBudget(target, upgrade);
      }

      if (config.displayAverageRolls) {
         this.renderAverageRolls(target);
      }

      if (config.displayUUID) {
         this.renderUUID(target);
      }

      if (!config.compactLore) {
         this.renderTreeInfo(target);
      }

      target.add(Component.empty());
      target.addAll(renderer.render(this.effects, upgrade ? this.upgradedEffects : null, this.type != CharmType.ABILITY));
      if (!config.compactLore && this.canUpgrade()) {
         target.add(FormatUtil.literal("[shift] - see upgrade | [ctrl] - toggle", ChatFormatting.DARK_GRAY));
      }

      if (this.hasWarning) {
         target.add(FormatUtil.literal("* WARNING - possible bug, report to Flowey *", ChatFormatting.RED));
         target.add(FormatUtil.literal("* Charm display data may be wrong *", ChatFormatting.RED));
      }
   }

   public String dumpForOptimizer(ItemStack stack) {
      String mainResult = String.format(
         "%s;%s;%s;%s;%s",
         this.rarity.ordinal(),
         stack.getHoverName().getString(),
         this.charmPower,
         this.effects.stream().map(x -> x.effect.name.toLowerCase().replace(" ", "_")).collect(Collectors.joining(":")),
         this.effects.stream().map(x -> String.format(Locale.ROOT, "%.2f", x.value)).collect(Collectors.joining(":"))
      );
      return this.upgradedEffects != null
         ? String.format(
            "%s;%s", mainResult, this.upgradedEffects.stream().map(x -> String.format(Locale.ROOT, "%.2f", x.value)).collect(Collectors.joining(":"))
         )
         : mainResult;
   }

   public long getUuid() {
      return this.uuid;
   }
}
