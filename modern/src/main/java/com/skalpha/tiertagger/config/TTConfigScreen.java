package com.skalpha.tiertagger.config;

import com.skalpha.tiertagger.TierTagger;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.widget.*;
import main.walksy.lib.core.gui.widget.options.*;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.gui.liquid.*;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;

public final class TTConfigScreen extends WLScreen {
    private final Theme previousTheme=Theme.get();
    private final Integer previousAccent=Theme.accentOverride();
    private final Config options=new WalksySettings().getOrCreateConfig();
    private final PopoverHost popover=new PopoverHost();
    private final LiquidTransition transition=new LiquidTransition(LiquidProfile.smooth(),0.5f);
    private long frame=-1;
    private LiquidRect bounds;
    private float scale;
    private double lastSize=-1;
    private boolean lastDark;
    private double lastOpacity=-1;
    private final Icons.Icon logo=new Icons.Icon(Identifier.fromNamespaceAndPath("tier-tagger","icon.png"),1254);
    private boolean popoverPressed,pointerDown;
    private int page,x,y,w,h,rail;
    private String query="";
    private ScrollPanel list;
    private RowContext context;
    public TTConfigScreen(Screen parent){super(parent,"Afri-Tiers Settings");}
    public static Screen create(Screen parent){
        return TierTagger.getManager().getConfig().isLegacyMenu()?new LegacyConfigScreen(parent):new TTConfigScreen(parent);
    }
    static void switchMenu(Screen parent,boolean legacy){
        TierTagger.getManager().getConfig().setLegacyMenu(legacy);
        TierTagger.getManager().saveConfig();
        main.walksy.lib.core.platform.WLClient.setScreen(create(parent));
    }
    private main.walksy.lib.core.render.glass.api.GlassStyle panelStyle(TierTaggerConfig config){
        var style=WalksyLibPanelScreen.panelStyle().copy().tint(config.getMenuColor(),0.12f);
        return config.isLiquidGlass()?style:style.flat();
    }
    private void applyTheme(){
        WalksyLibPanelScreen.THEME=TierTagger.getManager().getConfig().isDarkTheme()?Theme.DARK:Theme.LIGHT;
        Theme.setAccentOverride(0xFF000000|TierTagger.getManager().getConfig().getMenuColor());
    }
    public void refresh(int tab){page=Math.max(0,Math.min(3,tab));if(list!=null)populate();}
    private void setDark(boolean dark){
        TierTagger.getManager().getConfig().setDarkTheme(dark);TierTagger.getManager().saveConfig();
        popover.closeInstant();applyTheme();rebuildWidgets();
    }
    private int px(float value){return Math.max(1,Math.round(value*scale));}
    private Icons.Icon icon(String name){return new Icons.Icon(Identifier.fromNamespaceAndPath("tier-tagger","textures/gui/"+name+".png"),64);}
    @Override protected void init(){
        super.init();applyTheme();
        var config=TierTagger.getManager().getConfig();lastSize=config.getMenuScale();lastDark=config.isDarkTheme();lastOpacity=config.getMenuOpacity();
        scale=WLScreen.uiScaleFactor()*(float)lastSize;
        float fraction=Math.min(0.96f,0.6085f*WLScreen.sizeMultiplier()*(float)lastSize);
        w=Math.round(width*fraction);h=Math.round(height*fraction);x=(width-w)/2;y=(height-h)/2;
        rail=Math.round(Math.max(Math.min(128*scale,w*0.32f),Math.min(w*0.208f,208*scale)));
        int margin=px(8),padding=px(12),rowHeight=px(20),gap=px(3),left=x+margin+padding;
        int rowWidth=rail-2*padding;
        String[] names={"General","Alerts","Tier colors","Settings"};
        String[] icons={"home","bell","palette","settings"};
        for(int i=0;i<names.length;i++){
            int tab=i;
            addRenderableWidget(new SidebarActionRow(left,i==3?y+h-px(32)-rowHeight-gap:y+px(62)+i*(rowHeight+gap),rowWidth,rowHeight,names[i],icon(icons[i]),()->{page=tab;query="";popover.closeInstant();rebuildWidgets();},()->page==tab));
        }
        addRenderableWidget(new SidebarActionRow(left,y+h-px(32),rowWidth,rowHeight,"Done",Icons.CHECK,this::onClose,null));
        if(page==3)addRenderableWidget(new SidebarActionRow(left,y+h-px(32)-2*(rowHeight+gap),rowWidth,rowHeight,"Legacy menu",Icons.APPEARANCE,()->switchMenu(parent,true),null));
        int right=x+rail+2*margin,cw=w-rail-3*margin;
        int searchWidth=Math.min(px(120),cw/3),searchHeight=px(18);
        SearchBar search=new SearchBar(right+cw-searchWidth,y+margin,searchWidth,searchHeight,value->{query=value;populate();});
        search.setValue(query);addRenderableWidget(search);
        String[] destinations={"https://github.com/CammyOnDrugs/Afri-TierTagger","https://modrinth.com/mod/afri-tiertagger","https://afritiers.co.za"};
        String[] socialIcons={"github","modrinth","website"};
        for(int i=0;i<3;i++)addRenderableWidget(new LinkButton(right+px(150+i*23),y+margin,px(20),icon(socialIcons[i]),destinations[i]));
        list=addRenderableWidget(new ScrollPanel(right,y+px(62),cw,h-px(76)));
        context=new RowContext(option->{},(anchor,content)->popover.open(anchor,width,height,content),
            (anchor,content)->popover.openCentered(anchor,width,height,x,y,w,h,content),
            (anchor,px,py,content)->popover.openAt(anchor,px,py,width,height,content),popover::close);
        populate();
        LiquidRect nextBounds=new LiquidRect(x,y,w,h,18*scale);
        if(bounds==null)transition.open(nextBounds);
        else if(!bounds.equals(nextBounds))transition.retarget(nextBounds);
        bounds=nextBounds;
        if(!config.isMenuAnimations())transition.setProgress(1);
    }
    private void populate(){
        if(list==null)return;list.clear();
        if(page==3&&query.isBlank())Updates.start();
        if(query.isBlank())OptionWidgetFactory.populateGroups(list,java.util.List.of(options.categories().getFirst().groups().get(page)),context,null);
        else OptionWidgetFactory.populateSearch(list,options.categories(),query,context);
    }
    private void advance(){
        long current=WLRenderSystem.frameId();if(frame==current)return;frame=current;
        transition.update(WLRenderSystem.frameDeltaTicks());
    }
    @Override protected float contentOpacity(){return Math.max(0,Math.min(1,transition.value()));}
    @Override protected void applyContentTransform(org.joml.Matrix3x2fStack pose){
        LiquidRect rect=transition.currentPrimary();if(rect==null||transition.isSettled()||w<=0||h<=0)return;
        pose.translate(rect.centerX(),rect.centerY()).scale(rect.w()/w,rect.h()/h).translate(-(x+w/2f),-(y+h/2f));
    }
    @Override protected boolean isAnimating(){return !transition.isSettled();}
    @Override protected void extractBlurredBackground(WLGraphics2D graphics){advance();if(TierTagger.getManager().getConfig().isLiquidGlass())WLRenderSystem.glassUniforms().requestGlassPass(graphics);}
    @Override protected void extractBackground(WLGraphics2D graphics,int mouseX,int mouseY,float delta){
        if(minecraft==null||minecraft.level==null)extractPanorama(graphics,delta);
        if(TierTagger.getManager().getConfig().isLiquidGlass())extractScreenBlur(graphics,delta);
    }
    @Override protected void extractTransparentBackground(WLGraphics2D graphics){}
    @Override protected void extractMenuBackground(WLGraphics2D graphics,int x,int y,int width,int height){}
    @Override public void extract(WLGraphics2D graphics,int mouseX,int mouseY,float delta){
        advance();applyTheme();Theme theme=Theme.get();
        var config=TierTagger.getManager().getConfig();
        transition.emit(graphics,panelStyle(config).opacity((float)config.getMenuOpacity()*contentOpacity()),0);
        graphics.pose().pushMatrix();applyContentTransform(graphics.pose());
        graphics.glass().dimensions(x+px(8),y+px(8),rail,h-px(16)).cornerRadius(10*scale).style((config.isLiquidGlass()?WalksyLibPanelScreen.sidebarPanelStyle().copy():WalksyLibPanelScreen.sidebarPanelStyle().copy().flat()).opacity(contentOpacity())).extract();
        float logoSize=20*scale;
        logo.extract(graphics,x+px(18),y+px(23),logoSize,0xFFFFFFFF);
        Fonts.SFPRO_DISPLAY_BOLD.extract(graphics,"Afri-TiersTagger",x+px(43),y+px(22),12*scale,theme.label());
        Fonts.SFPRO_DISPLAY.extract(graphics,"By Brianni_MC",x+px(43),y+px(39),10*scale,theme.secondaryLabel());
        float cx=x+rail+px(16);
        Fonts.SFPRO_DISPLAY_BOLD.extract(graphics,"Afri-TiersTagger",cx,y+px(8),14*scale,theme.label());
        Fonts.SFPRO_DISPLAY.extract(graphics,(page==3?Updates.status():"Player tiers, rankings and testing alerts • By Brianni_MC"),cx,y+px(31),11*scale,theme.secondaryLabel());
        graphics.pose().popMatrix();
        popover.extract(graphics,mouseX,mouseY,delta);
    }
    @Override public void tick(){
        super.tick();
        var config=TierTagger.getManager().getConfig();
        float nextScale=WLScreen.uiScaleFactor()*(float)config.getMenuScale();
        if(!pointerDown&&Math.abs(nextScale-scale)>0.001f){popover.closeInstant();rebuildWidgets();}
    }
    @Override public boolean mouseClicked(MouseButtonEvent click,boolean twice){pointerDown=true;if(popover.isOpen()&&popover.mouseClicked(click,twice)){popoverPressed=true;return true;}
        popoverPressed=false;return super.mouseClicked(click,twice);}
    @Override public boolean mouseDragged(MouseButtonEvent click,double dx,double dy){return popoverPressed?popover.mouseDragged(click,dx,dy):super.mouseDragged(click,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent click){pointerDown=false;boolean handled=popoverPressed?popover.mouseReleased(click):super.mouseReleased(click);popoverPressed=false;return handled;}
    @Override public boolean mouseScrolled(double mx,double my,double sx,double sy){return popover.mouseScrolled(mx,my,sx,sy)||super.mouseScrolled(mx,my,sx,sy);}
    @Override public boolean keyPressed(KeyEvent input){return popover.isOpen()?popover.keyPressed(input):super.keyPressed(input);}
    @Override public boolean charTyped(CharacterEvent input){return popover.charTyped(input)||super.charTyped(input);}
    @Override public void removed(){
        popover.closeInstant();options.saveAndNotify();
        WalksyLibPanelScreen.THEME=previousTheme;Theme.setAccentOverride(previousAccent);super.removed();
    }
}
