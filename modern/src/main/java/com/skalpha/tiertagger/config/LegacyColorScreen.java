package com.skalpha.tiertagger.config;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
public final class LegacyColorScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> setter;
    private int color;
    private EditBox hex;
    private boolean syncing;
    private final java.util.List<Channel> channels=new java.util.ArrayList<>();
    public LegacyColorScreen(Screen parent,String title,String value,Consumer<String> setter){super(Component.literal(title+" colour"));this.parent=parent;this.color=TierTaggerConfig.parseColor(value);this.setter=setter;}
    @Override protected void init(){
        channels.clear();int x=width/2-120,y=height/2-62;
        hex=new EditBox(Minecraft.getInstance().font,x,y,240,20,Component.literal("Hex colour"));hex.setMaxLength(7);hex.setValue(String.format("#%06X",color));
        hex.setResponder(value->{if(!syncing&&value.matches("#[0-9a-fA-F]{6}")){color=TierTaggerConfig.parseColor(value);setter.accept(value);for(Channel channel:channels)channel.sync();}});addRenderableWidget(hex);
        for(int i=0;i<3;i++){Channel channel=new Channel(x,y+28+i*25,16-8*i,new String[]{"R","G","B"}[i]);channels.add(addRenderableWidget(channel));}
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2-60,y+112,120,20).build());
    }
    private final class Channel extends AbstractSliderButton {
        final int shift;final String label;
        Channel(int x,int y,int shift,String label){super(x,y,240,20,Component.empty(),((color>>shift)&255)/255.0);this.shift=shift;this.label=label;updateMessage();}
        protected void updateMessage(){setMessage(Component.literal(label+": "+Math.round(value*255)));}
        protected void applyValue(){color=(color&~(255<<shift))|((int)Math.round(value*255)<<shift);String text=String.format("#%06X",color);setter.accept(text);syncing=true;hex.setValue(text);syncing=false;}
        void sync(){value=((color>>shift)&255)/255.0;updateMessage();}
    }
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public void render(GuiGraphics graphics,int mx,int my,float delta){super.render(graphics,mx,my,delta);graphics.drawCenteredString(Minecraft.getInstance().font,title,width/2,height/2-90,0xFFFFFFFF);graphics.fill(width/2-120,height/2+79,width/2+120,height/2+89,0xFF000000|color);}
}
