package com.floweytf.fma.features.cz.data;

import com.floweytf.fma.util.FormatUtil;
import net.minecraft.network.chat.Component;

public enum CharmRarity {
   COMMON("Common", 10457756, 2),
   UNCOMMON("Uncommon", 7388269, 3),
   RARE("Rare", 7364298, 4),
   EPIC("Epic", 13459146, 5),
   LEGENDARY("Legendary", 14981920, 6);

   public final String displayName;
   public final int color;
   public final Component coloredText;
   public final int budgetMultiplier;

   private CharmRarity(String displayName, int color, int budgetMultiplier) {
      this.displayName = displayName;
      this.color = color;
      this.budgetMultiplier = budgetMultiplier;
      this.coloredText = FormatUtil.literal(displayName, color);
   }

   public CharmRarity upgrade() {
      if (this == LEGENDARY) {
         throw new UnsupportedOperationException("Can't upgrade legendary");
      } else {
         return values()[this.ordinal() + 1];
      }
   }

   public boolean isGreater(CharmRarity other) {
      return this.ordinal() > other.ordinal();
   }
}
