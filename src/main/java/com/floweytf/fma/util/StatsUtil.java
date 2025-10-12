package com.floweytf.fma.util;

import com.floweytf.fma.util.StatsUtil.Custom;
import com.floweytf.fma.util.StatsUtil.Detail;
import com.floweytf.fma.util.StatsUtil.Time;
import com.google.common.base.CaseFormat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import static com.floweytf.fma.util.ChatUtil.send;
import static com.floweytf.fma.util.FormatUtil.numeric;
import static com.floweytf.fma.util.FormatUtil.timestamp;

public class StatsUtil {
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Time {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Detail {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Custom {
    }

    public static int logTime(String key, boolean send, long start, long deltaBegin, long deltaEnd, long... entries) {
        List<Component> tooltipLines = new ArrayList<>();

        var base = deltaBegin;
        for (int i = 0; i < entries.length; i++) {
            tooltipLines.add(Component.translatable(
                    key + "." + i,
                    FormatUtil.timestamp(entries[i] - base)
            ));
            base = entries[i];
        }

        final var text = Component.translatable(
                key,
                FormatUtil.timestamp(deltaEnd - deltaBegin),
                FormatUtil.timestampAlt(deltaEnd - start)
        );

        if (send) {
            if (entries.length == 1) {
                send(text);
            } else {
                final var event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, FormatUtil.buildTooltip(tooltipLines));
                send(text.withStyle(style -> style.withHoverEvent(event)));
            }
        }

        return (int) (deltaEnd - deltaBegin);
    }

    public static <T> void dumpStats(String translationRoot, T object) {
        try {
            List<MutableComponent> tooltipLines = new ArrayList<>();
            List<MutableComponent> regularLines = new ArrayList<>();

            for (Field part : object.getClass().getFields()) {
                boolean isDetail = part.getAnnotationsByType(Detail.class).length != 0;

                MutableComponent text;

                if (part.getAnnotationsByType(Custom.class).length != 0) {
                    // For custom fields, call the custom render method
                    String formatterName = "render" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, part.getName());

                    try {
                        text = (MutableComponent) object.getClass().getMethod(formatterName).invoke(object);
                    } catch (ReflectiveOperationException e) {
                        Util.sneakyThrow(e);
                        return;
                    }
                } else {
                    boolean isTimestamp = part.getAnnotationsByType(Time.class).length != 0;

                    final var value = part.get(object);

                    if (part.getType() != int.class) {
                        throw new IllegalStateException("idk");
                    }

                    final int intValue = (int) value;
                    final var key = translationRoot + "." + part.getName();

                    if (intValue == -1) {
                        text = Component.translatable(key, numeric("N/A"));
                    } else if (isTimestamp) {
                        text = Component.translatable(key, timestamp(intValue));
                    } else {
                        text = Component.translatable(key, numeric(intValue));
                    }
                }

                (isDetail ? tooltipLines : regularLines).add(text);
            }

            final var hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, FormatUtil.buildTooltip(tooltipLines));
            send(Component.translatable(translationRoot + "." + "title").withStyle(ChatFormatting.UNDERLINE));
            for (final var regularLine : regularLines) {
                send(regularLine.withStyle(style -> style.withHoverEvent(hover)));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
