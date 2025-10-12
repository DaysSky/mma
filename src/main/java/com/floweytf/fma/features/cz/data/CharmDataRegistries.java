package com.floweytf.fma.features.cz.data;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.util.LateInit;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharmDataRegistries {
   private static final Path CACHE_PATH = FabricLoader.getInstance().getConfigDir().resolve("fma-cached-charm-effects.json");
   private static final Logger LOGGER = LoggerFactory.getLogger("CharmDataRegistries");
   private static final String API_URL = "https://api.playmonumenta.com/zenith_charm_effects";
   private static final LateInit<CharmDataRegistries> MAIN = new LateInit<>();
   private static final LateInit<CharmDataRegistries> DUMMY = new LateInit<>();
   public final CharmDataRegistry<CharmEffectType> charmEffectType;
   public final CharmDataRegistry<ZenithAbility> zenithAbilities;

   private CharmDataRegistries(CharmDataRegistry<CharmEffectType> charmEffectType, CharmDataRegistry<ZenithAbility> zenithAbilities) {
      this.charmEffectType = charmEffectType;
      this.zenithAbilities = zenithAbilities;
   }

   private static CharmDataRegistries fromJson(JsonArray data) {
      CharmDataRegistry<CharmEffectType> charmEffectType = new CharmDataRegistry<>();
      CharmDataRegistry<ZenithAbility> zenithAbilities = new CharmDataRegistry<>();
      int effectOrdinal = 0;

      for (JsonElement element : data) {
         JsonObject entry = element.getAsJsonObject();
         String effectName = entry.get("effectName").getAsString();
         String ability = entry.get("ability").getAsString();
         boolean isOnlyPositive = entry.get("isOnlyPositive").getAsBoolean();
         boolean isPercent = entry.get("isPercent").getAsBoolean();
         double variance = entry.get("variance").getAsDouble();
         double effectCap = entry.get("effectCap").getAsDouble();
         String tree = entry.get("tree").getAsString();
         int maxRarity = entry.get("maxRarity").getAsInt();
         JsonArray rarityValuesJson = entry.get("rarityValues").getAsJsonArray();
         double[] rarityValues = new double[rarityValuesJson.size()];
         int i = 0;

         for (JsonElement rarityValue : rarityValuesJson) {
            rarityValues[i++] = rarityValue.getAsDouble();
         }

         ZenithAbility zenithAbility = zenithAbilities.byName.computeIfAbsent(ability, name -> new ZenithAbility(name, ZenithTree.byName(tree).orElseThrow()));
         Preconditions.checkState(effectName.startsWith(zenithAbility.name + " "), "'%s' does not start with '%s'", effectName, zenithAbility.name);
         charmEffectType.byName
            .put(
               effectName,
               new CharmEffectType(
                  effectOrdinal++,
                  effectName.replace(zenithAbility.name + " ", ""),
                  zenithAbility,
                  isOnlyPositive,
                  isPercent,
                  variance,
                  effectCap,
                  CharmEffectRarity.values()[CharmEffectRarity.COMMON.ordinal() + maxRarity - 1],
                  rarityValues
               )
            );
      }

      return new CharmDataRegistries(charmEffectType, zenithAbilities);
   }

   private static CharmDataRegistries fromStream(InputStream stream) {
      Preconditions.checkState(stream != null);
      JsonArray data = (JsonArray)FMAClient.GSON.fromJson(new InputStreamReader(stream), JsonArray.class);
      return fromJson(data);
   }

   private static CharmDataRegistries fromClassPath(String source) throws IOException {
      CharmDataRegistries var2;
      try (InputStream reader = CharmDataRegistries.class.getResourceAsStream(source)) {
         var2 = fromStream(reader);
      }

      return var2;
   }

   private static CharmDataRegistries fromPath(Path path) throws IOException {
      CharmDataRegistries var2;
      try (InputStream reader = Files.newInputStream(path)) {
         var2 = fromStream(reader);
      }

      return var2;
   }

   public static void init() throws IOException {
      DUMMY.init(fromClassPath("/assets/fma/zenith_charm_config_dummy.json"));

      try {
         LOGGER.info("GET {}", "https://api.playmonumenta.com/zenith_charm_effects");
         HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.of(10L, ChronoUnit.SECONDS)).build();
         HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.playmonumenta.com/zenith_charm_effects")).build();
         Path temp = Files.createTempFile("flowey-monumenta-addons", ".json");
         client.send(request, BodyHandlers.ofFile(temp));
         MAIN.init(fromPath(temp));
         Files.move(temp, CACHE_PATH, StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception var3) {
         LOGGER.warn("failed to fetch zenith charm effects, falling back to cache!", var3);
         MAIN.init(fromPath(CACHE_PATH));
      }
   }

   public static CharmDataRegistries getMain() {
      return MAIN.get();
   }

   public static CharmDataRegistries getDummy() {
      return DUMMY.get();
   }
}
