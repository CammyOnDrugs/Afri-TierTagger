package com.skalpha.tiertagger.queue;
import com.skalpha.tiertagger.model.GameMode;
import com.skalpha.tiertagger.tierlist.Leaderboard;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.skalpha.tiertagger.config.GlassScreen;
public final class QueueScreen extends GlassScreen {
    private final List<ButtonWidget> joins=new ArrayList<>();
    private final List<String> modes=Leaderboard.MODES.stream().filter(id->!id.equals("overall")).toList();
    private int top,left,column,rowHeight;
    public QueueScreen(Screen parent){super("Open Queues",parent);}
    @Override protected void init(){
        joins.clear();column=Math.min(235,(width-24)/2);left=(width-2*column)/2;top=64;
        rowHeight=Math.max(12,Math.min(24,(height-top-32)/9));
        for(int i=0;i<modes.size();i++){
            String mode=modes.get(i);int x=left+(i/9)*column,y=top+(i%9)*rowHeight;
            joins.add(addDrawableChild(ButtonWidget.builder(Text.literal("Join"),b->QueueAlerts.join(mode)).dimensions(x+column-48,y,44,Math.min(20,rowHeight-1)).build()));
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Follow Discord Instructions ↗"),b->net.minecraft.util.Util.getOperatingSystem().open(java.net.URI.create("https://discord.gg/afritiers"))).dimensions(width/2-120,40,240,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"),b->close()).dimensions(width/2-60,height-26,120,20).build());
        tick();
    }
    @Override public void tick(){for(int i=0;i<joins.size();i++)joins.get(i).active=QueueAlerts.isOpen(modes.get(i))&&QueueAlerts.supported()&&!Boolean.FALSE.equals(QueueAlerts.linked());}
    @Override public void extract(main.walksy.lib.core.render.WLGraphics2D layer,int mx,int my,float delta){
        super.extract(layer,mx,my,delta);var graphics=layer.context().extractor();
        graphics.drawCenteredTextWithShadow(textRenderer,"Queues: "+(QueueAlerts.feedOnline()?"Online":"Offline"),width/2,10,QueueAlerts.feedOnline()?0xFF65D48A:0xFFFF7474);
        Boolean linked=QueueAlerts.linked();
        String status=Boolean.FALSE.equals(linked)?"Not Linked! Follow Discord Instructions!":Boolean.TRUE.equals(linked)?"Linked — choose an open queue":QueueAlerts.supported()?"Checking your linked account…":"Join a server with AfriTiersLink to enter queues";
        graphics.drawCenteredTextWithShadow(textRenderer,status,width/2,25,0xFFF0B768);
        for(int i=0;i<modes.size();i++){
            String mode=modes.get(i);int x=left+(i/9)*column,y=top+(i%9)*rowHeight+5;
            graphics.drawTextWithShadow(textRenderer,GameMode.byId(mode).asStyled(true),x,y,labelColor());
            graphics.drawTextWithShadow(textRenderer,QueueAlerts.isOpen(mode)?"Open":"Closed",x+column-92,y,QueueAlerts.isOpen(mode)?0xFF65D48A:0xFFFF7474);
        }
    }
}
