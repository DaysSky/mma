package com.floweytf.fma.features.cz;

import java.util.List;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface CharmLoreRenderer {
    List<? extends Component> render(List<CharmEffect> var1, @Nullable List<CharmEffect> var2, boolean var3);
}
