package com.skalpha.tiertagger.config;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
public final class LegacyColorScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> setter;
    private int color;
    private TextFieldWidget hex;
    private boolean syncing;
    private final java.util.List<Channel> channels=new java.util.ArrayList<>();
    public LegacyColorScreen(Screen parent,String title,String value,Consumer<String> setter){super(Text.literal(title+" colour"));this.parent=parent;this.color=TierTaggerConfig.parseColor(value);this.setter=setter;}
    @Override protected void init(){
        channels.clear();int x=width/2-120,y=height/2-62;
        hex=new TextFieldWidget(MinecraftClient.getInstance().textRenderer,x,y,240,20,Text.literal("Hex colour"));hex.setMaxLength(7);hex.setText(String.format("#%06X",color));
        hex.setChangedListener(value->{if(!syncing&&value.matches("#[0-9a-fA-F]{6}")){color=TierTaggerConfig.parseColor(value);setter.accept(value);for(Channel channel:channels)channel.sync();}});addDrawableChild(hex);
        for(int i=0;i<3;i++){Channel channel=new Channel(x,y+28+i*25,16-8*i,new String[]{"R","G","B"}[i]);channels.add(addDrawableChild(channel));}
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"),b->close()).dimensions(width/2-60,y+112,120,20).build());
    }
    private final class Channel extends SliderWidget {
        final int shift;final String label;
        Channel(int x,int y,int shift,String label){super(x,y,240,20,Text.empty(),((color>>shift)&255)/255.0);this.shift=shift;this.label=label;updateMessage();}
        protected void updateMessage(){setMessage(Text.literal(label+": "+Math.round(value*255)));}
        protected void applyValue(){color=(color&~(255<<shift))|((int)Math.round(value*255)<<shift);String text=String.format("#%06X",color);setter.accept(text);syncing=true;hex.setText(text);syncing=false;}
        void sync(){value=((color>>shift)&255)/255.0;updateMessage();}
    }
    @Override public void close(){MinecraftClient.getInstance().setScreen(parent);}
    @Override public void render(DrawContext graphics,int mx,int my,float delta){super.render(graphics,mx,my,delta);graphics.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,title,width/2,height/2-90,0xFFFFFFFF);graphics.fill(width/2-120,height/2+79,width/2+120,height/2+89,0xFF000000|color);}
}
