package com.skalpha.tiertagger.tierlist;

import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.model.GameMode;
import com.skalpha.tiertagger.model.PlayerInfo;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.uku3lig.ukulib.config.screen.CloseableScreen;

public class PlayerInfoScreen extends CloseableScreen {
    private final PlayerInfo info;
    private final String mode;
    private final int page;
    private PortraitTexture portrait;
    private List<PlayerInfo.NamedRanking> tiers=List.of();
    private int contentTop,visible,portraitSize,profileLeft;
    public PlayerInfoScreen(Screen parent,PlayerInfo info){this(parent,info,"overall",0);}
    public PlayerInfoScreen(Screen parent,PlayerInfo info,PlayerSkinWidget unused){this(parent,info);}
    private PlayerInfoScreen(Screen parent,PlayerInfo info,String mode,int page){super("Player Profile",parent);this.info=info;this.mode=mode;this.page=Math.max(0,page);}
    protected void init(){
        if(portrait==null)portrait=new PortraitTexture(info.portrait());
        for(ButtonWidget button:PlayerSearchScreen.tabs(width,mode,id->client.setScreen(new PlayerInfoScreen(parent,info,id,0))))addDrawableChild(button);
        contentTop=PlayerSearchScreen.tabsEnd(width)+12;
        portraitSize=Math.min(96,Math.max(32,height-contentTop-40));
        profileLeft=(width-Math.min(310,width-32))/2;
        tiers=info.getSortedTiers().stream().filter(t->mode.equals("overall")||t.mode().id().equals(mode)).toList();visible=Math.max(1,(height-contentTop-80)/12);
        addDrawableChild(ButtonWidget.builder(Text.literal("Back to rankings"),b->close()).dimensions(width/2-75,height-28,150,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("<"),b->client.setScreen(new PlayerInfoScreen(parent,info,mode,page-1))).dimensions(16,height-28,35,20).build()).active=page>0;
        addDrawableChild(ButtonWidget.builder(Text.literal(">"),b->client.setScreen(new PlayerInfoScreen(parent,info,mode,page+1))).dimensions(55,height-28,35,20).build()).active=(page+1)*visible<tiers.size();
    }
    private Text formatTier(GameMode mode,PlayerInfo.Ranking ranking){return Text.empty().append(mode.asStyled(true)).append(Text.literal(": ").formatted(Formatting.GRAY)).append(TierTagger.getRankingText(ranking,false));}
    public void render(DrawContext graphics,int mouseX,int mouseY,float delta){
        super.render(graphics,mouseX,mouseY,delta);graphics.drawCenteredTextWithShadow(textRenderer,info.name()+"'s profile",width/2,8,0xFFFFFFFF);
        portrait.draw(graphics,profileLeft,contentTop,portraitSize,portraitSize);
        int x=profileLeft+112;
        graphics.drawTextWithShadow(textRenderer,info.region()+" - "+info.points()+" points",x,contentTop,0xFFFFFFFF);
        Text placement=Text.empty();if(info.overall()>=1&&info.overall()<=3)placement=placement.copy().append(Text.literal(""+(char)(0xe900+info.overall())+" ").formatted(net.minecraft.util.Formatting.WHITE));
        placement=placement.copy().append(Text.literal(info.overall()>0?"Overall #"+info.overall():"Unranked").styled(style->style.withColor(Leaderboard.positionColor(info.overall()))));
        graphics.drawTextWithShadow(textRenderer,placement,x,contentTop+14,0xFFFFFFFF);
        int start=page*visible,end=Math.min(tiers.size(),start+visible);
        for(int i=start;i<end;i++){var tier=tiers.get(i);graphics.drawTextWithShadow(textRenderer,formatTier(tier.mode(),tier.ranking()),x,contentTop+34+(i-start)*12,0xFFFFFFFF);}
        if(tiers.isEmpty())graphics.drawTextWithShadow(textRenderer,"No tier in this mode",x,contentTop+34,0xFFAAAAAA);
    }
    public void removed(){if(portrait!=null){portrait.close();portrait=null;}super.removed();}
}
