package com.floweytf.fma;

import com.floweytf.fma.events.ClientJoinServerEvent;
import com.floweytf.fma.util.FormatUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.swing.text.html.Option;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import static com.floweytf.fma.util.ChatUtil.send;
import static com.floweytf.fma.util.ChatUtil.sendWarn;
import static net.minecraft.network.chat.Component.translatable;

public class VersionChecker {
    public record Info(Result state, Optional<Version> remoteVersion) {
        public enum Result {
            NOT_READY,
            NOT_AVAILABLE,
            DISABLED,
            OUTDATED,
            LATEST,
            DEV_BUILD
        }

        public Version unwrap() {
            return remoteVersion.orElseThrow();
        }
    }

    private static final String VERSION_URL = "https://api.github.com/repos/Floweynt/flowey-monumenta-addons/releases";
    private static final Gson GSON = new Gson();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
        .build();

    private static final HttpRequest REQUEST = HttpRequest.newBuilder()
        .uri(URI.create(VERSION_URL))
        .GET()
        .build();

    private final CompletableFuture<Optional<Version>> latestVersion;

    public VersionChecker(FMAConfig config) {
        if (config.features.versionCheck) {
            latestVersion = CLIENT.sendAsync(REQUEST, HttpResponse.BodyHandlers.ofString()).thenApply(s -> {
                final var list = new ArrayList<Version>();

                try {
                    final var versions = GSON
                        .fromJson(s.body(), JsonElement.class)
                        .getAsJsonArray();

                    for (final var version : versions) {
                        final var file = version.getAsJsonObject().get("tag_name").getAsString();
                        final var preRelease = version.getAsJsonObject().get("prerelease").getAsBoolean();

                        if(preRelease && !FMAClient.features().versionCheckIncludeBeta) {
                            continue;
                        }

                        final var semVer = SemanticVersion.parse(file.substring(1));
                        list.add(semVer);
                    }
                } catch (VersionParsingException e) {
                    throw new RuntimeException(e);
                }

                list.sort(Version::compareTo);

                if(list.isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(list.get(list.size() - 1));
            });
        } else {
            latestVersion = CompletableFuture.completedFuture(Optional.empty());
        }
    }

    public void registerEvent() {
        ClientJoinServerEvent.EVENT.register(() -> {
            final var info = getVersionInfo();
            switch (info.state) {
            case OUTDATED -> send(translatable(
                "text.fma.version.common.new_version",
                FormatUtil.altText(info.unwrap().getFriendlyString())
            ));
            case NOT_READY -> sendWarn(translatable("text.fma.version.common.update_check_timeout"));
            case NOT_AVAILABLE -> sendWarn(translatable("text.fma.version.common.update_check_fail"));
            }
        });
    }

    public Info getVersionInfo() {
        if (latestVersion.isDone()) {
            if (latestVersion.isCompletedExceptionally()) {
                return new Info(Info.Result.NOT_AVAILABLE, Optional.empty());
            }

            final var res = latestVersion.join();

            if (res.isEmpty()) {
                return new Info(Info.Result.DISABLED, Optional.empty());
            }

            final var comp = res.get().compareTo(FMAClient.MOD.getMetadata().getVersion());

            if (comp == 0) {
                return new Info(Info.Result.LATEST, res);
            } else if (comp < 0) {
                return new Info(Info.Result.DEV_BUILD, res);
            } else {
                return new Info(Info.Result.OUTDATED, res);
            }
        }

        return new Info(Info.Result.NOT_READY, Optional.empty());
    }
}
