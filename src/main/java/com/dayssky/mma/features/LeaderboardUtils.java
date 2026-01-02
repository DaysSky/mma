package com.dayssky.mma.features;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.dayssky.mma.MMAClient;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

public class LeaderboardUtils {
    private static final Path OVERRIDE_PATH = FabricLoader.getInstance().getConfigDir().resolve("mma-leaderboard.json");
    private static Map<String, String> conversionMap = Collections.emptyMap();

    public static void init() {
        final Map<String, String> merged = new HashMap<>();

        try (InputStream stream = LeaderboardUtils.class.getResourceAsStream("/assets/mma/mma-leaderboard.json")) {
            if (stream != null) {
                final Map<String, String> defaults = MMAClient.GSON.fromJson(
                        new InputStreamReader(stream),
                        new TypeToken<Map<String, String>>() {}.getType()
                );
                if (defaults != null) {
                    merged.putAll(defaults);
                }
            }
        } catch (IOException e) {
            MMAClient.LOGGER.warn("Failed to load bundled leaderboard aliases", e);
        }

        if (Files.exists(OVERRIDE_PATH)) {
            try (var reader = Files.newBufferedReader(OVERRIDE_PATH)) {
                final Map<String, String> overrides = MMAClient.GSON.fromJson(
                        reader,
                        new TypeToken<Map<String, String>>() {}.getType()
                );
                if (overrides != null) {
                    merged.putAll(overrides);
                }
            } catch (IOException e) {
                MMAClient.LOGGER.warn("Failed to load leaderboard alias overrides from", OVERRIDE_PATH, e);
            }
        }

        conversionMap = Collections.unmodifiableMap(merged);
    }

    public static String resolve(String alias) {
        return conversionMap.getOrDefault(alias, alias);
    }

    public static Set<String> getKeys() {
        return conversionMap.keySet();
    }
}
