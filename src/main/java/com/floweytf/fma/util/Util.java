package com.floweytf.fma.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import net.minecraft.util.Mth;

public class Util {
    public static long now() {
        return System.currentTimeMillis();
    }

    public static <T> T c(Object o) {
        return (T) o;
    }

    public static <T, C extends Collection<T>> Collector<T, ?, C> colAppend(C collection) {
        return Collectors.toCollection(() -> collection);
    }

    public static <K, V> Optional<V> get(Map<K, V> map, K key) {
        return Optional.ofNullable(map.get(key));
    }

    public static int colorRange(int val, int max) {
        return Mth.hsvToRgb(val / (max * 3.0F), 1.0F, 1.0F);
    }

    public static int colorRange(float val, float max) {
        return Mth.hsvToRgb(val / (max * 3.0F), 1.0F, 1.0F);
    }

    public static int colorRange(float val) {
        return Mth.hsvToRgb(val / 3.0F, 1.0F, 1.0F);
    }

    public static int colorRange(double val) {
        val = Mth.clamp(val, 0.0, 1.0);
        return Mth.hsvToRgb((float) (val / 3.0), 1.0F, 1.0F);
    }

    public static List<String> match(Pattern pattern, String string, int group) {
        Matcher matcher = pattern.matcher(string);
        ArrayList<String> result = new ArrayList<>();

        while (matcher.find()) {
            result.add(matcher.group(group));
        }

        return result;
    }

    public static <T> void with(List<T> list, int index, UnaryOperator<T> operator) {
        list.set(index, operator.apply(list.get(index)));
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable, U> U sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    public static <T> T randomEntry(List<T> value, Random random) {
        return value.get(random.nextInt(0, value.size()));
    }
}
