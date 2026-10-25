package com.skalpha.tiertagger.config;

import com.skalpha.tiertagger.TierCache;
import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.tierlist.PlayerSearchScreen;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import net.uku3lig.ukulib.config.option.CyclingOption;
import net.uku3lig.ukulib.config.option.InputOption;
import net.uku3lig.ukulib.config.option.ScreenOpenButton;
import net.uku3lig.ukulib.config.option.WidgetCreator;
import net.uku3lig.ukulib.config.option.widget.ButtonTab;
import net.uku3lig.ukulib.config.screen.TabbedConfigScreen;

public class TTConfigScreen extends TabbedConfigScreen<TierTaggerConfig> {
   private boolean isInitialized = false;
   private int selectedTabIndex = 0;

   public TTConfigScreen(Screen var1) {
      super("Afri-TiersTagger Config", var1, TierTagger.getManager());
   }

   protected Tab[] getTabs(TierTaggerConfig var1) {
      return new Tab[]{new TTConfigScreen.MainSettingsTab(), new TTConfigScreen.ColorsTab(), new TTConfigScreen.AlertsTab()};
   }

   public void refresh(int var1) {
      this.selectedTabIndex = var1;
      this.clearChildren();
      this.init();
   }

   protected void init() {
      super.init();
      this.children()
         .stream()
         .filter(var0 -> var0 instanceof TabNavigationWidget)
         .map(var0 -> (TabNavigationWidget)var0)
         .findFirst()
         .ifPresent(var1 -> var1.selectTab(this.selectedTabIndex, false));
      this.isInitialized = true;
   }

   protected Collection<ClickableWidget> getInvalidOptions() {
      return (Collection<ClickableWidget>)(!this.isInitialized ? Collections.emptyList() : super.getInvalidOptions());
   }

   public class ColorsTab extends ButtonTab<TierTaggerConfig> {
      public ColorsTab() {
         super("tiertagger.colors", TTConfigScreen.this.manager);
      }

      protected WidgetCreator[] getWidgets(TierTaggerConfig var1) {
         ArrayList<WidgetCreator> var2 = new ArrayList();
         var2.add(new ColorInputOption("tiertagger.colors.retired", var1.getRetiredColorHex(), var1x -> {
            var1.setRetiredColorHex(var1x);
            TierTagger.invalidateTierComponentCache();
         }));

         for (Entry var3 : var1.getTierColorsHex()
            .entrySet()
            .stream()
            .sorted(
               Comparator.comparingInt(
                  var0 -> Integer.parseInt(((String)var0.getKey()).replaceAll("[^0-9]", "")) * 2 + (((String)var0.getKey()).startsWith("LT") ? 1 : 0)
               )
            )
            .toList()) {
            String var5 = (String)var3.getKey();
            var2.add(new ColorInputOption("tiertagger.colors." + var5, (String)var3.getValue(), var2x -> {
               var1.setTierColorHex(var5, var2x);
               TierTagger.invalidateTierComponentCache();
            }));
         }

         return var2.toArray(WidgetCreator[]::new);
      }
   }

   public class AlertsTab extends ButtonTab<TierTaggerConfig> {
      public AlertsTab() { super("tiertagger.alerts", TTConfigScreen.this.manager); }
      protected WidgetCreator[] getWidgets(TierTaggerConfig config) {
         return new WidgetCreator[]{
            CyclingOption.ofBoolean("tiertagger.alerts.waitlist", config.isWaitlistAlerts(), config::setWaitlistAlerts),
            CyclingOption.ofTranslatableEnum("tiertagger.alerts.waitlistLocation", TierTaggerConfig.AlertLocation.class, config.getWaitlistLocation(), config::setWaitlistLocation),
            CyclingOption.ofBoolean("tiertagger.alerts.waitlistSound", config.isWaitlistSound(), config::setWaitlistSound),
            CyclingOption.ofBoolean("tiertagger.alerts.ticket", config.isTicketAlerts(), config::setTicketAlerts),
            CyclingOption.ofTranslatableEnum("tiertagger.alerts.ticketLocation", TierTaggerConfig.AlertLocation.class, config.getTicketLocation(), config::setTicketLocation),
            CyclingOption.ofBoolean("tiertagger.alerts.ticketSound", config.isTicketSound(), config::setTicketSound)
         };
      }
   }

   public class MainSettingsTab extends ButtonTab<TierTaggerConfig> {
      public MainSettingsTab() {
         super("tiertagger.config", TTConfigScreen.this.manager);
      }

      protected WidgetCreator[] getWidgets(TierTaggerConfig var1) {
         ArrayList<WidgetCreator> var2 = new ArrayList();
         var2.add(CyclingOption.ofBoolean("tiertagger.config.enabled", var1.isEnabled(), var1x -> {
            var1.setEnabled(var1x);
            TierTagger.invalidateTierComponentCache();
         }));
         var2.add(
            new CyclingOption<com.skalpha.tiertagger.model.GameMode>(
               "tiertagger.config.gamemode",
               TierCache.getGamemodesOrDefault(),
               var1.getGameMode(),
               var1x -> {
                  var1.setGameMode(var1x.id());
                  TierTagger.invalidateTierComponentCache();
               },
               var0 -> Text.literal(var0.title()).styled(var1x -> var1x.withColor(var0.color())),
               var0 -> var0.isNone() ? Tooltip.of(Text.translatable("tiertagger.config.gamemode.none")) : null,
               !var1.getGameMode().isNone()
            )
         );
         var2.add(CyclingOption.ofBoolean("tiertagger.config.retired", var1.isShowRetired(), var1x -> {
            var1.setShowRetired(var1x);
            TierTagger.invalidateTierComponentCache();
         }));
         var2.add(CyclingOption.ofTranslatableEnum("tiertagger.config.highest", TierTaggerConfig.HighestMode.class, var1.getHighestMode(), var1x -> {
            var1.setHighestMode(var1x);
            TierTagger.invalidateTierComponentCache();
         }, SimpleOption.constantTooltip(Text.translatable("tiertagger.config.highest.desc"))));
         var2.add(CyclingOption.ofBoolean("tiertagger.config.playerList", var1.isPlayerList(), var1::setPlayerList));
         var2.add(CyclingOption.ofBoolean("tiertagger.config.kitIdentifier", var1.isKitIdentifier(), enabled -> {
            var1.setKitIdentifier(enabled);
            com.skalpha.tiertagger.kit.KitIdentifier.clear();
            TierTagger.invalidateTierComponentCache();
         }));
         var2.add(CyclingOption.ofBoolean("tiertagger.config.rankNumbers", var1.isRankNumbers(), value -> { var1.setRankNumbers(value); TierTagger.invalidateTierComponentCache(); }));
         var2.add(CyclingOption.ofBoolean("tiertagger.config.podiumColors", var1.isPodiumColors(), value -> { var1.setPodiumColors(value); TierTagger.invalidateTierComponentCache(); }));
         var2.add(new ScreenOpenButton("tiertagger.config.search", PlayerSearchScreen::new));
         return var2.toArray(WidgetCreator[]::new);
      }
   }
}
