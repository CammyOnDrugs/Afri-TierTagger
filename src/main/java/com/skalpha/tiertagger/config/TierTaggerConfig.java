package com.skalpha.tiertagger.config;

import com.google.gson.internal.LinkedTreeMap;
import com.skalpha.tiertagger.TierCache;
import com.skalpha.tiertagger.model.GameMode;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.uku3lig.ukulib.config.option.StringTranslatable;

public class TierTaggerConfig implements Serializable {
   private boolean enabled = true;
   private String gameMode = "overall";
   private boolean showRetired = true;
   private TierTaggerConfig.HighestMode highestMode = TierTaggerConfig.HighestMode.NOT_FOUND;
   private boolean showIcons = true;
   private boolean rankNumbers = true;
   private boolean rankTrophies = true;
   private boolean podiumColors = true;
   public boolean isRankNumbers() { return rankNumbers; }
   public void setRankNumbers(boolean value) { rankNumbers = value; }
   public boolean isRankTrophies() { return rankTrophies; }
   public void setRankTrophies(boolean value) { rankTrophies = value; }
   public boolean isPodiumColors() { return podiumColors; }
   public void setPodiumColors(boolean value) { podiumColors = value; }
   private boolean playerList = true;
   private int retiredColor = 10671871;
   private double sliderValue = 50.0;
   public int tierlistPage = 0;
   private LinkedTreeMap<String, Integer> tierColors = defaultColors();
   private boolean kitIdentifier = false;
   private boolean waitlistAlerts = true;
   private boolean waitlistSound = true;
   private boolean ticketAlerts = true;
   private boolean ticketSound = true;
   private AlertLocation waitlistLocation = AlertLocation.TITLE;
   private AlertLocation ticketLocation = AlertLocation.TITLE;

   public boolean isWaitlistAlerts() { return waitlistAlerts; }
   public void setWaitlistAlerts(boolean value) { waitlistAlerts = value; }
   public boolean isWaitlistSound() { return waitlistSound; }
   public void setWaitlistSound(boolean value) { waitlistSound = value; }
   public boolean isTicketAlerts() { return ticketAlerts; }
   public void setTicketAlerts(boolean value) { ticketAlerts = value; }
   public boolean isTicketSound() { return ticketSound; }
   public void setTicketSound(boolean value) { ticketSound = value; }
   public AlertLocation getWaitlistLocation() { return waitlistLocation == null ? AlertLocation.TITLE : waitlistLocation; }
   public void setWaitlistLocation(AlertLocation value) { waitlistLocation = value; }
   public AlertLocation getTicketLocation() { return ticketLocation == null ? AlertLocation.TITLE : ticketLocation; }
   public void setTicketLocation(AlertLocation value) { ticketLocation = value; }

   public enum AlertLocation implements StringTranslatable {
      CHAT("chat"), TITLE("title");
      private final String value;
      AlertLocation(String value) { this.value = value; }
      public String getName() { return value; }
      public String getTranslationKey() { return "tiertagger.alerts.location." + value; }
   }

   public GameMode getGameMode() {
      List<GameMode> var1 = TierCache.getGamemodesOrDefault();
      Optional<GameMode> var2 = var1.stream().filter(var1x -> var1x.id().equalsIgnoreCase(this.gameMode)).findFirst();
      if (var2.isPresent()) {
         return (GameMode)var2.get();
      } else {
         GameMode var3 = GameMode.byId(this.gameMode);
         if (!var3.isNone()) {
            Optional<GameMode> var4 = var1.stream().filter(var1x -> var1x.id().equalsIgnoreCase(var3.id())).findFirst();
            if (var4.isPresent()) {
               return (GameMode)var4.get();
            }
         }

         GameMode var5 = (GameMode)var1.getFirst();
         if (!var5.isNone()) {
            this.gameMode = var5.id();
         }

         return var5;
      }
   }

   public static LinkedTreeMap<String, Integer> defaultColors() {
      LinkedTreeMap var0 = new LinkedTreeMap();
      var0.put("HT1", 15252026);
      var0.put("LT1", 14005077);
      var0.put("HT2", 12899303);
      var0.put("LT2", 10528690);
      var0.put("HT3", 16293722);
      var0.put("LT3", 13007682);
      var0.put("HT4", 8483994);
      var0.put("LT4", 6642553);
      var0.put("HT5", 9405096);
      var0.put("LT5", 6642553);
      return var0;
   }

   public String getRetiredColorHex() {
      return "#" + String.format("%06X", this.retiredColor);
   }

   public void setRetiredColorHex(String var1) {
      this.retiredColor = parseColor(var1);
   }

   public LinkedTreeMap<String, String> getTierColorsHex() {
      LinkedTreeMap var1 = new LinkedTreeMap();

      for (Entry var2 : this.tierColors.entrySet()) {
         var1.put((String)var2.getKey(), "#" + String.format("%06X", var2.getValue()));
      }

      return var1;
   }

   public void setTierColorHex(String var1, String var2) {
      this.tierColors.put(var1, parseColor(var2));
   }

   public static int parseColor(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.trim();
         if (var1.startsWith("#")) {
            var1 = var1.substring(1);
         }

         try {
            if (var1.length() == 3) {
               int var2 = Integer.parseInt(var1.substring(0, 1) + var1.substring(0, 1), 16);
               int var3 = Integer.parseInt(var1.substring(1, 2) + var1.substring(1, 2), 16);
               int var4 = Integer.parseInt(var1.substring(2, 3) + var1.substring(2, 3), 16);
               return var2 << 16 | var3 << 8 | var4;
            } else {
               return var1.length() == 6 ? Integer.parseInt(var1, 16) : Integer.parseInt(var1);
            }
         } catch (NumberFormatException var5) {
            return 13882323;
         }
      } else {
         return 13882323;
      }
   }

   public static boolean isValidColor(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.trim();
         if (var1.startsWith("#")) {
            var1 = var1.substring(1);
         }

         return var1.matches("[0-9A-Fa-f]{6}") || var1.matches("[0-9A-Fa-f]{3}") || var1.matches("\\d+");
      } else {
         return false;
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean isShowRetired() {
      return this.showRetired;
   }

   public TierTaggerConfig.HighestMode getHighestMode() {
      return this.highestMode;
   }

   public boolean isShowIcons() {
      return this.showIcons;
   }

   public boolean isPlayerList() {
      return this.playerList;
   }

   public int getRetiredColor() {
      return this.retiredColor;
   }

   public double getSliderValue() {
      return this.sliderValue;
   }

   public int getTierlistPage() {
      return this.tierlistPage;
   }

   public LinkedTreeMap<String, Integer> getTierColors() {
      return this.tierColors;
   }

   public String getApiUrl() { return "https://afritiers.co.za"; }

   public boolean isKitIdentifier() { return kitIdentifier; }

   public void setKitIdentifier(boolean enabled) { kitIdentifier = enabled; }

   public void setEnabled(boolean var1) {
      this.enabled = var1;
   }

   public void setGameMode(String var1) {
      this.gameMode = var1;
   }

   public void setShowRetired(boolean var1) {
      this.showRetired = var1;
   }

   public void setHighestMode(TierTaggerConfig.HighestMode var1) {
      this.highestMode = var1;
   }

   public void setShowIcons(boolean var1) {
      this.showIcons = var1;
   }

   public void setPlayerList(boolean var1) {
      this.playerList = var1;
   }

   public void setRetiredColor(int var1) {
      this.retiredColor = var1;
   }

   public void setSliderValue(double var1) {
      this.sliderValue = var1;
   }

   public void setTierlistPage(int var1) {
      this.tierlistPage = var1;
   }

   public void setTierColors(LinkedTreeMap<String, Integer> var1) {
      this.tierColors = var1;
   }

   public TierTaggerConfig() {
   }

   public static enum HighestMode implements StringTranslatable {
      NEVER("never", "tiertagger.highest.never"),
      NOT_FOUND("not_found", "tiertagger.highest.not_found"),
      ALWAYS("always", "tiertagger.highest.always");

      private final String name;
      private final String translationKey;

      public String getName() {
         return this.name;
      }

      public String getTranslationKey() {
         return this.translationKey;
      }

      private HighestMode(final String name, final String translationKey) {
         this.name = name;
         this.translationKey = translationKey;
      }
   }
}
