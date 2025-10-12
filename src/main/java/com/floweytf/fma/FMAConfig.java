package com.floweytf.fma;

import com.floweytf.fma.features.Waypoint;
import com.floweytf.fma.features.cz.data.CharmEffectType;
import com.floweytf.fma.features.cz.data.CharmType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.BoundedDiscrete;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category;
import me.shedaniel.autoconfig.annotation.ConfigEntry.ColorPicker;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.CollapsibleObject;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.PrefixText;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

@Config(
   name = "fma"
)
public class FMAConfig implements ConfigData {
   @Category("features")
   @TransitiveObject
   public FMAConfig.FeatureToggles features = new FMAConfig.FeatureToggles();
   @Category("appearance")
   @TransitiveObject
   public FMAConfig.Appearance appearance = new FMAConfig.Appearance();
   @Category("hpIndicator")
   @TransitiveObject
   public FMAConfig.HpIndicator hpIndicator = new FMAConfig.HpIndicator();
   @Category("chat")
   @TransitiveObject
   public FMAConfig.Chat chat = new FMAConfig.Chat();
   @Category("strikes")
   @CollapsibleObject
   public FMAConfig.Portal portal = new FMAConfig.Portal();
   @Category("strikes")
   @CollapsibleObject
   public FMAConfig.Ruin ruin = new FMAConfig.Ruin();
   @Category("zenith")
   @TransitiveObject
   public FMAConfig.Zenith zenith = new FMAConfig.Zenith();

   public static ConfigHolder<FMAConfig> register() {
      ConfigHolder<FMAConfig> holder = AutoConfig.register(
         FMAConfig.class, (config, clazz) -> new GsonConfigSerializer(config, clazz, FMAConfigHandlerHelper.GSON)
      );
      holder.registerSaveListener((configHolder, config) -> {
         config.validatePostLoad();
         FMAClient.reload();
         return InteractionResult.PASS;
      });
      FMAConfigHandlerHelper.register();
      return holder;
   }

   public void validatePostLoad() {
      if (this.hpIndicator.mediumHpPercent > this.hpIndicator.goodHpPercent) {
         this.hpIndicator.mediumHpPercent = this.hpIndicator.goodHpPercent;
      }

      if (this.hpIndicator.lowHpPercent > this.hpIndicator.mediumHpPercent) {
         this.hpIndicator.lowHpPercent = this.hpIndicator.mediumHpPercent;
      }
   }

   public static class Appearance {
      @ColorPicker
      public int bracketColor = 12041720;
      @ColorPicker
      public int tagColor = 13017334;
      public String tagText = "MAID";
      @ColorPicker
      public int textColor = 16047062;
      @ColorPicker
      public int numericColor = 15961000;
      @ColorPicker
      public int detailColor = 7106437;
      @ColorPicker
      public int playerNameColor = 15703926;
      @ColorPicker
      public int altTextColor = 11845374;
      @ColorPicker
      public int errorColor = 15091027;
      @ColorPicker
      public int warningColor = 14650909;
   }

   public static class Chat {
      public String meowingChannel = "wc";
      public String meowingText = "meow";
   }

   public static class FeatureToggles {
      public boolean enableHpIndicators = true;
      public boolean enableTimerAndStats = true;
      public boolean enableVanillaEffectInUMMHud = false;
      public boolean enableVanityDurability = true;
      public boolean enableCustomSplash = true;
      @CollapsibleObject
      public FMAConfig.InventoryOverlayToggles inventoryOverlay = new FMAConfig.InventoryOverlayToggles();
      @CollapsibleObject
      public FMAConfig.SidebarToggles sidebarToggles = new FMAConfig.SidebarToggles();
      public boolean enableDebug = SharedConstants.IS_RUNNING_IN_IDE;
      public boolean suppressDebugWarning = !SharedConstants.IS_RUNNING_IN_IDE;
      public boolean versionCheck = false;
      public boolean versionCheckIncludeBeta = false;
      public boolean contractCheck = true;
      public String contractCheckText = "Switch your contract";
      public int contractThreshold = 40;
      public int czContractThreshold = 50;
      @CollapsibleObject
      public Waypoint.Config waypoint = new Waypoint.Config();
   }

   public @interface Hidden {
   }

   public static class HpIndicator {
      public boolean enableGlowingPlayer = true;
      public boolean enableHitboxColoring = true;
      public boolean countAbsorptionAsHp = true;
      public boolean disableSelf = true;
      public boolean disableInHycenea = true;
      public boolean smoothColor = false;
      @BoundedDiscrete(
         max = 100L
      )
      public int goodHpPercent = 70;
      @BoundedDiscrete(
         max = 100L
      )
      public int mediumHpPercent = 50;
      @BoundedDiscrete(
         max = 100L
      )
      public int lowHpPercent = 25;
      @ColorPicker
      public int goodHpColor = 3403567;
      @ColorPicker
      public int mediumHpColor = 14282543;
      @ColorPicker
      public int lowHpColor = 15704623;
      @ColorPicker
      public int criticalHpColor = 15681325;
   }

   public static class InventoryOverlayToggles {
      public boolean enable = true;
      public boolean enableRarity = false;
      public boolean enableCZCharmRarity = false;
      public boolean enableCooldown = true;
      public boolean enableCZCharmPower = false;
      public boolean enablePICount = true;
      public boolean enableLoomFirmCount = false;
      @BoundedDiscrete(
         min = 0L,
         max = 20L
      )
      public int updateDelayTicks = 5;
   }

   public static class Portal {
      public boolean enablePortalButtonIndicator = true;
      public boolean nodeSplit = true;
      public boolean soulsSplit = true;
      public boolean startBossSplit = true;
      public boolean phase1Split = false;
      public boolean phase2Split = false;
      public boolean phase3Split = false;
      public boolean bossSplit = true;
      public boolean enableIotaFix = true;
   }

   public static class Ruin {
      public boolean soulsSplit = true;
      public boolean startBossSplit = true;
      public boolean daggerSplit = false;
      public boolean dpsSplit = false;
      public boolean bossSplit = true;
   }

   public static class SidebarToggles {
      public boolean enable = true;
      public boolean enableProxy = true;
      public boolean enableShard = true;
      public boolean enableIp = true;
      public boolean enableIpElision = true;
      public boolean situationals = true;
   }

   public static class Zenith {
      @PrefixText
      @DisplayCharmExamples({@CharmTypeExample(CharmType.ABILITY), @CharmTypeExample(CharmType.TREE), @CharmTypeExample(CharmType.WILDCARD)})
      public boolean enableCustomCharmInfo = true;
      @Tooltip
      public boolean disableMonumentaLore = true;
      @Tooltip
      public boolean peliCompatibilityMode = false;
      @Tooltip
      public boolean compactLore = false;
      @Tooltip
      public boolean compactUpgrade = false;
      public boolean disableBudget = false;
      public boolean displayAverageRolls = false;
      @Tooltip
      public boolean enableStatBreakdown = false;
      @Tooltip
      public boolean displayRollValue = true;
      @Tooltip
      public boolean displayEffectRarity = false;
      @Tooltip
      public boolean displayUUID = false;
      @ZenithAbilitySelection
      public Set<CharmEffectType> ignoredAbilities = new HashSet<>();
   }

   @Retention(RetentionPolicy.RUNTIME)
   @Target({ElementType.FIELD})
   public @interface ZenithAbilitySelection {
   }

   public @interface DisplayCharmExamples {
      CharmTypeExample[] value();
   }

   public @interface CharmTypeExample {
      com.floweytf.fma.features.cz.data.CharmType value();
   }
}
