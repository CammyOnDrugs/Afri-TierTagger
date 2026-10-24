package net.afritiers.velocity;

import com.google.gson.*;
import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.*;
import com.velocitypowered.api.event.player.*;
import com.velocitypowered.api.event.proxy.*;
import com.velocitypowered.api.plugin.*;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.*;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.github.retrooper.packetevents.PacketEvents;
import com.skalpha.tiertagger.net.PublicHttp;
import com.skalpha.tiertagger.queue.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.slf4j.Logger;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Plugin(id="afritiersvelocity",name="Afri-Tiers Velocity",version="1.0.1",description="Afri-Tiers tags, settings and queue alerts for Velocity 3.5.1",authors={"Brianni_MC"},dependencies={@Dependency(id="packetevents")})
public final class AfriTiersVelocity {
    static final MinecraftChannelIdentifier HELLO=MinecraftChannelIdentifier.create("afritiers","hello");
    static final MinecraftChannelIdentifier QUEUE=MinecraftChannelIdentifier.create("afritiers","queue");
    static final UUID PACK_ID=UUID.fromString("4315731a-9a72-4e2b-a9a9-17cc804fc712");
    private static final URI PLAYERS=URI.create("https://afritiers.co.za/api/players"),QUEUES=URI.create("https://afritiers.co.za/api/queues");
    final ProxyServer proxy;
    final Logger logger;
    private final Path directory;
    final Rankings rankings=new Rankings();
    Preferences.Store preferences;
    final Set<UUID> modded=ConcurrentHashMap.newKeySet(),packLoaded=ConcurrentHashMap.newKeySet();
    final Map<UUID,Session> sessions=new ConcurrentHashMap<>();
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final AtomicBoolean refreshing=new AtomicBoolean(),polling=new AtomicBoolean();
    private QueueTracker tracker=new QueueTracker();
    private volatile List<QueueFeed.Entry> queues=List.of();
    private volatile boolean running;
    private volatile String packUrl="",packSha1="";
    TagRenderer tags;
    private SettingsCommand settings;
    record Notice(Component title,Component subtitle,boolean ticket){}
    static final class Session {
        final Set<String> seen=Collections.synchronizedSet(new LinkedHashSet<>());
        final Deque<Notice> notices=new ConcurrentLinkedDeque<>();
        volatile long nextDisplay;
        volatile int notes;
        volatile long nextNote;
    }
    @Inject public AfriTiersVelocity(ProxyServer proxy,Logger logger,@DataDirectory Path directory){this.proxy=proxy;this.logger=logger;this.directory=directory;}
    @Subscribe public void initialize(ProxyInitializeEvent event)throws Exception{
        Files.createDirectories(directory);
        loadConfig();preferences=new Preferences.Store(directory);
        settings=new SettingsCommand(this);
        register("afritiers-settings",settings);
        register("afritiers",new SimpleCommand(){
            public void execute(Invocation call){search(call.source(),String.join(" ",call.arguments()));}
            public List<String> suggest(Invocation call){return rankings.all().stream().map(Rankings.Profile::name).filter(n->n.toLowerCase(Locale.ROOT).startsWith(String.join(" ",call.arguments()).toLowerCase(Locale.ROOT))).limit(50).toList();}
        });
        register("join-queue",new SimpleCommand(){
            public void execute(Invocation call){if(call.source() instanceof Player player)join(player,call.arguments());else call.source().sendMessage(Component.text("Run this command in-game.",RED));}
            public List<String> suggest(Invocation call){String prefix=call.arguments().length==0?"":call.arguments()[0];return openModes(prefix);}
        });
        register("afritiersreload",new SimpleCommand(){
            public void execute(Invocation call){if(!call.source().hasPermission("afritiers.reload")){call.source().sendMessage(Component.text("You do not have permission.",RED));return;}try{loadConfig();refreshRankings();call.source().sendMessage(Component.text("Afri-Tiers reloaded.",GREEN));}catch(Exception error){call.source().sendMessage(Component.text("Invalid config.json; check the proxy console.",RED));}}
        });
        proxy.getChannelRegistrar().register(HELLO,QUEUE);
        tags=new TagRenderer(this);PacketEvents.getAPI().getEventManager().registerListener(tags);
        running=true;refreshRankings();
        proxy.getScheduler().buildTask(this,this::refreshRankings).repeat(Duration.ofMinutes(2)).schedule();
        proxy.getScheduler().buildTask(this,this::pollQueues).repeat(Duration.ofSeconds(5)).schedule();
        proxy.getScheduler().buildTask(this,this::tickAlerts).repeat(Duration.ofMillis(100)).schedule();
        proxy.getScheduler().buildTask(this,()->{for(Player p:proxy.getAllPlayers())tags.refresh(p);}).repeat(Duration.ofSeconds(1)).schedule();
        logger.info("Afri-Tiers enabled for Velocity 3.5.1. Settings: /afritiers-settings. Queue joining/tickets use AfriTiersLink on each backend.");
    }
    private void register(String name,SimpleCommand command){proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder(name).plugin(this).build(),command);}
    private void loadConfig()throws Exception{
        Path file=directory.resolve("config.json");
        if(!Files.exists(file))Files.writeString(file,"{\n  \"resourcePackUrl\": \"\",\n  \"resourcePackSha1\": \"\"\n}\n");
        JsonObject config=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        String url=config.get("resourcePackUrl").getAsString(),hash=config.get("resourcePackSha1").getAsString();
        if(!url.isEmpty()){
            URI uri=URI.create(url);
            if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||!hash.matches("[a-fA-F0-9]{40}"))throw new IllegalArgumentException("Use an HTTPS resource-pack URL and SHA-1.");
        }
        packUrl=url;packSha1=hash;
    }
    private CompletableFuture<String> get(URI uri,int limit){return http.sendAsync(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).header("User-Agent","AfriTiersVelocity/1.0.1").GET().build(),PublicHttp.limitedUtf8(limit)).thenApply(response->{if(response.statusCode()!=200)throw new IllegalStateException("HTTP "+response.statusCode());return response.body();});}
    void refreshRankings(){if(!refreshing.compareAndSet(false,true))return;get(PLAYERS,8_000_000).thenAccept(rankings::update).whenComplete((result,error)->{refreshing.set(false);if(error!=null)logger.warn("Public rankings refresh failed; keeping the previous cache.");});}
    private void pollQueues(){
        if(!running||!polling.compareAndSet(false,true))return;
        get(QUEUES,65_536).thenAccept(raw->{QueueFeed feed=QueueFeed.parse(raw);List<QueueFeed.Entry> opened; synchronized(this){opened=tracker.accept(feed,Instant.now());queues=feed.queues();}
            for(var queue:opened)for(Player player:proxy.getAllPlayers()){
                var s=sessions.get(player.getUniqueId());if(s==null||modded.contains(player.getUniqueId()))continue;
                var pref=prefs(player);if(!pref.waitlistAlerts)continue;
                if(s.notices.size()<16)s.notices.addLast(new Notice(Component.text(queue.mode().toUpperCase(Locale.ROOT)+" queue has just opened!",RED),Component.text("Do /join-queue "+queue.mode()+" to join the waitlist!",WHITE),false));
            }
        }).whenComplete((result,error)->polling.set(false));
    }
    List<String> openModes(String prefix){return QueueFeed.openModes(queues,prefix);}
    void join(Player player,String[] args){
        if(args.length!=1||!Rankings.MODES.contains(Rankings.mode(args[0]))||Rankings.mode(args[0]).equals("overall")){player.sendMessage(Component.text("Use /join-queue <gamemode>.",YELLOW));return;}
        if(player.getCurrentServer().isEmpty()){player.sendMessage(Component.text("Connect to a game server first.",RED));return;}
        player.spoofChatInput("/afritierslink:join-queue "+Rankings.mode(args[0]));
    }
    Preferences prefs(Player player){return preferences.get(player.getUniqueId());}
    boolean current(Player player,Session session){return running&&player.isActive()&&sessions.get(player.getUniqueId())==session;}
    @Subscribe public void connected(ServerConnectedEvent event){
        Player player=event.getPlayer();Session session=new Session();sessions.put(player.getUniqueId(),session);
        if(tags!=null)tags.reset(player.getUniqueId());
        proxy.getScheduler().buildTask(this,()->{
            if(!current(player,session))return;
            player.getCurrentServer().filter(connection->connection.getServer().equals(event.getServer())).ifPresent(connection->connection.sendPluginMessage(HELLO,encode("velocity-1.0.1")));
            if(!packUrl.isEmpty()&&!modded.contains(player.getUniqueId()))player.sendResourcePackOffer(proxy.createResourcePackBuilder(packUrl).setId(PACK_ID).setHash(HexFormat.of().parseHex(packSha1)).setShouldForce(false).setPrompt(Component.text("Afri-Tiers gamemode and trophy icons")).build());
        }).delay(Duration.ofSeconds(2)).schedule();
    }
    @Subscribe public void disconnected(DisconnectEvent event){UUID id=event.getPlayer().getUniqueId();sessions.remove(id);modded.remove(id);packLoaded.remove(id);preferences.remove(id);tags.reset(id);}
    @Subscribe public void packStatus(PlayerResourcePackStatusEvent event){if(PACK_ID.equals(event.getPackId())){if(event.getStatus()==PlayerResourcePackStatusEvent.Status.SUCCESSFUL)packLoaded.add(event.getPlayer().getUniqueId());else packLoaded.remove(event.getPlayer().getUniqueId());tags.refresh(event.getPlayer());}}
    @Subscribe public void message(PluginMessageEvent event){
        if(event.getIdentifier().equals(HELLO)){
            if(event.getSource() instanceof Player player&&event.getData().length<=32){modded.add(player.getUniqueId());tags.refresh(player);}
            else event.setResult(PluginMessageEvent.ForwardResult.handled());
            return;
        }
        if(!event.getIdentifier().equals(QUEUE))return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if(!(event.getSource() instanceof ServerConnection connection)||!(event.getTarget() instanceof Player player)||player.getCurrentServer().orElse(null)!=connection)return;
        Session session=sessions.get(player.getUniqueId());if(session==null||event.getData().length>16_010)return;
        if(modded.contains(player.getUniqueId())){player.sendPluginMessage(QUEUE,event.getData());return;}
        try{
            JsonObject payload=JsonParser.parseString(decode(event.getData())).getAsJsonObject();
            if(!"tickets".equals(payload.get("type").getAsString()))return;
            JsonArray tickets=payload.getAsJsonArray("tickets");if(tickets.size()>32)return;
            for(var item:tickets){var ticket=item.getAsJsonObject();String id=ticket.get("eventId").getAsString(),name=ticket.get("username").getAsString(),mode=ticket.get("mode").getAsString();
                if(!id.matches("[a-f0-9]{64}")||!name.equalsIgnoreCase(player.getUsername())||!Rankings.MODES.contains(mode)||!session.seen.add(id))continue;
                if(prefs(player).ticketAlerts&&session.notices.size()<16)session.notices.addFirst(new Notice(Component.text(player.getUsername(),GREEN),Component.text("Your Testing Ticket for "+mode.toUpperCase(Locale.ROOT)+" is open",WHITE),true));
            }
            synchronized(session.seen){while(session.seen.size()>256){var first=session.seen.iterator();first.next();first.remove();}}
        }catch(Exception ignored){}
    }
    private void tickAlerts(){
        long now=System.nanoTime();
        for(Player player:proxy.getAllPlayers()){
            Session s=sessions.get(player.getUniqueId());if(s==null||!current(player,s)||modded.contains(player.getUniqueId()))continue;Preferences p=prefs(player);
            if(!p.waitlistAlerts||!p.waitlistSound)s.notes=0;
            if(s.notes>0&&now>=s.nextNote){sound(player);s.notes--;s.nextNote=now+Duration.ofMillis(100).toNanos();}
            if(now<s.nextDisplay)continue;
            Notice notice=s.notices.poll();if(notice==null)continue;
            if(notice.ticket()?!p.ticketAlerts:!p.waitlistAlerts)continue;
            String location=notice.ticket()?p.ticketLocation:p.waitlistLocation;
            if(location.equals("chat"))player.sendMessage(notice.title().append(Component.space()).append(notice.subtitle()));
            else player.showTitle(Title.title(notice.title(),notice.subtitle(),Title.Times.times(Duration.ZERO,Duration.ofSeconds(3),Duration.ZERO)));
            s.nextDisplay=now+Duration.ofSeconds(3).toNanos();
            if(notice.ticket()){s.notes=0;if(p.ticketSound)sound(player);}else if(p.waitlistSound){sound(player);s.notes=29;s.nextNote=now+Duration.ofMillis(100).toNanos();}
        }
    }
    private void sound(Player player){player.playSound(Sound.sound(Key.key("minecraft:block.note_block.pling"),Sound.Source.MASTER,1,1),Sound.Emitter.self());}
    void search(com.velocitypowered.api.command.CommandSource source,String name){
        if(!name.matches("[A-Za-z0-9_]{1,16}")){source.sendMessage(Component.text("Use /afritiers <Minecraft username>.",YELLOW));return;}
        var profile=rankings.find(null,name);if(profile.isEmpty()){source.sendMessage(Component.text("No Afri-Tiers profile found for "+name+".",GRAY));return;}
        var p=profile.get();source.sendMessage(Component.text(p.name()+" — Overall #"+p.position()+" — "+p.points()+" points — "+p.region(),AQUA));
        for(var tier:p.tiers())source.sendMessage(Component.text(tier.mode()+": "+(tier.retired()?"R":"")+tier.level(),WHITE));
    }
    static byte[] encode(String text){byte[] bytes=text.getBytes(StandardCharsets.UTF_8);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();int length=bytes.length;while((length&~127)!=0){out.write((length&127)|128);length>>>=7;}out.write(length);out.writeBytes(bytes);return out.toByteArray();}
    static String decode(byte[] bytes){int length=0,index=0,shift=0;while(true){if(index>=bytes.length||shift>21)throw new IllegalArgumentException();int b=bytes[index++]&255;length|=(b&127)<<shift;if((b&128)==0)break;shift+=7;}if(length>16_000||length!=bytes.length-index)throw new IllegalArgumentException();return new String(bytes,index,length,StandardCharsets.UTF_8);}
    @Subscribe public void shutdown(ProxyShutdownEvent event){running=false;proxy.getScheduler().tasksByPlugin(this).forEach(task->task.cancel());if(tags!=null){for(Player player:proxy.getAllPlayers())tags.restore(player);PacketEvents.getAPI().getEventManager().unregisterListener(tags);}sessions.clear();}
}
