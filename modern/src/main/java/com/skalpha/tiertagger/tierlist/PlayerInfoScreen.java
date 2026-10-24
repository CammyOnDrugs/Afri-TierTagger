package com.skalpha.tiertagger.tierlist;

import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.model.GameMode;
import com.skalpha.tiertagger.model.PlayerInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import com.skalpha.tiertagger.config.CloseableScreen;
import org.jetbrains.annotations.NotNull;

public class PlayerInfoScreen extends CloseableScreen {
   private final PlayerInfo info;
   private final PlayerSkinWidget skin;

   public PlayerInfoScreen(Screen parent, PlayerInfo info, PlayerSkinWidget skin) {
      super(Component.literal("Player Info"), parent);
      this.info = info;
      this.skin = skin;
   }

   protected void init() {
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, b -> Minecraft.getInstance().setScreen(this.parent))
            .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
            .build()
      );
      this.addRenderableWidget(this.skin);
      int lineHeight = 9;
      int rankingHeight = this.info.rankings().size() * lineHeight;
      int infoHeight = 46;
      int startY = (this.height - infoHeight - rankingHeight) / 2;
      int rankingY = startY + infoHeight;

      for (PlayerInfo.NamedRanking named : this.info.getSortedTiers()) {
         if (named.mode() != null) {
            StringWidget text = new StringWidget(this.formatTier(named.mode(), named.ranking()), this.font);
            text.setX(this.width / 2 + 5);
            text.setY(rankingY);
            Component tooltip = Component.literal(named.ranking().retired() ? "Status: Retired" : "").withStyle(ChatFormatting.GRAY);
            if (!tooltip.getString().isEmpty()) {
               text.setTooltip(Tooltip.create(tooltip));
            }

            this.addRenderableWidget(text);
            rankingY += lineHeight;
         }
      }
   }

   public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      super.render(graphics, mouseX, mouseY, delta);
      graphics.drawCenteredString(this.font, this.info.name() + "'s profile", this.width / 2, 20, -1);
      int lineHeight = 9;
      int rankingHeight = this.info.rankings().size() * lineHeight;
      int infoHeight = 46;
      int startY = (this.height - infoHeight - rankingHeight) / 2;
      graphics.drawString(this.font, this.getRegionText(this.info), this.width / 2 + 5, startY, -1);
      graphics.drawString(this.font, this.getPointsText(this.info), this.width / 2 + 5, startY + 15, -1);
      graphics.drawString(this.font, "Rankings:", this.width / 2 + 5, startY + 30, -1);
   }

   private Component formatTier(@NotNull GameMode mode, PlayerInfo.Ranking ranking) {
      Component tierText = TierTagger.getRankingText(ranking, false);
      return Component.empty().append(mode.asStyled(true)).append(Component.literal(": ").withStyle(ChatFormatting.GRAY)).append(tierText);
   }

   private Component getRegionText(PlayerInfo info) {
      return Component.empty().append(Component.literal("Region: ")).append(Component.literal(info.region()).withStyle(s -> s.withColor(info.getRegionColor())));
   }

   private Component getPointsText(PlayerInfo info) {
      PlayerInfo.PointInfo pointInfo = info.getPointInfo();
      return Component.empty()
         .append(Component.literal("Points: "))
         .append(Component.literal(info.points() + " ").withStyle(s -> s.withColor(pointInfo.getColor())))
         .append(Component.literal("(" + pointInfo.getTitle() + ")").withStyle(s -> s.withColor(pointInfo.getAccentColor())));
   }
}
