package com.skalpha.tiertagger.tierlist;

import com.skalpha.tiertagger.TierCache;
import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.model.GameMode;
import com.skalpha.tiertagger.model.PlayerInfo;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.uku3lig.ukulib.config.option.widget.TextInputWidget;
import net.uku3lig.ukulib.config.screen.CloseableScreen;

public class PlayerSearchScreen extends CloseableScreen {
    private final String mode,query;
    private final int page;
    private TextInputWidget textField;
    private List<PlayerInfo> rows=List.of();
    private String status="";
    private int rowTop,pageSize;
    private boolean loading;
    private record RowPortrait(PortraitTexture texture,int y){}
    private final java.util.List<RowPortrait> portraits=new java.util.ArrayList<>();
    public PlayerSearchScreen(Screen parent){this(parent,"overall","",0);}
    public PlayerSearchScreen(Screen parent,String mode,String query,int page){super("Afri-Tiers Rankings",parent);this.mode=mode;this.query=query;this.page=Math.max(0,page);}
    protected void init(){
        portraits.forEach(row->row.texture().close());portraits.clear();
        for(ButtonWidget button:tabs(width,mode,selected->show(selected,textField==null?query:textField.getText(),0)))addDrawableChild(button);
        int tabsEnd=tabsEnd(width);
        int inputY=tabsEnd+8;
        textField=addSelectableChild(new TextInputWidget(16,inputY,Math.max(80,width-112),20,query,value->{},"Minecraft username",value->value.matches("[a-zA-Z0-9_]*"),16));
        addDrawableChild(ButtonWidget.builder(Text.literal("Search"),b->show(mode,textField.getText(),0)).dimensions(width-88,inputY,72,20).build());
        rows=Leaderboard.rows(TierCache.getPlayerCacheValues(),mode,query);rowTop=inputY+42;pageSize=Math.max(1,(height-rowTop-38)/36);
        int start=Math.min(page*pageSize,Math.max(0,rows.size()-1)),end=Math.min(rows.size(),start+pageSize);
        for(int i=start;i<end;i++){
            PlayerInfo profile=rows.get(i);Text label=Text.empty();
            if(mode.equals("overall")&&profile.overall()>=1&&profile.overall()<=3)label=label.copy().append(Text.literal(""+(char)(0xe900+profile.overall())+" ").formatted(net.minecraft.util.Formatting.WHITE));
            if(mode.equals("overall")&&profile.overall()>0)label=label.copy().append(Text.literal("#"+profile.overall()+" ").styled(style->style.withColor(Leaderboard.positionColor(profile.overall()))));
            label=label.copy().append(Text.literal(profile.name()+"  "+(mode.equals("overall")?profile.points()+" points":profile.rankings().get(mode).tierName()+(profile.rankings().get(mode).retired()?" (Retired)":""))));
            addDrawableChild(ButtonWidget.builder(label,b->MinecraftClient.getInstance().setScreen(new PlayerInfoScreen(this,profile))).dimensions(16,rowTop+(i-start)*36,width-32,32).build());
            portraits.add(new RowPortrait(new PortraitTexture(profile.portrait()),rowTop+(i-start)*36+2));
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("<"),b->show(mode,query,page-1)).dimensions(16,height-28,35,20).build()).active=page>0;
        addDrawableChild(ButtonWidget.builder(Text.literal(">"),b->show(mode,query,page+1)).dimensions(55,height-28,35,20).build()).active=(page+1)*pageSize<rows.size();
        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"),b->refresh()).dimensions(width/2-36,height-28,72,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"),b->close()).dimensions(width-88,height-28,72,20).build());
        setInitialFocus(textField);
        if(TierCache.getPlayerCacheValues().isEmpty()&&!loading)refresh();
    }
    private void show(String selected,String filter,int index){MinecraftClient.getInstance().setScreen(new PlayerSearchScreen(parent,selected,filter,index));}
    private void refresh(){
        if(loading)return;loading=true;status="Refreshing...";
        TierCache.reload().whenComplete((list,error)->MinecraftClient.getInstance().execute(()->{loading=false;if(MinecraftClient.getInstance().currentScreen!=this)return;if(error!=null||list==null||list.isEmpty()){status="Could not refresh; showing cached rankings.";return;}show(mode,textField.getText(),0);}));
    }
    static int tabsEnd(int width){int columns=Math.max(4,Math.min(9,(width-24)/44));return 28+((Leaderboard.MODES.size()+columns-1)/columns)*22;}
    static List<ButtonWidget> tabs(int width,String selected,java.util.function.Consumer<String> choose){
        java.util.ArrayList<ButtonWidget> result=new java.util.ArrayList<>();int columns=Math.max(4,Math.min(9,(width-24)/44)),size=Math.min(60,(width-24)/columns);
        for(int i=0;i<Leaderboard.MODES.size();i++){
            String id=Leaderboard.MODES.get(i);GameMode mode=GameMode.byId(id);
            Text label=id.equals("overall")?Text.literal("Overall"):Text.literal(mode.icon().map(Object::toString).orElse(id));
            ButtonWidget button=ButtonWidget.builder(label,b->choose.accept(id)).dimensions((width-columns*size)/2+(i%columns)*size,26+(i/columns)*22,size-2,20).build();
            button.setTooltip(Tooltip.of(mode.asStyled(false)));button.active=!id.equals(selected);result.add(button);
        }
        return result;
    }
    public void render(DrawContext graphics,int mouseX,int mouseY,float delta){
        super.render(graphics,mouseX,mouseY,delta);graphics.drawCenteredTextWithShadow(textRenderer,title,width/2,8,0xFFFFFFFF);textField.render(graphics,mouseX,mouseY,delta);
        graphics.drawTextWithShadow(textRenderer,GameMode.byId(mode).asStyled(true).copy().append(Text.literal(" - "+rows.size()+" players")),16,rowTop-14,0xFFFFFFFF);
        for(RowPortrait row:portraits)row.texture().thumbnail(graphics,20,row.y(),28,28);
        if(rows.isEmpty())graphics.drawCenteredTextWithShadow(textRenderer,query.isBlank()?"Rankings are loading...":"No matching players in this mode.",width/2,rowTop+8,0xFFAAAAAA);
        if(!status.isEmpty())graphics.drawCenteredTextWithShadow(textRenderer,status,width/2,height-42,0xFFAAAAAA);
    }
    public void removed(){portraits.forEach(row->row.texture().close());portraits.clear();super.removed();}
}
