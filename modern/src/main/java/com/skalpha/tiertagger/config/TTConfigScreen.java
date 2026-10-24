package com.skalpha.tiertagger.config;
import com.skalpha.tiertagger.TierCache;
import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.tierlist.PlayerSearchScreen;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class TTConfigScreen extends CloseableScreen {
    private int page;
    private int colorPage;
    private final List<EditBox> colors = new ArrayList<>();
    public TTConfigScreen(Screen parent) { super("Afri-Tiers Settings", parent); }
    private TierTaggerConfig config() { return TierTagger.getManager().getConfig(); }
    private void save() { TierTagger.getManager().saveConfig(); TierTagger.invalidateTierComponentCache(); }
    public void refresh(int tab) { page = tab; rebuildWidgets(); }
    private void button(String text, int x, int y, int width, Runnable action) {
        if (page==0 && y>=58 && width==300) { int slot=(y-58)/22; x=this.width/2-153+(slot%2)*156; y=58+(slot/2)*26; width=150; }
        addRenderableWidget(Button.builder(Component.literal(text), b -> { action.run(); save(); rebuildWidgets(); }).bounds(x,y,width,20).build());
    }
    private void toggle(String name, int y, BooleanSupplier getter, Consumer<Boolean> setter) {
        button(name + ": " + (getter.getAsBoolean() ? "ON" : "OFF"), width/2-150,y,300,() -> setter.accept(!getter.getAsBoolean()));
    }
    @Override protected void init() {
        colors.clear();
        String[] tabs = {"General", "Colors", "Alerts"};
        for (int i=0;i<tabs.length;i++) { int tab=i; button(tabs[i],width/2-153+i*103,28,100,() -> page=tab); }
        var c=config(); int y=58;
        if(page==0) {
            toggle("Enabled",y,c::isEnabled,c::setEnabled); y+=22;
            button("Gamemode: "+c.getGameMode().id(),width/2-150,y,300,() -> c.setGameMode(TierCache.findNextMode(c.getGameMode()).id())); y+=22;
            toggle("Show retired",y,c::isShowRetired,c::setShowRetired);y+=22;
            button("Highest tier: "+c.getHighestMode().getName(),width/2-150,y,300,() -> c.setHighestMode(TierTaggerConfig.HighestMode.values()[(c.getHighestMode().ordinal()+1)%3]));y+=22;
            toggle("Gamemode icons",y,c::isShowIcons,c::setShowIcons);y+=22;
            toggle("Tab list",y,c::isPlayerList,c::setPlayerList);y+=22;
            toggle("Kit identifier",y,c::isKitIdentifier,v -> { c.setKitIdentifier(v); com.skalpha.tiertagger.kit.KitIdentifier.clear(); });y+=22;
            toggle("Rank numbers",y,c::isRankNumbers,c::setRankNumbers);y+=22;
            toggle("Rank trophies",y,c::isRankTrophies,c::setRankTrophies);y+=22;
            toggle("Podium colors",y,c::isPodiumColors,c::setPodiumColors);y+=22;
            button("Search player",width/2-150,y,300,() -> Minecraft.getInstance().setScreen(new PlayerSearchScreen(this)));
        } else if(page==2) {
            toggle("Waitlist alerts",y,c::isWaitlistAlerts,c::setWaitlistAlerts);y+=24;
            button("Waitlist location: "+c.getWaitlistLocation().getName(),width/2-150,y,300,() -> c.setWaitlistLocation(c.getWaitlistLocation()==TierTaggerConfig.AlertLocation.CHAT?TierTaggerConfig.AlertLocation.TITLE:TierTaggerConfig.AlertLocation.CHAT));y+=24;
            toggle("Waitlist sound",y,c::isWaitlistSound,c::setWaitlistSound);y+=24;
            toggle("Ticket alerts",y,c::isTicketAlerts,c::setTicketAlerts);y+=24;
            button("Ticket location: "+c.getTicketLocation().getName(),width/2-150,y,300,() -> c.setTicketLocation(c.getTicketLocation()==TierTaggerConfig.AlertLocation.CHAT?TierTaggerConfig.AlertLocation.TITLE:TierTaggerConfig.AlertLocation.CHAT));y+=24;
            toggle("Ticket sound",y,c::isTicketSound,c::setTicketSound);
        } else {
            List<String> keys=List.of("retired","HT1","LT1","HT2","LT2","HT3","LT3","HT4","LT4","HT5","LT5");
            for(int i=colorPage*4;i<Math.min(keys.size(),colorPage*4+4);i++) {
                String key=keys.get(i); String initial=key.equals("retired")?c.getRetiredColorHex():c.getTierColorsHex().get(key);
                EditBox edit=new EditBox(Minecraft.getInstance().font,width/2-20,y,125,20,Component.literal(key));
                edit.setMaxLength(8); edit.setValue(initial);
                edit.setResponder(value -> { if(TierTaggerConfig.isValidColor(value)) { if(key.equals("retired"))c.setRetiredColorHex(value);else c.setTierColorHex(key,value);save(); } });
                colors.add(addRenderableWidget(edit));y+=28;
            }
            button("Previous",width/2-150,y+6,145,() -> colorPage=(colorPage+2)%3);
            button("Next",width/2+5,y+6,145,() -> colorPage=(colorPage+1)%3);
        }
        addRenderableWidget(Button.builder(Component.literal("Done"),b -> onClose()).bounds(width/2-100,height-26,200,20).build());
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float delta) {
        super.render(graphics,x,y,delta);
        graphics.drawCenteredString(Minecraft.getInstance().font,title,width/2,10,0xFFFFFFFF);
        if(page==1) {
            String[] keys={"Retired","HT1","LT1","HT2","LT2","HT3","LT3","HT4","LT4","HT5","LT5"};
            for(int i=0;i<colors.size();i++) {
                EditBox edit=colors.get(i);
                graphics.drawString(Minecraft.getInstance().font,keys[colorPage*4+i],width/2-145,edit.getY()+6,0xFFFFFFFF);
                graphics.fill(width/2+116,edit.getY()+3,width/2+132,edit.getY()+19,0xFFFFFFFF);
                int rgb=TierTaggerConfig.isValidColor(edit.getValue())?TierTaggerConfig.parseColor(edit.getValue()):0x444444;
                graphics.fill(width/2+117,edit.getY()+4,width/2+131,edit.getY()+18,0xFF000000|rgb);
            }
        }
    }
    @Override public void onClose() { save();super.onClose(); }
}
