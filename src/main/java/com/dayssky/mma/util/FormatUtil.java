package com.dayssky.mma.util;

import com.dayssky.mma.MMAClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.resources.ResourceLocation;

public class FormatUtil {
    private static final ResourceLocation SPACING = new ResourceLocation("mma:formatting/spacing");

    public static ComponentJoiner joiner() {
        return new ComponentJoiner();
    }

    public static MutableComponent join(Component... components) {
        return join(Component.empty(), List.of(components));
    }

    public static MutableComponent join(MutableComponent root, Iterable<? extends Component> components) {
        for (Component component : components) {
            root.append(component);
        }

        return root;
    }

    public static MutableComponent join(MutableComponent root, Component delimiter, List<? extends Component> components) {
        for (int i = 0; i < components.size(); i++) {
            root.append(components.get(i));
            if (i + 1 != components.size()) {
                root.append(delimiter);
            }
        }

        return root;
    }

    public static MutableComponent buildTooltip(List<? extends Component> components) {
        MutableComponent inst = colored(MMAClient.appearance().textColor);

        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            inst.append(component);
            if (i + 1 != components.size()) {
                inst.append(Component.literal("\n"));
            }
        }

        return inst;
    }

    public static MutableComponent withColor(MutableComponent component, int color) {
        return component.withStyle(s -> s.withColor(color));
    }

    public static MutableComponent withColor(String component, int color) {
        return withColor(Component.literal(component), color);
    }

    public static MutableComponent withHover(MutableComponent component, Component hover) {
        return component.withStyle(s -> s.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, hover)));
    }

    public static MutableComponent colored(int color) {
        return withColor(Component.empty(), color);
    }

    private static String formatTimestamp(long ticks) {
        long second = ticks / 1000L;
        long min = second / 60L;
        long hr = min / 60L;
        return hr == 0L
                ? String.format("%d:%02d.%03d", min % 60L, second % 60L, ticks % 1000L)
                : String.format("%d:%02d:%02d.%03d", hr, min % 60L, second % 60L, ticks % 1000L);
    }

    public static MutableComponent timestamp(long ticks) {
        return withColor(formatTimestamp(ticks), MMAClient.appearance().numericColor);
    }

    public static MutableComponent timestampAlt(long ticks) {
        return withColor(formatTimestamp(ticks), MMAClient.appearance().detailColor);
    }

    public static MutableComponent numeric(Object value) {
        return withColor(String.valueOf(value), MMAClient.appearance().numericColor);
    }

    public static MutableComponent altText(String text) {
        return withColor(text, MMAClient.appearance().altTextColor);
    }

    public static MutableComponent playerNameText(String text) {
        return withColor(text, MMAClient.appearance().playerNameColor);
    }

    public static String fmtDouble(double d) {
        return fmtDouble(d, true);
    }

    public static String fmtDouble(double d, boolean showPlus) {
        String prefix = showPlus && d > 0.0 ? "+" : "";
        return d == (int) d ? prefix + (int) d : prefix + d;
    }

    public static String fmtDoubleDelta(double d) {
        String prefix = d >= 0.0 ? "+" : "";
        return d == (int) d ? prefix + (int) d : prefix + d;
    }

    public static Component pad(int pixels) {
        MutableComponent res = Component.empty();

        while (pixels > 0) {
            for (int i = 9; i >= 0; i--) {
                int spacing = 1 << i;
                if (pixels >= spacing) {
                    pixels -= spacing;
                    res = res.append(Component.literal(Character.toString(48 + i))).withStyle(style -> style.withFont(SPACING));
                    break;
                }
            }
        }

        return res;
    }

    public static List<MutableComponent> tabulate(Font font, List<? extends List<? extends Component>> table) {
        int cols = table.get(0).size();
        MutableComponent[] components = new MutableComponent[table.size()];

        for (int i = 0; i < components.length; i++) {
            components[i] = Component.empty();
        }

        for (int j = 0; j < cols; j++) {
            int[] width = new int[table.size()];
            int maxWidth = 0;

            for (int i = 0; i < table.size(); i++) {
                maxWidth = Math.max(maxWidth, width[i] = font.width((FormattedText) table.get(i).get(j)));
            }

            if (j + 1 == cols) {
                for (int i = 0; i < table.size(); i++) {
                    components[i] = components[i].append(table.get(i).get(j));
                }
            } else {
                for (int i = 0; i < table.size(); i++) {
                    components[i] = components[i].append(table.get(i).get(j)).append(pad(maxWidth - width[i])).append(" ");
                }
            }
        }

        return List.of(components);
    }

    public static MutableComponent literal(Object value, ChatFormatting... formatting) {
        return Component.literal(String.valueOf(value)).withStyle(formatting);
    }

    public static MutableComponent literal(Object value, UnaryOperator<Style> formatter) {
        return Component.literal(String.valueOf(value)).withStyle(formatter);
    }

    public static MutableComponent literal(Object value, int color) {
        return Component.literal(String.valueOf(value)).withStyle(s -> s.withColor(color));
    }

    public static MutableComponent literal(Object value, Style s) {
        return Component.literal(String.valueOf(value)).withStyle(s);
    }

    public static double twoDecimal(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static class ComponentJoiner {
        private final List<Component> elements = new ArrayList<>();

        public ComponentJoiner add(Component... components) {
            this.elements.addAll(List.of(components));
            return this;
        }

        public MutableComponent build() {
            return FormatUtil.join(Component.empty(), this.elements);
        }
    }
}
