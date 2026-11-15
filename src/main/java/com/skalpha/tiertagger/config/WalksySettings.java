package com.skalpha.tiertagger.config;

import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.kit.KitIdentifier;
import com.skalpha.tiertagger.tierlist.PlayerSearchScreen;
import main.walksy.lib.api.WalksyLibApi;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.*;
import main.walksy.lib.api.config.value.WLColor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import java.util.*;
import java.util.function.*;

public final class WalksySettings implements WalksyLibApi {
    private static final Map<String,WLColor> colors=new HashMap<>();
    private static WLColor menuColor;
    private static TierTaggerConfig config(){return TierTagger.getManager().getConfig();}
    private static void save(){TierTagger.getManager().saveConfig();TierTagger.invalidateTierComponentCache();}
    private static Option<Boolean> toggle(String label,String key,String description,boolean defaultValue,Supplier<Boolean> getter,Consumer<Boolean> setter){
        return Options.ofBoolean(label,getter,defaultValue,value->{setter.accept(value);save();}).saveKey(key).description(description).build();
    }
    public enum DisplayMode {
        Overall("overall"),None("annoying_long_id_that_no_one_will_ever_use_just_to_make_sure"),Sword("sword"),Axe("axe"),Vanilla("vanilla"),UHC("uhc"),DiaPot("diapot"),NethOp("nethpot"),SMP("smp"),Mace("mace"),SpearMace("spear"),DiaSMP("diasmp"),DiaVanilla("diavanilla"),Cart("cart"),Speed("speed"),Creeper("creeper"),Bed("bed"),OGVanilla("ogv"),RocketMace("rocketmace"),HoleFight("holefight"),Bow("bow"),Debuff("debuff"),Elytra("elytra"),Manhunt("manhunt"),Trident("trident");
        final String id;DisplayMode(String id){this.id=id;}
        static DisplayMode current(){return Arrays.stream(values()).filter(mode->mode.id.equals(config().getGameMode().id())).findFirst().orElse(None);}
    }
    @Override public Config define(){
        
        var general=OptionGroup.builder("General")
            .option(toggle("Mod Enabled","enabled","Show Afri-Tiers tags in game",true,()->config().isEnabled(),value->config().setEnabled(value)))
            .option(Options.ofEnum("Gamemode",DisplayMode::current,DisplayMode.Overall,value->{config().setGameMode(value.id);save();},DisplayMode.class).saveKey("gamemode").build())
            .option(toggle("Show Retired Players","retired","Include retired tier placements",true,()->config().isShowRetired(),value->config().setShowRetired(value)))
            .option(Options.ofEnum("Highest Tier",()->config().getHighestMode(),TierTaggerConfig.HighestMode.NOT_FOUND,value->{config().setHighestMode(value);save();},TierTaggerConfig.HighestMode.class).saveKey("highest").description("When to display the player's highest tier").build())
            .option(toggle("Name Tags","nameTags","Display tiers beside player name tags",true,()->config().isNameTags(),value->config().setNameTags(value)))
            .option(toggle("Tab List","playerList","Display tiers in the player list",true,()->config().isPlayerList(),value->config().setPlayerList(value)))
            .option(toggle("Kit Detection","kitIdentifier","Choose a gamemode from your equipment",false,()->config().isKitIdentifier(),value->{config().setKitIdentifier(value);KitIdentifier.clear();}))
            .option(toggle("Rank Numbers","rankNumbers","Display the player's overall rank and top-three trophies",true,()->config().isRankNumbers(),value->config().setRankNumbers(value)))
            .option(toggle("Podium Colors","podiumColors","Gold, silver and bronze for the top three",true,()->config().isPodiumColors(),value->config().setPodiumColors(value)))
            .option(toggle("Show Peak Tiers","showPeakTiers","Include earned peaks beside current tiers",true,()->config().isShowPeakTiers(),value->config().setShowPeakTiers(value)))
            .option(toggle("Show Main Tiers","showMainTiers","Display placements in the nine main modes",true,()->config().isShowMainTiers(),value->config().setShowMainTiers(value)))
            .option(toggle("Show Sub Tiers","showSubTiers","Display placements in the nine subtier modes",true,()->config().isShowSubTiers(),value->config().setShowSubTiers(value)))
            .option(Options.ofEnum("Icon Style",()->config().getIconSet(),TierTaggerConfig.IconSet.AfriTiers,value->{config().setIconSet(value);save();},TierTaggerConfig.IconSet.class).saveKey("iconSet").build())
            .option(toggle("Show Own Tier in F5","ownTierF5","Show your tag while using third-person view",true,()->config().isShowOwnTier(),value->config().setShowOwnTier(value)))
            .option(Options.ofButton("Search Players",()->{var client=MinecraftClient.getInstance();client.setScreen(new PlayerSearchScreen(client.currentScreen));}).build()).build();
        var alerts=OptionGroup.builder("Alerts")
            .option(Options.ofButton("Open Queues",()->{var client=MinecraftClient.getInstance();client.setScreen(new com.skalpha.tiertagger.queue.QueueScreen(client.currentScreen));}).build())
            .option(toggle("Waitlist Alerts","waitlistAlerts","Notify you when a testing queue opens",false,()->config().isWaitlistAlerts(),value->config().setWaitlistAlerts(value)))
            .option(Options.ofEnum("Waitlist Location",()->config().getWaitlistLocation(),TierTaggerConfig.AlertLocation.TITLE,value->{config().setWaitlistLocation(value);save();},TierTaggerConfig.AlertLocation.class).saveKey("waitlistLocation").build())
            .option(toggle("Waitlist Sound","waitlistSound","Play a sound for queue notifications",false,()->config().isWaitlistSound(),value->config().setWaitlistSound(value)))
            .option(toggle("Ticket Alerts","ticketAlerts","Notify you when your test ticket opens",false,()->config().isTicketAlerts(),value->config().setTicketAlerts(value)))
            .option(Options.ofEnum("Ticket Location",()->config().getTicketLocation(),TierTaggerConfig.AlertLocation.TITLE,value->{config().setTicketLocation(value);save();},TierTaggerConfig.AlertLocation.class).saveKey("ticketLocation").build())
            .option(toggle("Ticket Sound","ticketSound","Play a sound for ticket notifications",false,()->config().isTicketSound(),value->config().setTicketSound(value))).build();
        var palette=OptionGroup.builder("Tier Colors");
        for(String key:List.of("retired","HT1","LT1","HT2","LT2","HT3","LT3","HT4","LT4","HT5","LT5")){
            String hex=key.equals("retired")?config().getRetiredColorHex():config().getTierColorsHex().get(key);
            WLColor initial=new WLColor(0xFF000000|TierTaggerConfig.parseColor(hex));colors.putIfAbsent(key,initial);
            palette.option(Options.ofColor(key.equals("retired")?"Retired":key,()->colors.get(key),initial,value->{colors.put(key,value);applyColor(key,value);save();}).saveKey("color_"+key).build());
        }
        menuColor=new WLColor(0xFF000000|config().getMenuColor());
        var appearance=OptionGroup.builder("Menu Settings")
            .option(Options.ofButton("Check for Updates",Updates::check).build())
            .option(Options.ofButton("Install Update After Closing Minecraft",Updates::install).build())
            .option(toggle("Liquid Glass","liquidGlass","Use refractive glass panels. Off with the Minecraft icon style",true,()->config().isLiquidGlass(),value->config().setLiquidGlass(value)))
            .option(Options.ofColor("Menu Colour",()->menuColor,menuColor,value->{config().setMenuColor(value.getRGB());save();}).saveKey("menuColor").build())
            .option(toggle("Dark Theme","darkTheme","Switch between dark and light glass",true,()->config().isDarkTheme(),value->config().setDarkTheme(value)))
            .option(Options.ofNumber("Menu Size",()->config().getMenuScale(),1.0,value->{config().setMenuScale(value);save();}).range(0.8,1.5,0.1).saveKey("menuScale").build())
            .option(Options.ofNumber("Glass Opacity",()->config().getMenuOpacity(),1.0,value->{config().setMenuOpacity(value);save();}).range(0.4,1.0,0.05).saveKey("menuOpacity").build())
            .option(toggle("Menu Animations","menuAnimations","Animate the panel when opening",true,()->config().isMenuAnimations(),value->config().setMenuAnimations(value))).build();
        return Config.builder().path(FabricLoader.getInstance().getConfigDir().resolve("afritiers-walksy.json"))
            .category(Category.builder("Afri-Tiers").group(general).group(alerts).group(palette.build()).group(appearance).build()).onSave(WalksySettings::save).disableUpdateChecks().build();
    }
    private static void applyColor(String key,WLColor color){
        String hex=String.format("#%06X",color.getRGB()&0xFFFFFF);
        if(key.equals("retired"))config().setRetiredColorHex(hex);else config().setTierColorHex(key,hex);
    }
    public static void tickColors(){
        boolean changed=false;
        if(menuColor!=null&&(menuColor.isRainbow()||menuColor.isPulse())){menuColor.tick();config().setMenuColor(menuColor.getRGB());}
        for(var entry:colors.entrySet()){
            WLColor color=entry.getValue();if(!color.isRainbow()&&!color.isPulse())continue;
            color.tick();
            String before=entry.getKey().equals("retired")?config().getRetiredColorHex():config().getTierColorsHex().get(entry.getKey());
            applyColor(entry.getKey(),color);changed|=!before.equalsIgnoreCase(String.format("#%06X",color.getRGB()&0xFFFFFF));
        }
        if(changed)TierTagger.invalidateTierComponentCache();
    }
}
