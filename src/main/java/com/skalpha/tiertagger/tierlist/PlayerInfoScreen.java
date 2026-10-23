package com.skalpha.tiertagger.tierlist;

import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.model.GameMode;
import com.skalpha.tiertagger.model.PlayerInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.uku3lig.ukulib.config.screen.CloseableScreen;
import org.jetbrains.annotations.NotNull;

public class PlayerInfoScreen extends CloseableScreen {
   private final PlayerInfo info;
   private final PlayerSkinWidget skin;

   public PlayerInfoScreen(Screen parent, PlayerInfo info, PlayerSkinWidget skin) {
      super(Text.literal("Player Info"), parent);
      this.info = info;
      this.skin = skin;
   }

   protected void init() {
      this.addDrawableChild(
         ButtonWidget.builder(ScreenTexts.DONE, b -> MinecraftClient.getInstance().setScreen(this.parent))
            .dimensions(this.width / 2 - 100, this.height - 27, 200, 20)
            .build()
      );
      this.addDrawableChild(this.skin);
      int lineHeight = 9;
      int rankingHeight = this.info.rankings().size() * lineHeight;
      int infoHeight = 46;
      int startY = (this.height - infoHeight - rankingHeight) / 2;
      int rankingY = startY + infoHeight;

      for (PlayerInfo.NamedRanking named : this.info.getSortedTiers()) {
         if (named.mode() != null) {
            TextWidget text = new TextWidget(this.formatTier(named.mode(), named.ranking()), this.textRenderer);
            text.setX(this.width / 2 + 5);
            text.setY(rankingY);
            Text tooltip = Text.literal(named.ranking().retired() ? "Status: Retired" : "").formatted(Formatting.GRAY);
            if (!tooltip.getString().isEmpty()) {
               text.setTooltip(Tooltip.of(tooltip));
            }

            this.addDrawableChild(text);
            rankingY += lineHeight;
         }
      }
   }

   public void render(@NotNull DrawContext graphics, int mouseX, int mouseY, float delta) {
      super.render(graphics, mouseX, mouseY, delta);
      graphics.drawCenteredTextWithShadow(this.textRenderer, this.info.name() + "'s profile", this.width / 2, 20, -1);
      int lineHeight = 9;
      int rankingHeight = this.info.rankings().size() * lineHeight;
      int infoHeight = 46;
      int startY = (this.height - infoHeight - rankingHeight) / 2;
      graphics.drawTextWithShadow(this.textRenderer, this.getRegionText(this.info), this.width / 2 + 5, startY, -1);
      graphics.drawTextWithShadow(this.textRenderer, this.getPointsText(this.info), this.width / 2 + 5, startY + 15, -1);
      graphics.drawTextWithShadow(this.textRenderer, "Rankings:", this.width / 2 + 5, startY + 30, -1);
   }

   private Text formatTier(@NotNull GameMode mode, PlayerInfo.Ranking ranking) {
      Text tierText = TierTagger.getRankingText(ranking, false);
      return Text.empty().append(mode.asStyled(true)).append(Text.literal(": ").formatted(Formatting.GRAY)).append(tierText);
   }

   private Text getRegionText(PlayerInfo info) {
      return Text.empty().append(Text.literal("Region: ")).append(Text.literal(info.region()).styled(s -> s.withColor(info.getRegionColor())));
   }

   private Text getPointsText(PlayerInfo info) {
      PlayerInfo.PointInfo pointInfo = info.getPointInfo();
      return Text.empty()
         .append(Text.literal("Points: "))
         .append(Text.literal(info.points() + " ").styled(s -> s.withColor(pointInfo.getColor())))
         .append(Text.literal("(" + pointInfo.getTitle() + ")").styled(s -> s.withColor(pointInfo.getAccentColor())));
   }
}
