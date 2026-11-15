package com.skalpha.tiertagger.config;
import com.skalpha.tiertagger.TierTagger;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import net.minecraft.client.gui.screens.Screen;
public abstract class GlassScreen extends WLScreen {
    protected GlassScreen(String title,Screen parent){super(parent,title);}
    @Override protected void extractBackground(WLGraphics2D graphics,int mx,int my,float delta){
        if(net.minecraft.client.Minecraft.getInstance().level==null)extractPanorama(graphics,delta);
        if(glass())extractScreenBlur(graphics,delta);
    }
    @Override protected void extractBlurredBackground(WLGraphics2D graphics){if(glass())WLRenderSystem.glassUniforms().requestGlassPass(graphics);}
    @Override protected void extractTransparentBackground(WLGraphics2D graphics){}
    @Override protected void extractMenuBackground(WLGraphics2D graphics,int x,int y,int w,int h){}
    protected boolean glass(){return TierTagger.getManager().getConfig().isLiquidGlass();}
    protected int labelColor(){return TierTagger.getManager().getConfig().isDarkTheme()?0xFFFFFFFF:0xFF20252C;}
    @Override public void extract(WLGraphics2D graphics,int mx,int my,float delta){
        var config=TierTagger.getManager().getConfig();
        var previous=WalksyLibPanelScreen.THEME;WalksyLibPanelScreen.THEME=config.isDarkTheme()?main.walksy.lib.core.gui.style.Theme.DARK:main.walksy.lib.core.gui.style.Theme.LIGHT;
        var style=WalksyLibPanelScreen.panelStyle().copy().tint(config.getMenuColor(),0.12f).opacity((float)config.getMenuOpacity());
        if(!glass())style.flat();
        graphics.glass().dimensions(8,4,width-16,height-8).cornerRadius(14).style(style).extract();
        WalksyLibPanelScreen.THEME=previous;
    }
}
