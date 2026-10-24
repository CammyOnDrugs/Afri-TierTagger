package net.afritiers.purpur;
import com.google.gson.*;
import com.skalpha.tiertagger.net.PublicHttp;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
final class TicketService implements AutoCloseable {
    private record Settings(HttpClient client,URI endpoint,String server,String secret,boolean trusted,boolean unverified){}
    private record Target(Player player,AfriTiersPurpur.Session session){}
    private final AfriTiersPurpur plugin;
    private final AtomicBoolean polling=new AtomicBoolean();
    private volatile Settings settings;
    private volatile boolean closed;
    TicketService(AfriTiersPurpur plugin){this.plugin=plugin;reload();}
    void reload(){settings=null;var link=Bukkit.getPluginManager().getPlugin("AfriTiersLink");if(!(link instanceof JavaPlugin backend)||!backend.isEnabled())return;
        try{
            var config=YamlConfiguration.loadConfiguration(new java.io.File(backend.getDataFolder(),"config.yml"));String base=config.getString("backend-url","").replaceAll("/+$","");URI url=URI.create(base+"/api/minecraft-queue");
            boolean local=Set.of("localhost","127.0.0.1","::1","[::1]").contains(String.valueOf(url.getHost()));
            if((!"https".equals(url.getScheme())&&!(local&&"http".equals(url.getScheme())))||url.getUserInfo()!=null||url.getQuery()!=null||url.getFragment()!=null||!url.getPath().equals("/api/minecraft-queue"))return;
            String server=config.getString("server-id",""),secret=config.getString("server-secret","");if(!Set.of("la-network","surgemc").contains(server)||secret.length()<32)return;
            String certificate=config.getString("backend-ca-file","");HttpClient client=BackendTls.createClient(certificate.isBlank()?null:backend.getDataFolder().toPath().resolve(certificate));
            settings=new Settings(client,url,server,secret,config.getBoolean("trusted-proxy-authentication",false),config.getBoolean("allow-unverified-server-session",false));
        }catch(Exception error){plugin.getLogger().warning("Ticket integration unavailable; check AfriTiersLink configuration and backend certificate.");}
    }
    void verifyBackend(){
        Settings active=settings;if(closed||active==null)return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            try{JsonObject body=new JsonObject();body.addProperty("action","status");body.add("players",new JsonArray());JsonObject response=request(active,body);if(!response.has("ok")||!response.get("ok").getAsBoolean())throw new IllegalStateException();if(!closed&&settings==active)plugin.getLogger().info("AfriTiersLink backend verified: TLS and signed queue status are working.");}
            catch(Exception error){if(!closed&&settings==active)plugin.getLogger().warning("AfriTiersLink backend verification failed: "+error.getClass().getSimpleName()+". Check backend settings, certificate and network access.");}
        });
    }
    boolean authenticated(Player player){
        if(!player.isOnline())return false;if(Bukkit.getOnlineMode())return true;
        if(Bukkit.getPluginManager().isPluginEnabled("AuthMe"))try{Class<?> api=Class.forName("fr.xephi.authme.api.v3.AuthMeApi");Object instance=api.getMethod("getInstance").invoke(null);return instance!=null&&Boolean.TRUE.equals(api.getMethod("isAuthenticated",Player.class).invoke(instance,player));}catch(ReflectiveOperationException error){return false;}
        Settings active=settings;return active!=null&&(active.trusted()||active.unverified());
    }
    void poll(){Settings active=settings;if(closed||active==null||!polling.compareAndSet(false,true))return;
        List<Target> targets=new ArrayList<>();for(Player player:Bukkit.getOnlinePlayers()){var session=plugin.sessions.get(player.getUniqueId());if(session!=null&&!plugin.modded.contains(player.getUniqueId())&&authenticated(player))targets.add(new Target(player,session));}
        if(targets.isEmpty()){polling.set(false);return;}
        List<JsonObject> batches=new ArrayList<>();for(int start=0;start<targets.size();start+=100){JsonArray players=new JsonArray();for(Target target:targets.subList(start,Math.min(start+100,targets.size()))){JsonObject identity=new JsonObject();identity.addProperty("uuid",target.player().getUniqueId().toString());identity.addProperty("username",target.player().getName());players.add(identity);}JsonObject body=new JsonObject();body.addProperty("action","status");body.add("players",players);batches.add(body);}
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            try{for(JsonObject body:batches){JsonObject response=request(active,body);if(!response.has("ok")||!response.get("ok").getAsBoolean())continue;
                for(var element:response.getAsJsonArray("players")){var row=element.getAsJsonObject();String uuid=row.get("uuid").getAsString();if(!uuid.matches("[a-f0-9]{32}"))continue;
                    Target target=targets.stream().filter(t->t.player().getUniqueId().toString().replace("-","").equals(uuid)).findFirst().orElse(null);if(target==null)continue;JsonArray tickets=row.getAsJsonArray("tickets");if(closed||settings!=active)return;
                    Bukkit.getScheduler().runTask(plugin,()->{if(!closed&&settings==active&&plugin.current(target.player(),target.session())&&authenticated(target.player()))plugin.tickets(target.player(),target.session(),tickets);});
                }
            }}catch(Exception ignored){}finally{polling.set(false);}
        });
    }
    static String sign(String secret,String stamp,String nonce,String payload)throws Exception{Mac hmac=Mac.getInstance("HmacSHA256");hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(hmac.doFinal((stamp+"\n"+nonce+"\nPOST\n/api/minecraft-queue\n"+payload).getBytes(StandardCharsets.UTF_8)));}
    private JsonObject request(Settings active,JsonObject body)throws Exception{
        String raw=body.toString(),stamp=Long.toString(System.currentTimeMillis()/1000),nonce=UUID.randomUUID().toString().replace("-","");
        HttpRequest request=HttpRequest.newBuilder(active.endpoint()).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("X-Afri-Server",active.server()).header("X-Afri-Timestamp",stamp).header("X-Afri-Nonce",nonce).header("X-Afri-Signature",sign(active.secret(),stamp,nonce,raw)).POST(HttpRequest.BodyPublishers.ofString(raw,StandardCharsets.UTF_8)).build();
        var response=active.client().send(request,PublicHttp.limitedUtf8(65_536));if(response.statusCode()!=200)throw new IllegalStateException();return JsonParser.parseString(response.body()).getAsJsonObject();
    }
    @Override public void close(){closed=true;settings=null;}
}
