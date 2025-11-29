package com.dayssky.mma;

import com.dayssky.mma.events.ClientJoinServerEvent;
import com.dayssky.mma.util.ChatUtil;
import com.dayssky.mma.util.FormatUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.network.chat.Component;

public class VersionChecker {
    private static final String VERSION_URL = "https://api.github.com/repos/DaysSky/mma/releases";
    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.of(10L, ChronoUnit.SECONDS)).build();
    private static final HttpRequest REQUEST = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/DaysSky/mma/releases"))
            .GET()
            .build();
    private final CompletableFuture<Optional<Version>> latestVersion;

    public VersionChecker(MMAConfig config) {
        if (config.features.versionCheck) {
            this.latestVersion = CLIENT.sendAsync(REQUEST, BodyHandlers.ofString()).thenApply(s -> {
                ArrayList<Version> list = new ArrayList<>();

                try {
                    for (JsonElement version : ((JsonElement) GSON.fromJson(s.body(), JsonElement.class)).getAsJsonArray()) {
                        String file = version.getAsJsonObject().get("tag_name").getAsString();
                        boolean preRelease = version.getAsJsonObject().get("prerelease").getAsBoolean();
                        if (!preRelease || MMAClient.features().versionCheckIncludeBeta) {
                            SemanticVersion semVer = SemanticVersion.parse(file.substring(1));
                            list.add(semVer);
                        }
                    }
                } catch (VersionParsingException var8) {
                    throw new RuntimeException(var8);
                }

                list.sort(Comparable::compareTo);
                return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
            });
        } else {
            this.latestVersion = CompletableFuture.completedFuture(Optional.empty());
        }
    }

    public void init() {
        ClientJoinServerEvent.EVENT
                .register(
                        (ClientJoinServerEvent) () -> {
                            VersionChecker.Info info = this.getVersionInfo();
                            switch (info.state) {
                                case NOT_READY:
                                    ChatUtil.sendWarn(Component.translatable("text.mma.version.common.update_check_timeout"));
                                    break;
                                case NOT_AVAILABLE:
                                    ChatUtil.sendWarn(Component.translatable("text.mma.version.common.update_check_fail"));
                                case DISABLED:
                                default:
                                    break;
                                case OUTDATED:
                                    ChatUtil.send(
                                            Component.translatable("text.mma.version.common.new_version", new Object[]{FormatUtil.altText(info.unwrap().getFriendlyString())})
                                    );
                            }
                        }
                );
    }

    public VersionChecker.Info getVersionInfo() {
        if (this.latestVersion.isDone()) {
            if (this.latestVersion.isCompletedExceptionally()) {
                return new VersionChecker.Info(VersionChecker.Info.Result.NOT_AVAILABLE, Optional.empty());
            } else {
                Optional<Version> res = this.latestVersion.join();
                if (res.isEmpty()) {
                    return new VersionChecker.Info(VersionChecker.Info.Result.DISABLED, Optional.empty());
                } else {
                    int comp = res.get().compareTo(MMAClient.MOD.getMetadata().getVersion());
                    if (comp == 0) {
                        return new VersionChecker.Info(VersionChecker.Info.Result.LATEST, res);
                    } else {
                        return comp < 0
                                ? new VersionChecker.Info(VersionChecker.Info.Result.DEV_BUILD, res)
                                : new VersionChecker.Info(VersionChecker.Info.Result.OUTDATED, res);
                    }
                }
            }
        } else {
            return new VersionChecker.Info(VersionChecker.Info.Result.NOT_READY, Optional.empty());
        }
    }

    public record Info(VersionChecker.Info.Result state, Optional<Version> remoteVersion) {
        public Version unwrap() {
            return this.remoteVersion.orElseThrow();
        }

        public static enum Result {
            NOT_READY,
            NOT_AVAILABLE,
            DISABLED,
            OUTDATED,
            LATEST,
            DEV_BUILD;
        }
    }
}
