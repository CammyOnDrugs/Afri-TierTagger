package com.skalpha.tiertagger.tierlist;

import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.model.GameMode;
import com.skalpha.tiertagger.model.PlayerInfo;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.skalpha.tiertagger.config.CloseableScreen;

public class PlayerInfoScreen extends CloseableScreen {
    private final PlayerInfo info;
    private final String mode;
    private final int page;
    private PortraitTexture portrait;
    private List<PlayerInfo.NamedRanking> tiers=List.of();
    private int contentTop,visible;
    public PlayerInfoScreen(Screen parent,PlayerInfo info){this(parent,info,"overall",0);}
    public PlayerInfoScreen(Screen parent,PlayerInfo info,PlayerSkinWidget unused){this(parent,info);}
    private PlayerInfoScreen(Screen parent,PlayerInfo info,String mode,int page){super("Player Profile",parent);this.info=info;this.mode=mode;this.page=Math.max(0,page);}
    protected void init(){
        if(portrait==null)portrait=new PortraitTexture(info.portrait());
        for(Button button:PlayerSearchScreen.tabs(width,mode,id->net.minecraft.client.Minecraft.getInstance().setScreen(new PlayerInfoScreen(parent,info,id,0))))addRenderableWidget(button);
        contentTop=PlayerSearchScreen.tabsEnd(width)+12;
        tiers=info.getSortedTiers().stream().filter(t->mode.equals("overall")||t.mode().id().equals(mode)).toList();visible=Math.max(1,(height-contentTop-80)/12);
        addRenderableWidget(Button.builder(Component.literal("Back to rankings"),b->onClose()).bounds(width/2-75,height-28,150,20).build());
        addRenderableWidget(Button.builder(Component.literal("<"),b->net.minecraft.client.Minecraft.getInstance().setScreen(new PlayerInfoScreen(parent,info,mode,page-1))).bounds(16,height-28,35,20).build()).active=page>0;
        addRenderableWidget(Button.builder(Component.literal(">"),b->net.minecraft.client.Minecraft.getInstance().setScreen(new PlayerInfoScreen(parent,info,mode,page+1))).bounds(55,height-28,35,20).build()).active=(page+1)*visible<tiers.size();
    }
    private Component formatTier(GameMode mode,PlayerInfo.Ranking ranking){return Component.empty().append(mode.asStyled(true)).append(Component.literal(": ").withStyle(ChatFormatting.GRAY)).append(TierTagger.getRankingText(ranking,false));}
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta){
        super.render(graphics,mouseX,mouseY,delta);graphics.drawCenteredString(font,info.name()+"'s profile",width/2,8,0xFFFFFFFF);
        portrait.draw(graphics,16,contentTop,Math.max(64,width/2-30),Math.max(32,height-contentTop-40));
        int x=width/2+4;
        graphics.drawString(font,info.region()+" - "+info.points()+" points",x,contentTop,0xFFFFFFFF);
        Component placement=Component.empty();if(info.overall()>=1&&info.overall()<=3)placement=placement.copy().append(Component.literal(""+(char)(0xe900+info.overall())+" ").withStyle(net.minecraft.ChatFormatting.WHITE));
        placement=placement.copy().append(Component.literal(info.overall()>0?"Overall #"+info.overall():"Unranked").withStyle(style->style.withColor(Leaderboard.positionColor(info.overall()))));
        graphics.drawString(font,placement,x,contentTop+14,0xFFFFFFFF);
        int start=page*visible,end=Math.min(tiers.size(),start+visible);
        for(int i=start;i<end;i++){var tier=tiers.get(i);graphics.drawString(font,formatTier(tier.mode(),tier.ranking()),x,contentTop+34+(i-start)*12,0xFFFFFFFF);}
        if(tiers.isEmpty())graphics.drawString(font,"No tier in this mode",x,contentTop+34,0xFFAAAAAA);
    }
    public void removed(){if(portrait!=null){portrait.close();portrait=null;}super.removed();}
}
