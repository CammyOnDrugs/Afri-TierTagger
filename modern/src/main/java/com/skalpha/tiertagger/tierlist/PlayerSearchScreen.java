package com.skalpha.tiertagger.tierlist;

import com.skalpha.tiertagger.TierCache;
import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.model.GameMode;
import com.skalpha.tiertagger.model.PlayerInfo;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import com.skalpha.tiertagger.config.TextInputWidget;
import com.skalpha.tiertagger.config.CloseableScreen;

public class PlayerSearchScreen extends CloseableScreen {
    private final String mode,query;
    private final int page;
    private TextInputWidget textField;
    private List<PlayerInfo> rows=List.of();
    private String status="";
    private int rowTop,pageSize;
    private boolean loading;
    public PlayerSearchScreen(Screen parent){this(parent,"overall","",0);}
    public PlayerSearchScreen(Screen parent,String mode,String query,int page){super("Afri-Tiers Rankings",parent);this.mode=mode;this.query=query;this.page=Math.max(0,page);}
    protected void init(){
        for(Button button:tabs(width,mode,selected->show(selected,textField==null?query:textField.getText(),0)))addRenderableWidget(button);
        int tabsEnd=tabsEnd(width);
        int inputY=tabsEnd+8;
        textField=addWidget(new TextInputWidget(16,inputY,Math.max(80,width-112),20,query,value->{},"Minecraft username",value->value.matches("[a-zA-Z0-9_]*"),16));
        addRenderableWidget(Button.builder(Component.literal("Search"),b->show(mode,textField.getText(),0)).bounds(width-88,inputY,72,20).build());
        rows=Leaderboard.rows(TierCache.getPlayerCacheValues(),mode,query);rowTop=inputY+42;pageSize=Math.max(1,(height-rowTop-38)/23);
        int start=Math.min(page*pageSize,Math.max(0,rows.size()-1)),end=Math.min(rows.size(),start+pageSize);
        for(int i=start;i<end;i++){
            PlayerInfo profile=rows.get(i);Component label=Component.empty();
            if(mode.equals("overall")&&profile.overall()>0)label=label.copy().append(Component.literal("#"+profile.overall()+" ").withStyle(style->style.withColor(Leaderboard.positionColor(profile.overall()))));
            label=label.copy().append(Component.literal(profile.name()+"  "+(mode.equals("overall")?profile.points()+" points":profile.rankings().get(mode).tierName()+(profile.rankings().get(mode).retired()?" (Retired)":""))));
            addRenderableWidget(Button.builder(label,b->Minecraft.getInstance().setScreen(new PlayerInfoScreen(this,profile))).bounds(16,rowTop+(i-start)*23,width-32,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"),b->show(mode,query,page-1)).bounds(16,height-28,35,20).build()).active=page>0;
        addRenderableWidget(Button.builder(Component.literal(">"),b->show(mode,query,page+1)).bounds(55,height-28,35,20).build()).active=(page+1)*pageSize<rows.size();
        addRenderableWidget(Button.builder(Component.literal("Refresh"),b->refresh()).bounds(width/2-36,height-28,72,20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width-88,height-28,72,20).build());
        setInitialFocus(textField);
        if(TierCache.getPlayerCacheValues().isEmpty()&&!loading)refresh();
    }
    private void show(String selected,String filter,int index){Minecraft.getInstance().setScreen(new PlayerSearchScreen(parent,selected,filter,index));}
    private void refresh(){
        if(loading)return;loading=true;status="Refreshing...";
        TierCache.reload().whenComplete((list,error)->Minecraft.getInstance().execute(()->{loading=false;if(Minecraft.getInstance().screen!=this)return;if(error!=null||list==null||list.isEmpty()){status="Could not refresh; showing cached rankings.";return;}show(mode,textField.getText(),0);}));
    }
    static int tabsEnd(int width){int columns=Math.max(4,Math.min(9,(width-24)/44));return 28+((Leaderboard.MODES.size()+columns-1)/columns)*22;}
    static List<Button> tabs(int width,String selected,java.util.function.Consumer<String> choose){
        java.util.ArrayList<Button> result=new java.util.ArrayList<>();int columns=Math.max(4,Math.min(9,(width-24)/44)),size=Math.min(60,(width-24)/columns);
        for(int i=0;i<Leaderboard.MODES.size();i++){
            String id=Leaderboard.MODES.get(i);GameMode mode=GameMode.byId(id);
            Component label=id.equals("overall")?Component.literal("Overall"):Component.literal(mode.icon().map(Object::toString).orElse(id));
            Button button=Button.builder(label,b->choose.accept(id)).bounds((width-columns*size)/2+(i%columns)*size,26+(i/columns)*22,size-2,20).build();
            button.setTooltip(Tooltip.create(mode.asStyled(false)));button.active=!id.equals(selected);result.add(button);
        }
        return result;
    }
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta){
        super.render(graphics,mouseX,mouseY,delta);graphics.drawCenteredString(font,title,width/2,8,0xFFFFFFFF);this.textField.render(graphics,mouseX,mouseY,delta);
        graphics.drawString(font,GameMode.byId(mode).asStyled(true).copy().append(Component.literal(" - "+rows.size()+" players")),16,rowTop-14,0xFFFFFFFF);
        if(rows.isEmpty())graphics.drawCenteredString(font,query.isBlank()?"Rankings are loading...":"No matching players in this mode.",width/2,rowTop+8,0xFFAAAAAA);
        if(!status.isEmpty())graphics.drawCenteredString(font,status,width/2,height-42,0xFFAAAAAA);
    }
}
