package net.afritiers.velocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Preferences {
    public volatile boolean enabled=true, playerList=true, nameTags=true, showRetired=true, icons=true, kitIdentifier=false;
    public volatile boolean waitlistAlerts=true, waitlistSound=true, ticketAlerts=true, ticketSound=true;
    public volatile boolean rankNumbers=true, rankTrophies=true, podiumColors=true;
    public volatile String gameMode="overall", highest="not_found", waitlistLocation="title", ticketLocation="title";
    public Map<String,Integer> colors=new ConcurrentHashMap<>(Map.ofEntries(
        Map.entry("HT1",15252026),Map.entry("LT1",14005077),Map.entry("HT2",12899303),Map.entry("LT2",10528690),
        Map.entry("HT3",16293722),Map.entry("LT3",13007682),Map.entry("HT4",8483994),Map.entry("LT4",6642553),
        Map.entry("HT5",9405096),Map.entry("LT5",6642553),Map.entry("retired",10671871)));
    static int parseColor(String value) {
        String hex=value.replaceFirst("^#","");
        if(hex.matches("[0-9a-fA-F]{3}"))hex=""+hex.charAt(0)+hex.charAt(0)+hex.charAt(1)+hex.charAt(1)+hex.charAt(2)+hex.charAt(2);
        if(!hex.matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Use #RRGGBB or #RGB.");
        return Integer.parseInt(hex,16);
    }
    public static final class Store {
        private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
        private final Map<UUID,Preferences> cache=new ConcurrentHashMap<>();
        private final Path directory;
        public Store(Path root)throws Exception{directory=root.resolve("players");Files.createDirectories(directory);}
        public Preferences get(UUID uuid){return cache.computeIfAbsent(uuid,id->{
            Path file=directory.resolve(id+".json");
            try{Preferences p=Files.exists(file)?JSON.fromJson(Files.readString(file),Preferences.class):new Preferences();if(p==null||p.colors==null||!Rankings.MODES.contains(p.gameMode)||!List.of("always","never","not_found").contains(p.highest)||!List.of("chat","title").contains(p.waitlistLocation)||!List.of("chat","title").contains(p.ticketLocation))throw new IllegalArgumentException();for(String key:new Preferences().colors.keySet())if(!p.colors.containsKey(key)||p.colors.get(key)==null||p.colors.get(key)<0||p.colors.get(key)>0xFFFFFF)throw new IllegalArgumentException();p.colors=new ConcurrentHashMap<>(p.colors);return p;}
            catch(Exception error){try{if(Files.exists(file))Files.copy(file,file.resolveSibling(id+".invalid-"+System.currentTimeMillis()+".json"));}catch(Exception ignored){}return new Preferences();}
        });}
        public synchronized void save(UUID uuid)throws Exception{
            Path file=directory.resolve(uuid+".json"),temporary=directory.resolve(uuid+".tmp");
            Files.writeString(temporary,JSON.toJson(get(uuid)));
            try{Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException error){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
        }
        public void remove(UUID uuid){cache.remove(uuid);}
    }
}
