package net.afritiers.purpur;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import static net.kyori.adventure.text.format.NamedTextColor.*;

final class SettingsCommand implements CommandExecutor, TabCompleter {
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){execute(sender,args);return true;}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){return suggest(args);}
    private final AfriTiersPurpur plugin;
    private static final List<String> BOOLEAN=List.of("enabled","tab","nametags","retired","icons","kit","rank-numbers","rank-trophies","podium-colors","waitlist","waitlist-sound","ticket","ticket-sound");
    SettingsCommand(AfriTiersPurpur plugin){this.plugin=plugin;}
    private void execute(CommandSender sender,String[] args){
        if(!(sender instanceof Player player)){sender.sendMessage(Component.text("Run /afritiers-settings in-game.",RED));return;}
        var prefs=plugin.prefs(player);
        try{
            if(args.length>0&&!List.of("colors","general","alerts").contains(args[0])){
                change(prefs,args);
                plugin.preferences.save(player.getUniqueId());plugin.tags.refresh(player);
                player.sendMessage(Component.text("Afri-Tiers setting saved.",GREEN));
            }
            String page=args.length==0?"home":switch(args[0]){case "colors","color"->"colors";case "general"->"general";case "alerts"->"alerts";default->"home";};
            plugin.dialogs.open(player,page,"");
        }catch(IllegalArgumentException error){player.sendMessage(Component.text(error.getMessage(),RED));}
        catch(Exception error){player.sendMessage(Component.text("Could not save settings; ask staff to check the server's data directory.",RED));}
    }
    static void change(Preferences prefs,String[] args){
        if(args.length==3&&args[0].equals("color")){
            String key=args[1].equalsIgnoreCase("retired")?"retired":args[1].toUpperCase(Locale.ROOT);
            if(!prefs.colors.containsKey(key))throw new IllegalArgumentException("Choose retired, HT1–HT5 or LT1–LT5.");
            prefs.colors.put(key,Preferences.parseColor(args[2]));return;
        }
        if(args.length!=2)throw new IllegalArgumentException("Use /afritiers-settings, or click a setting in the menu.");
        String key=args[0],value=args[1].toLowerCase(Locale.ROOT);
        if(BOOLEAN.contains(key)){
            if(!value.equals("on")&&!value.equals("off"))throw new IllegalArgumentException("Use on or off.");
            boolean enabled=value.equals("on");
            switch(key){case "enabled"->prefs.enabled=enabled;case "tab"->prefs.playerList=enabled;case "nametags"->prefs.nameTags=enabled;case "retired"->prefs.showRetired=enabled;case "icons"->prefs.icons=enabled;case "kit"->prefs.kitIdentifier=enabled;case "rank-numbers"->prefs.rankNumbers=enabled;case "rank-trophies"->prefs.rankTrophies=enabled;case "podium-colors"->prefs.podiumColors=enabled;case "waitlist"->prefs.waitlistAlerts=enabled;case "waitlist-sound"->prefs.waitlistSound=enabled;case "ticket"->prefs.ticketAlerts=enabled;case "ticket-sound"->prefs.ticketSound=enabled;}
            return;
        }
        switch(key){
            case "mode"->{String mode=Rankings.mode(value);if(!Rankings.MODES.contains(mode))throw new IllegalArgumentException("Unknown gamemode.");prefs.gameMode=mode;}
            case "highest"->{if(!List.of("always","never","not_found").contains(value))throw new IllegalArgumentException("Use always, never or not_found.");prefs.highest=value;}
            case "waitlist-location","ticket-location"->{if(!List.of("chat","title").contains(value))throw new IllegalArgumentException("Use chat or title.");if(key.equals("waitlist-location"))prefs.waitlistLocation=value;else prefs.ticketLocation=value;}
            default->throw new IllegalArgumentException("Unknown Afri-Tiers setting.");
        }
    }
    private List<String> suggest(String[] args){
        
        if(args.length<=1){String prefix=args.length==0?"":args[0];List<String> choices=new ArrayList<>(BOOLEAN);choices.addAll(List.of("mode","highest","general","alerts","colors","color","waitlist-location","ticket-location"));return choices.stream().filter(v->v.startsWith(prefix)).toList();}
        List<String> values=BOOLEAN.contains(args[0])?List.of("on","off"):switch(args[0]){case "mode"->Rankings.MODES;case "highest"->List.of("always","never","not_found");case "waitlist-location","ticket-location"->List.of("chat","title");case "color"->List.of("retired","HT1","LT1","HT2","LT2","HT3","LT3","HT4","LT4","HT5","LT5");default->List.of();};
        return args.length==2?values.stream().filter(v->v.startsWith(args[1])).toList():List.of();
    }
}
