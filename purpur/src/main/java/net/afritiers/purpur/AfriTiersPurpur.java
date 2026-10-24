package net.afritiers.purpur;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.gson.*;
import com.skalpha.tiertagger.net.PublicHttp;
import com.skalpha.tiertagger.queue.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class AfriTiersPurpur extends JavaPlugin implements Listener,CommandExecutor,TabCompleter,PluginMessageListener {
    final Rankings rankings=new Rankings();
    Preferences.Store preferences;
    TagRenderer tags;
    SettingsDialogs dialogs;
    final Set<UUID> modded=ConcurrentHashMap.newKeySet(),packLoaded=ConcurrentHashMap.newKeySet();
    final Map<UUID,Session> sessions=new ConcurrentHashMap<>();
    volatile List<Subject> subjects=List.of();
    private volatile List<QueueFeed.Entry> queues=List.of();
    private final QueueTracker tracker=new QueueTracker();
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final AtomicBoolean polling=new AtomicBoolean(),refreshing=new AtomicBoolean();
    private volatile boolean running;
    private TicketService tickets;
    private String packUrl="",packSha1="";
    static final String ICON_SHA1="b50b99783b5b86699c6cd36802c88d638dd63678";
    private final Map<UUID,Set<UUID>> loadedPacks=new ConcurrentHashMap<>();
    private static final UUID PACK_ID=UUID.fromString("77a65b27-b380-4ea8-a564-a8e70019e833");
    private PluginCommand linkedQueue;
    private TabCompleter previousCompleter;
    record Subject(UUID id,String name,Component display,String kit){}
    record Notice(Component title,Component subtitle,boolean ticket){}
    static final class Session {
        final Set<String> seen=new LinkedHashSet<>();
        final Deque<Notice> notices=new ArrayDeque<>();
        int displayTicks,notes;
    }
    @Override public void onEnable(){
        try{
            saveDefaultConfig();loadSettings();preferences=new Preferences.Store(getDataFolder().toPath());
            dialogs=new SettingsDialogs(this);dialogs.verify();getLogger().info("Native settings dialog pages verified.");SettingsCommand settings=new SettingsCommand(this);getCommand("afritiers-settings").setExecutor(settings);getCommand("afritiers-settings").setTabCompleter(settings);
            for(String command:List.of("afritiers","afritiersreload","join-queue")){getCommand(command).setExecutor(this);getCommand(command).setTabCompleter(this);}
            linkedQueue=Bukkit.getPluginCommand("afritierslink:join-queue");
            if(linkedQueue!=null){previousCompleter=linkedQueue.getTabCompleter();linkedQueue.setTabCompleter(this);}
            Bukkit.getPluginManager().registerEvents(this,this);
            Bukkit.getMessenger().registerIncomingPluginChannel(this,"afritiers:hello",this);
            tags=new TagRenderer(this);PacketEvents.getAPI().getEventManager().registerListener(tags);
            tickets=new TicketService(this);running=true;tickets.verifyBackend();
            for(Player player:Bukkit.getOnlinePlayers())connected(player);
            Bukkit.getScheduler().runTaskTimer(this,this::tick,2,2);
            Bukkit.getScheduler().runTaskTimer(this,this::snapshot,1,20);
            Bukkit.getScheduler().runTaskTimer(this,this::pollQueues,1,100);
            Bukkit.getScheduler().runTaskTimer(this,this::refreshRankings,1,2400);
            Bukkit.getScheduler().runTaskTimer(this,tickets::poll,40,100);
            getLogger().info("Afri-Tiers Purpur enabled for 1.21.11. Settings: /afritiers-settings.");
        }catch(Exception error){getLogger().log(java.util.logging.Level.SEVERE,"Cannot initialize Afri-Tiers Purpur. Check config.yml and dependencies.",error);Bukkit.getPluginManager().disablePlugin(this);}
    }
    private void loadSettings(){
        reloadConfig();String url=getConfig().getString("resource-pack-url",""),hash=getConfig().getString("resource-pack-sha1","");
        if(!url.isEmpty()){URI uri=URI.create(url);if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||!hash.matches("[A-Fa-f0-9]{40}"))throw new IllegalArgumentException("Configure an HTTPS resource pack and SHA-1.");}
        packUrl=url;packSha1=hash;
    }
    Preferences prefs(Player player){return preferences.get(player.getUniqueId());}
    List<String> openModes(String prefix){return QueueFeed.openModes(queues,prefix);}
    void setQueues(List<QueueFeed.Entry> value){queues=List.copyOf(value);}
    boolean current(Player player,Session session){return running&&player.isOnline()&&Bukkit.getPlayer(player.getUniqueId())==player&&sessions.get(player.getUniqueId())==session;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(command.getName().equals("afritiersreload")){
            if(!sender.hasPermission("afritiers.purpur.reload")&&!sender.hasPermission("afritiers.reload")){sender.sendMessage(Component.text("You do not have permission.",RED));return true;}
            try{loadSettings();tickets.reload();tickets.verifyBackend();refreshRankings();sender.sendMessage(Component.text("Afri-Tiers reloaded.",GREEN));}catch(Exception error){sender.sendMessage(Component.text("Invalid configuration. Check the server console.",RED));}return true;
        }
        if(command.getName().equals("join-queue")){if(sender instanceof Player player)join(player,args);else sender.sendMessage("Run this command in-game.");return true;}
        if(args.length!=1||!args[0].matches("[A-Za-z0-9_]{1,16}")){sender.sendMessage(Component.text("Use /afritiers <Minecraft username>.",YELLOW));return true;}
        var found=rankings.find(null,args[0]);if(found.isEmpty()){sender.sendMessage(Component.text("No Afri-Tiers profile found.",GRAY));return true;}
        var profile=found.get();sender.sendMessage(Component.text(profile.name()+" — "+(profile.position()>0?"Overall #"+profile.position():"Unranked")+" — "+profile.points()+" points — "+profile.region(),AQUA));
        for(var tier:profile.tiers())sender.sendMessage(Component.text(tier.mode()+": "+(tier.retired()?"R":"")+tier.level(),WHITE));return true;
    }
    void join(Player player,String[] args){
        if(args.length!=1||!Rankings.MODES.contains(Rankings.mode(args[0]))||Rankings.mode(args[0]).equals("overall")){player.sendMessage(Component.text("Use /join-queue <gamemode>.",YELLOW));return;}
        PluginCommand backend=Bukkit.getPluginCommand("afritierslink:join-queue");
        if(backend==null){player.sendMessage(Component.text("Queue joining requires the configured AfriTiersLink plugin on this server.",RED));return;}
        backend.execute(player,"afritierslink:join-queue",new String[]{Rankings.mode(args[0])});
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(command.getName().equals("join-queue"))return args.length==1?openModes(args[0]):List.of();
        if(command.getName().equals("afritiers")&&args.length==1)return rankings.all().stream().map(Rankings.Profile::name).filter(name->name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).sorted().toList();return List.of();
    }
    private CompletableFuture<String> get(String path,int limit){return http.sendAsync(HttpRequest.newBuilder(URI.create("https://afritiers.co.za"+path)).timeout(Duration.ofSeconds(15)).header("User-Agent","AfriTiersPurpur/1.1.0").GET().build(),PublicHttp.limitedUtf8(limit)).thenApply(response->{if(response.statusCode()!=200)throw new IllegalStateException();return response.body();});}
    private void refreshRankings(){if(!running||!refreshing.compareAndSet(false,true))return;get("/api/players",8_000_000).thenAccept(rankings::update).whenComplete((result,error)->{refreshing.set(false);if(error!=null)getLogger().warning("Rankings refresh failed; retaining the cached public rankings.");});}
    private void pollQueues(){if(!running||!polling.compareAndSet(false,true))return;
        get("/api/queues",65_536).thenAccept(raw->{QueueFeed feed=QueueFeed.parse(raw);Bukkit.getScheduler().runTask(this,()->{
            if(!running)return;Instant now=Instant.now();if(feed.generatedAt().isBefore(now.minusSeconds(120))||feed.generatedAt().isAfter(now.plusSeconds(60)))return;
            var opened=tracker.accept(feed,now);setQueues(feed.queues());
            for(var queue:opened)for(Player player:Bukkit.getOnlinePlayers()){Session session=sessions.get(player.getUniqueId());if(session!=null&&!modded.contains(player.getUniqueId())&&prefs(player).waitlistAlerts&&session.notices.size()<16)session.notices.addLast(new Notice(Component.text(queue.mode().toUpperCase(Locale.ROOT)+" queue has just opened!",RED),Component.text("Do /join-queue "+queue.mode()+" to join the waitlist!",WHITE),false));}
        });}).whenComplete((result,error)->polling.set(false));
    }
    private void snapshot(){if(!running)return;subjects=Bukkit.getOnlinePlayers().stream().map(player->new Subject(player.getUniqueId(),player.getName(),player.playerListName(),PurpurKit.snapshot(player))).toList();for(Player player:Bukkit.getOnlinePlayers())tags.refresh(player);}
    static boolean serverIcons(org.bukkit.packs.ResourcePack pack,String expected){return pack!=null&&pack.getId()!=null&&expected.matches("[a-fA-F0-9]{40}")&&expected.equalsIgnoreCase(pack.getHash());}
    private boolean serverIcons(){return serverIcons(Bukkit.getServer().getServerResourcePack(),packSha1.isBlank()?ICON_SHA1:packSha1);}
    void offerIcons(Player player){
        if(!packUrl.isBlank())player.setResourcePack(PACK_ID,packUrl,HexFormat.of().parseHex(packSha1),Component.text("Afri-Tiers gamemode and rank trophy icons"),false);
        else if(serverIcons()){var pack=Bukkit.getServer().getServerResourcePack();player.setResourcePack(pack.getId(),pack.getUrl(),HexFormat.of().parseHex(pack.getHash()),Component.text("Afri-Tiers gamemode and rank trophy icons"),false);}
    }
    private void connected(Player player){preferences.get(player.getUniqueId());Session session=new Session();sessions.put(player.getUniqueId(),session);if(!packUrl.isEmpty()&&!serverIcons())Bukkit.getScheduler().runTaskLater(this,()->{if(current(player,session)&&!modded.contains(player.getUniqueId())&&!packLoaded.contains(player.getUniqueId()))offerIcons(player);},40);}
    @EventHandler public void joined(PlayerJoinEvent event){connected(event.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent event){UUID id=event.getPlayer().getUniqueId();sessions.remove(id);modded.remove(id);packLoaded.remove(id);loadedPacks.remove(id);preferences.remove(id);tags.reset(id);}
    @EventHandler public void pack(PlayerResourcePackStatusEvent event){
        var serverPack=Bukkit.getServer().getServerResourcePack();boolean own=PACK_ID.equals(event.getID())&&!packUrl.isBlank();boolean supplied=serverIcons()&&serverPack.getId().equals(event.getID());if(!own&&!supplied)return;
        UUID viewer=event.getPlayer().getUniqueId();Set<UUID> loaded=loadedPacks.computeIfAbsent(viewer,id->ConcurrentHashMap.newKeySet());
        if(event.getStatus()==PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED)loaded.add(event.getID());else if(!Set.of(PlayerResourcePackStatusEvent.Status.ACCEPTED,PlayerResourcePackStatusEvent.Status.DOWNLOADED).contains(event.getStatus()))loaded.remove(event.getID());
        if(loaded.isEmpty())packLoaded.remove(viewer);else packLoaded.add(viewer);tags.refresh(event.getPlayer());
    }
    @Override public void onPluginMessageReceived(String channel,Player player,byte[] message){if(channel.equals("afritiers:hello")&&message.length<=32){modded.add(player.getUniqueId());tags.refresh(player);}}
    void tickets(Player player,Session session,JsonArray rows){if(!current(player,session)||modded.contains(player.getUniqueId())||rows.size()>16)return;
        for(var element:rows){var row=element.getAsJsonObject();String event=row.get("eventId").getAsString(),name=row.get("username").getAsString(),mode=row.get("mode").getAsString();if(!event.matches("[a-f0-9]{64}")||!name.equalsIgnoreCase(player.getName())||!Rankings.MODES.contains(mode)||!session.seen.add(event))continue;if(prefs(player).ticketAlerts&&session.notices.size()<16)session.notices.addFirst(new Notice(Component.text(player.getName(),GREEN),Component.text("Your Testing Ticket for "+mode.toUpperCase(Locale.ROOT)+" is open",WHITE),true));}
        while(session.seen.size()>256){var first=session.seen.iterator();first.next();first.remove();}
    }
    private void tick(){if(!running)return;for(Player player:Bukkit.getOnlinePlayers()){
        Session session=sessions.get(player.getUniqueId());if(session==null||modded.contains(player.getUniqueId()))continue;var p=prefs(player);
        if(!p.waitlistAlerts||!p.waitlistSound)session.notes=0;if(session.notes>0){sound(player);session.notes--;}
        if(session.displayTicks>0){session.displayTicks-=2;continue;}Notice notice=session.notices.pollFirst();if(notice==null||(notice.ticket()?!p.ticketAlerts:!p.waitlistAlerts))continue;
        if((notice.ticket()?p.ticketLocation:p.waitlistLocation).equals("chat"))player.sendMessage(notice.title().append(Component.space()).append(notice.subtitle()));else player.showTitle(Title.title(notice.title(),notice.subtitle(),Title.Times.times(Duration.ZERO,Duration.ofSeconds(3),Duration.ZERO)));
        session.displayTicks=60;if(notice.ticket()){session.notes=0;if(p.ticketSound)sound(player);}else if(p.waitlistSound){sound(player);session.notes=29;}
    }}
    private void sound(Player player){player.playSound(player.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,1,1);}
    @Override public void onDisable(){running=false;Bukkit.getScheduler().cancelTasks(this);if(tags!=null){for(Player player:Bukkit.getOnlinePlayers())tags.restore(player);PacketEvents.getAPI().getEventManager().unregisterListener(tags);}if(linkedQueue!=null&&linkedQueue.getTabCompleter()==this)linkedQueue.setTabCompleter(previousCompleter);if(tickets!=null)tickets.close();sessions.clear();}
}
