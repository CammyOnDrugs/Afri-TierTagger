package com.skalpha.tiertagger.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.skalpha.tiertagger.TierCache;
import com.skalpha.tiertagger.TierTagger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

public record PlayerInfo(
   String uuid,
   String name,
   Map<String, PlayerInfo.Ranking> rankings,
   String region,
   int points,
   int overall,
   List<PlayerInfo.Badge> badges,
   boolean combatMaster
) {
   private static final SecureRandom RANDOM = new SecureRandom();

   public static CompletableFuture<List<PlayerInfo>> fetchAll(HttpClient client, String baseUrl) {
      String url = "https://afritiers.co.za/api/players";
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
         .timeout(java.time.Duration.ofSeconds(15))
         .header("User-Agent", "Afri-TiersTagger/2.7.0")
         .header("Cache-Control", "no-cache, no-store, must-revalidate")
         .header("Pragma", "no-cache")
         .GET()
         .build();
      return client.sendAsync(request, com.skalpha.tiertagger.net.PublicHttp.limitedUtf8(8_000_000)).thenApply(response -> {
         if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
         } else {
            throw new RuntimeException("rankings returned HTTP " + response.statusCode());
         }
      }).thenApply(PlayerInfo::parsePlayersJs).whenComplete((res, err) -> {
         if (err != null) {
            TierTagger.getLogger().warn("[Afri-TiersTagger] Error fetching rankings from {}", url, err);
         } else {
            TierTagger.getLogger().info("[Afri-TiersTagger] Parsed {} players from {}", res.size(), url);
         }
      });
   }

   static List<PlayerInfo> parsePlayersJs(String json) {
      JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
      JsonArray array = root.getAsJsonArray("players");
      if (array == null) throw new JsonParseException("Missing public players array");
      List<PlayerInfo> players = new ArrayList<>(array.size());
      for (JsonElement element : array) players.add(parsePlayer(element.getAsJsonObject()));
      assignOverallPositions(players);
      return players;
   }

   static void assignOverallPositions(List<PlayerInfo> players) {
      List<PlayerInfo> ranked = players.stream().filter(player -> !player.rankings().isEmpty())
         .sorted(Comparator.comparingInt(PlayerInfo::points).reversed()).toList();
      Map<PlayerInfo, Integer> positions = new java.util.IdentityHashMap<>();
      for (int index = 0; index < ranked.size(); index++) positions.put(ranked.get(index), index + 1);
      for (int index = 0; index < players.size(); index++) {
         PlayerInfo player = players.get(index);
         int position = positions.getOrDefault(player, 0);
         player.rankings().replaceAll((mode, previous) -> new Ranking(previous.tierName(), previous.tier(), previous.retired(), previous.points(), position));
         players.set(index, new PlayerInfo(player.uuid(), player.name(), player.rankings(), player.region(), player.points(), position, player.badges(), player.combatMaster()));
      }
   }

   private static PlayerInfo parsePlayer(JsonObject obj) {
      String name = getString(obj, "name");
      String uuid = getString(obj, "uuid");
      String region = getString(obj, "region");
      int totalPoints = getInt(obj, "totalPoints", 0);
      Map<String, PlayerInfo.Ranking> rankings = new HashMap<>();
      JsonArray tiers = obj.getAsJsonArray("tiers");
      if (tiers != null) {
         for (JsonElement t : tiers) {
            JsonObject tierObj = t.getAsJsonObject();
            String mode = getString(tierObj, "mode");
            String level = getString(tierObj, "level");
            boolean retired = getBoolean(tierObj, "retired", false);
            if (mode != null && !mode.isBlank() && !"debuff".equalsIgnoreCase(mode)) {
               GameMode canonical = GameMode.byId(mode);
               rankings.put(canonical.id(), new PlayerInfo.Ranking(level, level, retired, getInt(tierObj, "points", 0)));
            }
         }
      }

      return new PlayerInfo(uuid, name, rankings, region, totalPoints, 0, Collections.emptyList(), false);
   }

   public static Optional<PlayerInfo.NamedRanking> getHighestRanking(Map<String, PlayerInfo.Ranking> rankings) {
      return rankings.entrySet()
         .stream()
         .filter(e -> e.getKey() != null)
         .min(
            Comparator.<Entry<String, PlayerInfo.Ranking>, Boolean>comparing(e -> e.getValue().retired)
               .thenComparing(e -> tierRankValue(e.getValue().tierName))
         )
         .map(e -> e.getValue().asNamed(TierCache.findModeOrUgly(e.getKey())));
   }

   private static int tierRankValue(String tier) {
      if (tier == null) {
         return 999;
      } else {
         String cleaned = tier.startsWith("R") ? tier.substring(1) : tier;

         try {
            boolean ht = cleaned.startsWith("HT");
            int num = Integer.parseInt(cleaned.replaceAll("[A-Z]", ""));
            return num * 2 + (ht ? 0 : 1);
         } catch (Exception var4) {
            return 999;
         }
      }
   }

   public List<PlayerInfo.NamedRanking> getSortedTiers() {
      List<PlayerInfo.NamedRanking> tiers = new ArrayList<>(
         this.rankings.entrySet().stream().map(e -> e.getValue().asNamed(TierCache.findModeOrUgly(e.getKey()))).toList()
      );
      tiers.sort(
         Comparator.<PlayerInfo.NamedRanking, Boolean>comparing(a -> a.ranking().retired, Boolean::compare)
            .thenComparingInt(a -> tierRankValue(a.ranking().tierName()))
      );
      return tiers;
   }

   public PlayerInfo.PointInfo getPointInfo() {
      if (this.points >= 400) {
         return PlayerInfo.PointInfo.COMBAT_GRANDMASTER;
      } else if (this.points >= 250) {
         return PlayerInfo.PointInfo.COMBAT_MASTER;
      } else if (this.points >= 100) {
         return PlayerInfo.PointInfo.COMBAT_ACE;
      } else if (this.points >= 50) {
         return PlayerInfo.PointInfo.COMBAT_SPECIALIST;
      } else if (this.points >= 20) {
         return PlayerInfo.PointInfo.COMBAT_CADET;
      } else if (this.points >= 10) {
         return PlayerInfo.PointInfo.COMBAT_NOVICE;
      } else {
         return this.points >= 1 ? PlayerInfo.PointInfo.ROOKIE : PlayerInfo.PointInfo.UNRANKED;
      }
   }

   public int getRegionColor() {
      if (this.region == null) {
         return 16777215;
      } else {
         String var1 = this.region.toUpperCase(Locale.ROOT);

         return switch (var1) {
            case "NA" -> 16738926;
            case "EU" -> 7012206;
            case "SA" -> 16750848;
            case "AU" -> 16167531;
            case "ME" -> 16767334;
            case "AS" -> 12745632;
            case "AF", "ZA" -> 6770343;
            default -> 16777215;
         };
      }
   }

   private static String getString(JsonObject obj, String key) {
      return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
   }

   private static int getInt(JsonObject obj, String key, int def) {
      try {
         return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : def;
      } catch (Exception var4) {
         return def;
      }
   }

   private static boolean getBoolean(JsonObject obj, String key, boolean def) {
      try {
         return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsBoolean() : def;
      } catch (Exception var4) {
         return def;
      }
   }

   public record Badge(String title, String desc) {
   }

   public record NamedRanking(@Nullable GameMode mode, PlayerInfo.Ranking ranking) {
   }

   public static enum PointInfo {
      COMBAT_GRANDMASTER("Combat Grandmaster", 15124002, 16638023),
      COMBAT_MASTER("Combat Master", 16494651, 16765242),
      COMBAT_ACE("Combat Ace", 13445212, 14046324),
      COMBAT_SPECIALIST("Combat Specialist", 11368664, 13083624),
      COMBAT_CADET("Combat Cadet", 9605593, 11381986),
      COMBAT_NOVICE("Combat Novice", 9605593, 16777215),
      ROOKIE("Rookie", 7106936, 9148316),
      UNRANKED("Unranked", 16777215, 16777215);

      private final String title;
      private final int color;
      private final int accentColor;

      public String getTitle() {
         return this.title;
      }

      public int getColor() {
         return this.color;
      }

      public int getAccentColor() {
         return this.accentColor;
      }

      private PointInfo(final String title, final int color, final int accentColor) {
         this.title = title;
         this.color = color;
         this.accentColor = accentColor;
      }
   }

   public record Ranking(String tierName, String tier, boolean retired, int points, int position) {
      public Ranking(String tierName, String tier, boolean retired, int points) { this(tierName, tier, retired, points, 0); }
      public PlayerInfo.NamedRanking asNamed(@Nullable GameMode mode) {
         return new PlayerInfo.NamedRanking(mode, this);
      }
   }
}
