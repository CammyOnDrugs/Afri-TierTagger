package com.skalpha.tiertagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.skalpha.tiertagger.config.TTConfigScreen;
import com.skalpha.tiertagger.config.TierTaggerConfig;
import com.skalpha.tiertagger.model.GameMode;
import com.skalpha.tiertagger.model.PlayerInfo;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import com.skalpha.tiertagger.config.ConfigManager;
import com.skalpha.tiertagger.config.ClientUi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TierTagger implements ModInitializer {
   public static final String MOD_ID = "tiertagger";
   public static final Gson GSON = new GsonBuilder().create();
   private static final ConfigManager<TierTaggerConfig> manager = ConfigManager.createDefault(TierTaggerConfig.class, "tiertagger");
   private static final Logger logger = LoggerFactory.getLogger(TierTagger.class);
   private static final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(java.time.Duration.ofSeconds(10)).build();
   private static final Map<UUID, Component> RESULT_CACHE = new ConcurrentHashMap<>();
   private static final Map<UUID, String> RESULT_TEXT_CACHE = new ConcurrentHashMap<>();
   private static final Map<String, Component> RESULT_CACHE_BY_NAME = new ConcurrentHashMap<>();
   private static final Map<String, String> RESULT_TEXT_CACHE_BY_NAME = new ConcurrentHashMap<>();
   private static final Component SEPARATOR = Component.literal(" | ").withStyle(ChatFormatting.GRAY);
   private static final Component NULL_SENTINEL = Component.empty();

   public void onInitialize() {
      manager.saveConfig();
      CompletableFuture.runAsync(TierCache::init);
      com.skalpha.tiertagger.queue.QueueAlerts.init();
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(var0, var1x) -> {
               var0.register(ClientCommandManager.literal("afritiers-settings").executes(context -> { Minecraft client = Minecraft.getInstance(); client.execute(() -> client.setScreen(new TTConfigScreen(client.screen))); return 1; }));
               var0.register((LiteralArgumentBuilder)ClientCommandManager.literal("afritiersreload").executes(TierTagger::reloadTiers));
               var0.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal("tiertagger")
                        .then(ClientCommandManager.literal("reload").executes(TierTagger::reloadTiers)))
                     .then(ClientCommandManager.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(TierTagger::displayTierInfo))
               );
            }
         );
      Category var1 = Category.register(Identifier.fromNamespaceAndPath("tiertagger", "key"));
      ClientUi.registerKeybinding(new KeyMapping("tiertagger.keybind.gamemode", -1, var1), var0 -> {
         GameMode var1x = TierCache.findNextMode(((TierTaggerConfig)manager.getConfig()).getGameMode());
         ((TierTaggerConfig)manager.getConfig()).setGameMode(var1x.id());
         invalidateTierComponentCache();
         if (var0.player != null) {
            var0.player.displayClientMessage(Component.literal("Displayed gamemode: ").append(var1x.asStyled(false)), true);
         }
      });
      ClientUi.registerKeybinding(new KeyMapping("tiertagger.keybind.config", 45, var1), var0 -> Minecraft.getInstance().execute(() -> {
         Screen var1x = var0.screen;
         var0.setScreen(new TTConfigScreen(var1x));
      }));
   }

   public static Component appendTier(UUID var0, Component var1) {
      String var2 = var1.getString() + (manager.getConfig().isKitIdentifier()
         ? "\u0000" + com.skalpha.tiertagger.kit.KitIdentifier.inferredMode(var0).orElse("") : "");
      String var3 = RESULT_TEXT_CACHE.get(var0);
      if (var3 != null && var3.equals(var2)) {
         Component var4 = RESULT_CACHE.get(var0);
         if (var4 != null) {
            return var4;
         }
      }

      Component var6 = buildTierComponent(var0);
      if (var6 == null) {
         var6 = buildTierComponentByText(var1);
         if (var6 == null) {
            return var1;
         }
      }

      MutableComponent var5 = Component.empty();
      var5.append(var6);
      var5.append(SEPARATOR);
      var5.append(var1.copy().withStyle(var0x -> var0x.getColor() == null ? var0x.withColor(16777215) : var0x));
      RESULT_CACHE.put(var0, var5);
      RESULT_TEXT_CACHE.put(var0, var2);
      return var5;
   }

   public static Component appendTierByName(String var0, Component var1) {
      String var2 = var0.toLowerCase(java.util.Locale.ROOT);
      String var3 = var1.getString() + (manager.getConfig().isKitIdentifier()
         ? "\u0000" + com.skalpha.tiertagger.kit.KitIdentifier.byName(var0).orElse("") : "");
      String var4 = RESULT_TEXT_CACHE_BY_NAME.get(var2);
      if (var4 != null && var4.equals(var3)) {
         Component var5 = RESULT_CACHE_BY_NAME.get(var2);
         if (var5 != null) {
            return var5;
         }
      }

      Component var7 = buildTierComponentByName(var2);
      if (var7 == null) {
         return var1;
      } else {
         MutableComponent var6 = Component.empty();
         var6.append(var7);
         var6.append(SEPARATOR);
         var6.append(var1.copy().withStyle(var0x -> var0x.getColor() == null ? var0x.withColor(16777215) : var0x));
         RESULT_CACHE_BY_NAME.put(var2, var6);
         RESULT_TEXT_CACHE_BY_NAME.put(var2, var3);
         return var6;
      }
   }

   private static Component buildTierComponent(UUID var0) {
      return getPlayerTier(var0).map(TierTagger::formatRanking).orElse(null);
   }

   private static Component buildTierComponentByName(String var0) {
      return getPlayerTierByName(var0).map(TierTagger::formatRanking).orElse(null);
   }

   private static Component buildTierComponentByText(Component var0) {
      String var1 = var0.getString();
      Optional var2 = TierCache.findCachedNameInText(var1);
      return var2.isPresent() ? buildTierComponentByName((String)var2.get()) : null;
   }

   private static Component formatRanking(PlayerInfo.NamedRanking var0) {
      MutableComponent result = rankingPosition(var0.ranking());
      if (manager.getConfig().isShowIcons() && var0.mode() != null && var0.mode().icon().isPresent())
         result.append(Component.literal(var0.mode().icon().get().toString()));
      return result.append(tierText(var0.ranking(), false));
   }

   public static MutableComponent rankingPosition(PlayerInfo.Ranking ranking) {
      if (ranking.position() <= 0) return Component.empty();
      TierTaggerConfig config = manager.getConfig();
      String trophy = config.isRankTrophies() ? switch (ranking.position()) {
         case 1 -> "\uE901 "; case 2 -> "\uE902 "; case 3 -> "\uE903 "; default -> "";
      } : "";
      int color = config.isPodiumColors() ? switch (ranking.position()) {
         case 1 -> 0xFFD700; case 2 -> 0xC0C0C0; case 3 -> 0xCD7F32; default -> 0xAAAAAA;
      } : 0xAAAAAA;
      MutableComponent result = Component.empty();
      if (!trophy.isEmpty()) result.append(Component.literal(trophy).withStyle(ChatFormatting.WHITE));
      if (config.isRankNumbers()) result.append(Component.literal("#" + ranking.position() + " ").withStyle(style -> style.withColor(color)));
      return result;
   }

   public static void invalidateTierComponentCache() {
      RESULT_CACHE.clear();
      RESULT_TEXT_CACHE.clear();
      RESULT_CACHE_BY_NAME.clear();
      RESULT_TEXT_CACHE_BY_NAME.clear();
   }

   public static Optional<PlayerInfo.NamedRanking> getPlayerTier(UUID var0) {
      return TierCache.getPlayerRankings(var0).flatMap(r -> pickWithKit(r,
         manager.getConfig().isKitIdentifier() ? com.skalpha.tiertagger.kit.KitIdentifier.inferredMode(var0) : Optional.empty()));
   }

   public static Optional<PlayerInfo.NamedRanking> getPlayerTierByName(String var0) {
      return TierCache.getPlayerRankingsByName(var0).flatMap(r -> pickWithKit(r,
         manager.getConfig().isKitIdentifier() ? com.skalpha.tiertagger.kit.KitIdentifier.byName(var0) : Optional.empty()));
   }

   private static Optional<PlayerInfo.NamedRanking> pickWithKit(Map<String, PlayerInfo.Ranking> rankings, Optional<String> kit) {
      if (kit.isPresent()) {
         GameMode mode = GameMode.byId(kit.get());
         PlayerInfo.Ranking ranking = rankings.get(mode.id());
         if (ranking != null) return Optional.of(ranking.asNamed(mode));
      }
      return pickRanking(rankings);
   }

   private static Optional<PlayerInfo.NamedRanking> pickRanking(Map<String, PlayerInfo.Ranking> var0) {
      GameMode var1 = ((TierTaggerConfig)manager.getConfig()).getGameMode();
      TierTaggerConfig.HighestMode var2 = ((TierTaggerConfig)manager.getConfig()).getHighestMode();
      if (var1.isOverall()) {
         return PlayerInfo.getHighestRanking((Map<String, PlayerInfo.Ranking>)var0);
      } else {
         PlayerInfo.Ranking var3 = var0.get(var1.id());
         if (var3 != null) {
            return Optional.of(var3.asNamed(var1));
         } else {
            return var2 != TierTaggerConfig.HighestMode.ALWAYS && var2 != TierTaggerConfig.HighestMode.NOT_FOUND
               ? Optional.empty()
               : PlayerInfo.getHighestRanking((Map<String, PlayerInfo.Ranking>)var0);
         }
      }
   }

   public static Component getRankingText(PlayerInfo.Ranking var0, boolean var1) {
      return tierText(var0, var1);
   }

   private static Component tierText(PlayerInfo.Ranking var0, boolean var1) {
      String var2 = var0.tierName() != null ? var0.tierName() : var0.tier();
      if (var0.retired()) {
         var2 = "R" + var2;
      }

      int var3 = getTierColor(var2);
      MutableComponent var4 = Component.literal(var2).withStyle(var1x -> var1x.withColor(var3));
      if (var1) {
         var4.append(Component.literal(" (" + var0.points() + " pts)").withStyle(ChatFormatting.GRAY));
      }

      return var4;
   }

   public static int getTierColor(String var0) {
      return var0.startsWith("R")
         ? ((TierTaggerConfig)manager.getConfig()).getRetiredColor()
         : (Integer)((TierTaggerConfig)manager.getConfig()).getTierColors().getOrDefault(var0, 13882323);
   }

   public static int parseColor(String var0) {
      return TierTaggerConfig.parseColor(var0);
   }

   public static boolean isValidColor(String var0) {
      return TierTaggerConfig.isValidColor(var0);
   }

   private static int reloadTiers(CommandContext<FabricClientCommandSource> var0) {
      ((FabricClientCommandSource)var0.getSource())
         .sendFeedback(gradientPrefix().append(Component.literal(" Reloading Players Tiers Data").withStyle(ChatFormatting.WHITE)));
      TierCache.reload()
         .thenAccept(
            var1 -> Minecraft.getInstance()
               .execute(
                  () -> {
                     if (var1.isEmpty()) {
                        ((FabricClientCommandSource)var0.getSource())
                           .sendError(gradientPrefix().append(Component.literal(" Reload returned no players (check logs).").withStyle(ChatFormatting.RED)));
                     } else {
                        ((FabricClientCommandSource)var0.getSource())
                           .sendFeedback(
                              gradientPrefix()
                                 .append(Component.literal(" Players reloaded: ").withStyle(ChatFormatting.WHITE))
                                 .append(Component.literal(String.valueOf(var1.size())).withStyle(var0xxx -> var0xxx.withColor(5635925)))
                                 .append(Component.literal(" Players.").withStyle(ChatFormatting.WHITE))
                           );
                     }
                  }
               )
         )
         .exceptionally(
            var1 -> {
               Minecraft.getInstance()
                  .execute(
                     () -> ((FabricClientCommandSource)var0.getSource())
                        .sendError(gradientPrefix().append(Component.literal(" Reload failed: " + var1.getMessage()).withStyle(ChatFormatting.RED)))
                  );
               return null;
            }
         );
      return 0;
   }

   private static MutableComponent gradientPrefix() {
      MutableComponent var0 = Component.empty();

      for (int var1 = 0; var1 < "[Afri-TiersTagger]".length(); var1++) {
         float var2 = "[Afri-TiersTagger]".length() <= 1 ? 0.0F : (float)var1 / ("[Afri-TiersTagger]".length() - 1);
         int var3 = interpolateColor(16776960, 16766720, var2);
         var0.append(Component.literal(String.valueOf("[Afri-TiersTagger]".charAt(var1))).withStyle(var1x -> var1x.withColor(var3)));
      }

      return var0;
   }

   private static int interpolateColor(int var0, int var1, float var2) {
      int var3 = var0 >> 16 & 0xFF;
      int var4 = var0 >> 8 & 0xFF;
      int var5 = var0 & 0xFF;
      int var6 = var1 >> 16 & 0xFF;
      int var7 = var1 >> 8 & 0xFF;
      int var8 = var1 & 0xFF;
      int var9 = (int)(var3 + (var6 - var3) * var2);
      int var10 = (int)(var4 + (var7 - var4) * var2);
      int var11 = (int)(var5 + (var8 - var5) * var2);
      return var9 << 16 | var10 << 8 | var11;
   }

   private static int displayTierInfo(CommandContext<FabricClientCommandSource> var0) {
      String var1 = com.mojang.brigadier.arguments.StringArgumentType.getString(var0, "player");
      Optional var2 = ((FabricClientCommandSource)var0.getSource())
         .getWorld()
         .players()
         .stream()
         .filter(var1x -> var1x.getScoreboardName().equalsIgnoreCase(var1) || var1x.getStringUUID().equalsIgnoreCase(var1))
         .findFirst()
         .<UUID>map(Entity::getUUID)
         .flatMap(TierCache::getPlayerRankings);
      if (var2.isPresent()) {
         ((FabricClientCommandSource)var0.getSource()).sendFeedback(printPlayerInfo(var1, (Map<String, PlayerInfo.Ranking>)var2.get()));
      } else {
         ((FabricClientCommandSource)var0.getSource()).sendFeedback(Component.literal("[TierTagger] Searching..."));
         TierCache.searchPlayer(var1)
            .thenAccept(
               var2x -> Minecraft.getInstance()
                  .execute(() -> ((FabricClientCommandSource)var0.getSource()).sendFeedback(printPlayerInfo(var1, var2x.rankings())))
            )
            .exceptionally(var2x -> {
               ((FabricClientCommandSource)var0.getSource()).sendError(Component.literal("Could not find player " + var1));
               return null;
            });
      }

      return 0;
   }

   private static Component printPlayerInfo(String var0, Map<String, PlayerInfo.Ranking> var1) {
      if (var1.isEmpty()) {
         return Component.literal(var0 + " does not have any tiers.");
      } else {
         MutableComponent var2 = Component.empty().append("=== Rankings for " + var0 + " ===");
         var1.forEach((var1x, var2x) -> {
            if (var1x != null) {
               GameMode var3 = TierCache.findModeOrUgly(var1x);
               var2.append(Component.literal("\n")).append(var3.asStyled(true)).append(": ").append(getRankingText(var2x, true));
            }
         });
         return var2;
      }
   }

   public static ConfigManager<TierTaggerConfig> getManager() {
      return manager;
   }

   public static Logger getLogger() {
      return logger;
   }

   public static HttpClient getClient() {
      return client;
   }

}
