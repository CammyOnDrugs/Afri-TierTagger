package com.skalpha.tiertagger;

import com.skalpha.tiertagger.config.TierTaggerConfig;
import com.skalpha.tiertagger.model.GameMode;
import com.skalpha.tiertagger.model.PlayerInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;

public class TierCache {
   private static final List<GameMode> GAMEMODES = new ArrayList<>();
   private static final Map<String, PlayerInfo> PLAYER_CACHE = new ConcurrentHashMap<>();
   private static final Map<String, UUID> NAME_TO_UUID = new ConcurrentHashMap<>();
   private static final Map<UUID, String> UUID_NAME_CACHE = new ConcurrentHashMap<>();
   private static final Set<UUID> NOT_FOUND_CACHE = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> FETCHED_UUIDS = ConcurrentHashMap.newKeySet();
   private static final AtomicBoolean LOADING = new AtomicBoolean(false);
   private static volatile long loadingSince = 0L;
   private static volatile boolean loaded = false;
   private static volatile CompletableFuture<List<PlayerInfo>> activeReload = null;
   private static final long LOADING_TIMEOUT_MS = 30000L;
   private static final ScheduledExecutorService AUTO_RELOAD = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "AfriTiers-AutoReload");
      t.setDaemon(true);
      return t;
   });
   private static final List<String> DEFAULT_MODE_IDS = List.of(
      "overall", "sword", "axe", "mace", "uhc", "smp", "diapot", "vanilla", "nethpot", "ogv", "creeper", "speed", "cart", "bed", "diasmp", "spear", "diavanilla"
   );
   private static volatile int findCacheVersion = 0;
   private static volatile int lastFindVersion = -1;
   private static volatile List<String> sortedNames = null;

   public static void init() {
      try {
         reload().exceptionally(t -> {
            TierTagger.getLogger().error("[Afri-TiersTagger] Failed to load players!", t);
            return null;
         });
      } catch (Throwable var1) {
         TierTagger.getLogger().error("[Afri-TiersTagger] init() crashed", var1);
      }

      AUTO_RELOAD.scheduleAtFixedRate(() -> {
         try {
            reload().exceptionally(t -> {
               TierTagger.getLogger().error("[Afri-TiersTagger] Auto-reload failed", t);
               return null;
            });
         } catch (Throwable var1x) {
            TierTagger.getLogger().error("[Afri-TiersTagger] Auto-reload crashed", var1x);
         }
      }, 2L, 2L, TimeUnit.MINUTES);
   }

   public static CompletableFuture<List<PlayerInfo>> reload() {
      if (LOADING.get() && System.currentTimeMillis() - loadingSince > 30000L) {
         TierTagger.getLogger().warn("[Afri-TiersTagger] LOADING flag stuck, force-resetting");
         LOADING.set(false);
         activeReload = null;
      }

      CompletableFuture<List<PlayerInfo>> existing = activeReload;
      if (existing != null) {
         return existing;
      } else if (!LOADING.compareAndSet(false, true)) {
         existing = activeReload;
         return existing != null ? existing : CompletableFuture.completedFuture(Collections.emptyList());
      } else {
         loadingSince = System.currentTimeMillis();
         TierTagger.getLogger().info("[Afri-TiersTagger] Starting players.js reload...");
         CompletableFuture<List<PlayerInfo>> future = PlayerInfo.fetchAll(
               TierTagger.getClient(), ((TierTaggerConfig)TierTagger.getManager().getConfig()).getApiUrl()
            )
            .thenApply(list -> {
               synchronized (TierCache.class) {
                  PLAYER_CACHE.clear();
                  NAME_TO_UUID.clear();
                  UUID_NAME_CACHE.clear();
                  NOT_FOUND_CACHE.clear();

                  for (PlayerInfo p : list) {
                     if (p.name() != null && !p.name().isBlank()) {
                        String key = p.name().toLowerCase(Locale.ROOT);
                        PLAYER_CACHE.put(key, p);
                        UUID uuid = parseUuid(p.uuid());
                        if (uuid != null) {
                           NAME_TO_UUID.put(key, uuid);
                        }
                     }
                  }

                  rebuildGamemodes((List<PlayerInfo>)list);
                  loaded = true;
                  TierTagger.getLogger().info("[Afri-TiersTagger] Cache updated: {} players, {} gamemodes", list.size(), GAMEMODES.size());
                  return list;
               }
            })
            .exceptionally(t -> {
               synchronized (TierCache.class) {
                  TierTagger.getLogger().error("[Afri-TiersTagger] Reload failed, keeping existing cache ({} players)", PLAYER_CACHE.size(), t);
               }

               return Collections.emptyList();
            })
            .whenComplete((res, t) -> {
               LOADING.set(false);
               activeReload = null;
               loadingSince = 0L;
               TierTagger.invalidateTierComponentCache();
            });
         activeReload = future;
         return future;
      }
   }

   private static void rebuildGamemodes(List<PlayerInfo> players) {
      GAMEMODES.clear();
      GAMEMODES.add(GameMode.OVERALL);
      Set<String> seen = new LinkedHashSet<>();

      for (PlayerInfo p : players) {
         seen.addAll(p.rankings().keySet());
      }

      for (String id : seen) {
         GameMode mode = GameMode.byId(id);
         if (!mode.isNone() && !mode.isOverall() && !isFilteredMode(mode.id())) {
            GAMEMODES.add(mode);
         }
      }

      if (GAMEMODES.size() <= 1) {
         GAMEMODES.clear();
         GAMEMODES.add(GameMode.NONE);
      }
   }

   private static boolean isFilteredMode(String id) {
      return "debuff".equalsIgnoreCase(id);
   }

   public static List<GameMode> getGamemodesOrDefault() {
      if (GAMEMODES.size() > 1) {
         return Collections.unmodifiableList(GAMEMODES);
      } else {
         List<GameMode> defaults = new ArrayList<>();
         defaults.add(GameMode.OVERALL);

         for (String id : DEFAULT_MODE_IDS) {
            if (!id.equals("overall")) {
               GameMode mode = GameMode.byId(id);
               if (!mode.isNone() && !mode.isOverall()) {
                  defaults.add(mode);
               }
            }
         }

         return Collections.unmodifiableList(defaults);
      }
   }

   public static List<GameMode> getGamemodes() {
      return GAMEMODES.isEmpty() ? Collections.singletonList(GameMode.NONE) : Collections.unmodifiableList(GAMEMODES);
   }

   public static Optional<Map<String, PlayerInfo.Ranking>> getPlayerRankings(UUID uuid) {
      if (NOT_FOUND_CACHE.contains(uuid)) {
         return Optional.empty();
      } else {
         String name = UUID_NAME_CACHE.get(uuid);
         if (name == null) {
            name = resolveName(uuid);
            if (name == null) {
               NOT_FOUND_CACHE.add(uuid);
               return Optional.empty();
            }

            UUID_NAME_CACHE.put(uuid, name);
         }

         PlayerInfo info = PLAYER_CACHE.get(name.toLowerCase(Locale.ROOT));
         if (info != null) {
            return Optional.of(info.rankings());
         } else {
            NOT_FOUND_CACHE.add(uuid);
            return Optional.empty();
         }
      }
   }

   public static Optional<Map<String, PlayerInfo.Ranking>> getPlayerRankingsByName(String name) {
      if (name != null && !name.isBlank()) {
         PlayerInfo info = PLAYER_CACHE.get(name.toLowerCase(Locale.ROOT));
         return info != null ? Optional.of(info.rankings()) : Optional.empty();
      } else {
         return Optional.empty();
      }
   }

   public static CompletableFuture<PlayerInfo> searchPlayer(String query) {
      String key = query.toLowerCase(Locale.ROOT);
      return reload().thenApply(list -> {
         PlayerInfo p = PLAYER_CACHE.get(key);
         if (p == null) {
            throw new NoSuchElementException("Player not found: " + query);
         } else {
            return p;
         }
      });
   }

   public static Optional<String> findCachedNameInText(String text) {
      if (text != null && !text.isBlank() && !PLAYER_CACHE.isEmpty()) {
         String lower = text.toLowerCase(Locale.ROOT);
         String bestMatch = null;
         int bestLen = 0;

         for (String name : PLAYER_CACHE.keySet()) {
            if (lower.contains(name) && name.length() > bestLen) {
               bestMatch = name;
               bestLen = name.length();
            }
         }

         return Optional.ofNullable(bestMatch);
      } else {
         return Optional.empty();
      }
   }

   public static UUID getPlayerUuid(String name) {
      return NAME_TO_UUID.get(name.toLowerCase(Locale.ROOT));
   }

   public static Collection<PlayerInfo> getPlayerCacheValues() {
      return Collections.unmodifiableCollection(PLAYER_CACHE.values());
   }

   public static void clearCache() {
      PLAYER_CACHE.clear();
      NAME_TO_UUID.clear();
      UUID_NAME_CACHE.clear();
      NOT_FOUND_CACHE.clear();
      FETCHED_UUIDS.clear();
      loaded = false;
   }

   public static GameMode findNextMode(GameMode current) {
      return GAMEMODES.isEmpty() ? GameMode.NONE : GAMEMODES.get((GAMEMODES.indexOf(current) + 1) % GAMEMODES.size());
   }

   public static Optional<GameMode> findMode(String id) {
      return GAMEMODES.stream().filter(m -> m.id().equalsIgnoreCase(id)).findFirst();
   }

   public static GameMode findModeOrUgly(String id) {
      return findMode(id).orElseGet(() -> GameMode.byId(id));
   }

   private static String resolveName(UUID uuid) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() == null) {
         return null;
      } else {
         net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
         return info == null ? null : info.getProfile().name();
      }
   }

   private static UUID parseUuid(String raw) {
      if (raw != null && !raw.isBlank()) {
         try {
            return UUID.fromString(raw);
         } catch (IllegalArgumentException var4) {
            try {
               String dashed = raw.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
               return UUID.fromString(dashed);
            } catch (Exception var3) {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private TierCache() {
   }
}
