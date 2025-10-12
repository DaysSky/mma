package com.dayssky.mma.features;

import com.dayssky.mma.MMAConfig;
import com.dayssky.mma.MMAClient;
import com.dayssky.mma.util.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class HpIndicator {
    public static int computeEntityHealthColor(LivingEntity entity) {
        float hp = entity.getHealth();
        MMAConfig.HpIndicator config = MMAClient.config().hpIndicator;
        if (config.countAbsorptionAsHp) {
            hp += entity.getAbsorptionAmount();
        }

        int ratio = (int) (hp / entity.getMaxHealth() * 100.0F);
        if (config.smoothColor) {
            return Util.colorRange(Mth.clamp(hp, 0.0F, entity.getMaxHealth()), entity.getMaxHealth());
        } else if (ratio > config.goodHpPercent) {
            return config.goodHpColor;
        } else if (ratio > config.mediumHpPercent) {
            return config.mediumHpColor;
        } else {
            return ratio > config.lowHpPercent ? config.lowHpColor : config.criticalHpColor;
        }
    }
}
