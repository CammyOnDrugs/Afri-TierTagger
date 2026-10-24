package net.afritiers.purpur;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.format.NamedTextColor.*;

final class SettingsDialogs {
    record Toggle(String key,String title){}
    static final List<Toggle> GENERAL=List.of(new Toggle("enabled","Enable tier display"),new Toggle("nametags","Nametags"),new Toggle("tab","Tab list"),new Toggle("retired","Show retired tiers"),new Toggle("icons","Gamemode icons"),new Toggle("kit","Identify kits"),new Toggle("rank-numbers","Overall rank numbers"),new Toggle("rank-trophies","Rank trophy icons"),new Toggle("podium-colors","Gold, silver and bronze colors"));
    static final List<Toggle> ALERTS=List.of(new Toggle("waitlist","Queue-opening alerts"),new Toggle("waitlist-sound","Queue-opening sound"),new Toggle("ticket","Testing-ticket alerts"),new Toggle("ticket-sound","Testing-ticket sound"));
    static final List<String> COLORS=List.of("HT1","LT1","HT2","LT2","HT3","LT3","HT4","LT4","HT5","LT5","retired");
    private final AfriTiersPurpur plugin;
    SettingsDialogs(AfriTiersPurpur plugin){this.plugin=plugin;}
    void verify(){UUID owner=UUID.randomUUID();for(String page:List.of("home","general","colors","alerts"))create(owner,new Preferences(),page,"",false);}
    void open(Player player,String page,String message){player.showDialog(create(player.getUniqueId(),plugin.prefs(player),page,message,plugin.packLoaded.contains(player.getUniqueId())));}
    Dialog create(UUID owner,Preferences prefs,String page,String message,boolean iconsLoaded){
        if(!Set.of("home","general","colors","alerts").contains(page))throw new IllegalArgumentException("Unknown settings page.");
        List<DialogBody> body=new ArrayList<>();
        if(!message.isBlank())body.add(DialogBody.plainMessage(Component.text(message, message.startsWith("Saved")?GREEN:RED),320));
        if(page.equals("home")){
            body.add(DialogBody.plainMessage(Component.text("Choose the display, tier colors or alerts you want to change.",WHITE),320));
            body.add(DialogBody.plainMessage(Component.text(iconsLoaded?"Icon pack loaded":"Accept the Afri-Tiers resource pack to see gamemode and trophy icons.",iconsLoaded?GREEN:GRAY),320));
            return Dialog.create(builder->builder.empty().base(base("Afri-Tiers Settings",body,List.of())).type(DialogType.multiAction(List.of(
                button(owner,"Display & rankings",(p,v)->open(p,"general","")),button(owner,"Tier colors",(p,v)->open(p,"colors","")),button(owner,"Alerts",(p,v)->open(p,"alerts","")),button(owner,"Load icon pack",(p,v)->{plugin.offerIcons(p);open(p,"home","");})
            ),ActionButton.create(Component.text("Done"),null,150,null),2)));
        }
        List<DialogInput> inputs=new ArrayList<>();
        if(page.equals("general")){
            for(Toggle toggle:GENERAL)inputs.add(DialogInput.bool(inputKey(toggle.key()),Component.text(toggle.title())).initial(flag(prefs,toggle.key())).build());
            inputs.add(option("mode","Gamemode",Rankings.MODES,prefs.gameMode));
            inputs.add(option("highest","Highest tier fallback",List.of("not_found","always","never"),prefs.highest));
        }else if(page.equals("alerts")){
            for(Toggle toggle:ALERTS)inputs.add(DialogInput.bool(inputKey(toggle.key()),Component.text(toggle.title())).initial(flag(prefs,toggle.key())).build());
            inputs.add(option("waitlist-location","Queue-opening display",List.of("title","chat"),prefs.waitlistLocation));
            inputs.add(option("ticket-location","Testing-ticket display",List.of("title","chat"),prefs.ticketLocation));
        }else{
            body.add(DialogBody.plainMessage(Component.text("Enter a color such as #12ABEF. Changes apply when you press Save.",GRAY),320));
            for(String key:COLORS)inputs.add(DialogInput.text(key,Component.text(key.equals("retired")?"Retired tiers":key,TextColor.color(prefs.colors.get(key)))).initial(String.format("#%06X",prefs.colors.get(key))).maxLength(7).width(300).build());
        }
        String title=switch(page){case "general"->"Display & rankings";case "alerts"->"Queue & ticket alerts";default->"Tier colors";};
        return Dialog.create(builder->builder.empty().base(base(title,body,inputs)).type(DialogType.multiAction(List.of(
            button(owner,"Save",(player,view)->save(player,page,view)),button(owner,"Cancel",(player,view)->open(player,"home",""))
        ),ActionButton.create(Component.text("Close"),null,150,null),2)));
    }
    private DialogBase base(String title,List<DialogBody> body,List<DialogInput> inputs){return DialogBase.builder(Component.text(title,GOLD)).canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.CLOSE).body(body).inputs(inputs).build();}
    private DialogInput option(String key,String title,List<String> values,String selected){return DialogInput.singleOption(inputKey(key),Component.text(title),values.stream().map(value->OptionEntry.create(value,Component.text(label(value)),value.equals(selected))).toList()).width(300).build();}
    private String label(String value){return switch(value){case "not_found"->"When selected mode is missing";case "always"->"Always show highest tier";case "never"->"Never use fallback";case "title"->"Title";case "chat"->"Chat";case "overall"->"Overall";case "diavanilla"->"DiaVanilla";case "diapot"->"DiaPot";case "nethpot"->"NethPot";case "spear"->"Spearmace";default->value.substring(0,1).toUpperCase(Locale.ROOT)+value.substring(1);};}
    private ActionButton button(UUID owner,String label,BiConsumer<Player,DialogResponseView> action){
        return ActionButton.create(Component.text(label,AQUA),null,150,DialogAction.customClick((view,audience)->{
            if(!(audience instanceof Player player)||!owner.equals(player.getUniqueId())||!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{if(plugin.isEnabled()&&player.isOnline()&&Bukkit.getPlayer(owner)==player)action.accept(player,view);});
        },ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build()));
    }
    private void save(Player player,String page,DialogResponseView view){
        try{Map<String,String> values=read(page,view);apply(plugin.prefs(player),page,values);plugin.preferences.save(player.getUniqueId());plugin.tags.refresh(player);open(player,"home","Saved your settings.");}
        catch(IllegalArgumentException error){open(player,page,error.getMessage());}
        catch(Exception error){open(player,page,"Could not save your settings. Please try again.");}
    }
    static Map<String,String> read(String page,DialogResponseView view){
        if(view==null)throw new IllegalArgumentException("Please reopen settings and try again.");
        Map<String,String> values=new LinkedHashMap<>();
        if(page.equals("colors")){for(String key:COLORS)values.put(key,view.getText(inputKey(key)));return values;}
        for(Toggle toggle:page.equals("general")?GENERAL:ALERTS){Boolean value=view.getBoolean(inputKey(toggle.key()));if(value==null)throw new IllegalArgumentException("A setting is missing. Please reopen this menu.");values.put(toggle.key(),value?"on":"off");}
        for(String key:page.equals("general")?List.of("mode","highest"):List.of("waitlist-location","ticket-location"))values.put(key,view.getText(inputKey(key)));
        return values;
    }
    static void apply(Preferences prefs,String page,Map<String,String> values){
        Set<String> expected=new HashSet<>();
        if(page.equals("colors"))expected.addAll(COLORS);
        else if(page.equals("general")||page.equals("alerts")){
            for(Toggle toggle:page.equals("general")?GENERAL:ALERTS)expected.add(toggle.key());
            expected.addAll(page.equals("general")?List.of("mode","highest"):List.of("waitlist-location","ticket-location"));
        }else throw new IllegalArgumentException("Unknown settings page.");
        if(!values.keySet().equals(expected)||values.values().stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("A setting is missing. Please reopen this menu.");
        if(page.equals("colors")){Map<String,Integer> parsed=new HashMap<>();values.forEach((key,value)->parsed.put(key,Preferences.parseColor(value.trim())));prefs.colors.putAll(parsed);return;}
        Preferences validation=new Preferences();values.forEach((key,value)->SettingsCommand.change(validation,new String[]{key,value}));
        values.forEach((key,value)->SettingsCommand.change(prefs,new String[]{key,value}));
    }
    static String inputKey(String key){return key.replace('-','_');}
    static boolean flag(Preferences p,String key){return switch(key){case "enabled"->p.enabled;case "tab"->p.playerList;case "nametags"->p.nameTags;case "retired"->p.showRetired;case "icons"->p.icons;case "kit"->p.kitIdentifier;case "rank-numbers"->p.rankNumbers;case "rank-trophies"->p.rankTrophies;case "podium-colors"->p.podiumColors;case "waitlist"->p.waitlistAlerts;case "waitlist-sound"->p.waitlistSound;case "ticket"->p.ticketAlerts;case "ticket-sound"->p.ticketSound;default->throw new IllegalArgumentException("Unknown toggle.");};}
}
