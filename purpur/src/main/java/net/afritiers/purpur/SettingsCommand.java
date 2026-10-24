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
            if(args.length>0&&!args[0].equals("colors")){
                change(prefs,args);
                plugin.preferences.save(player.getUniqueId());plugin.tags.refresh(player);
                player.sendMessage(Component.text("Afri-Tiers setting saved.",GREEN));
            }
            if(args.length>0&&(args[0].equals("colors")||args[0].equals("color"))){colors(player,prefs);return;}
            menu(player,prefs);
        }catch(IllegalArgumentException error){player.sendMessage(Component.text(error.getMessage(),RED));}
        catch(Exception error){player.sendMessage(Component.text("Could not save settings; ask staff to check the proxy's data directory.",RED));}
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
    private Component action(String text,String args){return Component.text("["+text+"]",AQUA).clickEvent(ClickEvent.runCommand("/afritiers-settings "+args));}
    private void row(Player player,String title,String key,boolean enabled){player.sendMessage(Component.text(title+": ",WHITE).append(action(enabled?"ON":"OFF",key+" "+(enabled?"off":"on"))));}
    private void menu(Player player,Preferences p){
        player.sendMessage(Component.text("Afri-Tiers Settings — click to change",GOLD));
        row(player,"Enabled","enabled",p.enabled);row(player,"Tab list","tab",p.playerList);row(player,"Nametags","nametags",p.nameTags);
        int next=(Rankings.MODES.indexOf(p.gameMode)+1)%Rankings.MODES.size();
        player.sendMessage(Component.text("Gamemode: ",WHITE).append(action(p.gameMode,"mode "+Rankings.MODES.get(next))));
        String nextHighest=switch(p.highest){case "always"->"never";case "never"->"not_found";default->"always";};
        player.sendMessage(Component.text("Highest tier fallback: ",WHITE).append(action(p.highest,"highest "+nextHighest)));
        row(player,"Show retired","retired",p.showRetired);row(player,"Gamemode icons","icons",p.icons);row(player,"Kit identifier","kit",p.kitIdentifier);
        row(player,"Overall rank numbers","rank-numbers",p.rankNumbers);row(player,"Rank trophy icons","rank-trophies",p.rankTrophies);row(player,"Gold/silver/bronze colors","podium-colors",p.podiumColors);
        player.sendMessage(action("Tier colors","colors"));
        row(player,"Waitlist alerts","waitlist",p.waitlistAlerts);row(player,"Waitlist sound","waitlist-sound",p.waitlistSound);
        player.sendMessage(Component.text("Waitlist location: ",WHITE).append(action(p.waitlistLocation,"waitlist-location "+(p.waitlistLocation.equals("chat")?"title":"chat"))));
        row(player,"Ticket alerts","ticket",p.ticketAlerts);row(player,"Ticket sound","ticket-sound",p.ticketSound);
        player.sendMessage(Component.text("Ticket location: ",WHITE).append(action(p.ticketLocation,"ticket-location "+(p.ticketLocation.equals("chat")?"title":"chat"))));
        player.sendMessage(Component.text("Search: /afritiers <player>  •  Queue: /join-queue <mode>",GRAY));
    }
    private void colors(Player player,Preferences prefs){
        player.sendMessage(Component.text("Tier colors — click a swatch to edit #RRGGBB",GOLD));
        for(String key:List.of("retired","HT1","LT1","HT2","LT2","HT3","LT3","HT4","LT4","HT5","LT5")){
            String hex=String.format("#%06X",prefs.colors.get(key));
            player.sendMessage(Component.text("■ "+key+" "+hex,TextColor.color(prefs.colors.get(key))).clickEvent(ClickEvent.suggestCommand("/afritiers-settings color "+key+" "+hex)));
        }
        player.sendMessage(action("Back",""));
    }
    private List<String> suggest(String[] args){
        
        if(args.length<=1){String prefix=args.length==0?"":args[0];List<String> choices=new ArrayList<>(BOOLEAN);choices.addAll(List.of("mode","highest","colors","color","waitlist-location","ticket-location"));return choices.stream().filter(v->v.startsWith(prefix)).toList();}
        List<String> values=BOOLEAN.contains(args[0])?List.of("on","off"):switch(args[0]){case "mode"->Rankings.MODES;case "highest"->List.of("always","never","not_found");case "waitlist-location","ticket-location"->List.of("chat","title");case "color"->List.of("retired","HT1","LT1","HT2","LT2","HT3","LT3","HT4","LT4","HT5","LT5");default->List.of();};
        return args.length==2?values.stream().filter(v->v.startsWith(args[1])).toList():List.of();
    }
}
