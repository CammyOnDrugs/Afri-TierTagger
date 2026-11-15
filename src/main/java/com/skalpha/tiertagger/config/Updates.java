package com.skalpha.tiertagger.config;
import com.google.gson.*;
import com.skalpha.tiertagger.net.网络;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.*;
import net.fabricmc.loader.api.FabricLoader;
public final class Updates {
    record Candidate(String version,String url,String algorithm,String hash){}
    private static final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final AtomicBoolean busy=new AtomicBoolean();
    private static volatile Candidate latest;
    private static volatile String status="Updates have not been checked";
    private static boolean started;
    private Updates(){}
    public static String status(){return status;}
    public static synchronized void start(){if(!started){started=true;check();}}
    private static String version(String id){return FabricLoader.getInstance().getModContainer(id).orElseThrow().getMetadata().getVersion().getFriendlyString();}
    public static void check(){
        if(!busy.compareAndSet(false,true))return;status="Checking Modrinth and GitHub…";
        CompletableFuture.runAsync(()->{try{
            String minecraft=version("minecraft"),current=version("afri-tierstagger-tester");Candidate found=null;int available=0;
            try{var rows=getJson("https://api.modrinth.com/v2/project/afri-tiertagger/version?loaders=%5B%22fabric%22%5D&game_versions="+URLEncoder.encode("[\""+minecraft+"\"]",java.nio.charset.StandardCharsets.UTF_8)+"&include_changelog=false").getAsJsonArray();available++;
                for(var element:rows){var row=element.getAsJsonObject();if(!"release".equals(row.get("version_type").getAsString()))continue;for(var fileElement:row.getAsJsonArray("files")){var file=fileElement.getAsJsonObject();if(!file.get("filename").getAsString().endsWith(".jar"))continue;var candidate=new Candidate(row.get("version_number").getAsString(),file.get("url").getAsString(),"SHA-512",file.getAsJsonObject("hashes").get("sha512").getAsString());if(valid(candidate)&&newer(candidate.version(),current)&&(found==null||newer(candidate.version(),found.version())))found=candidate;}}
            }catch(Exception ignored){}
            try{var row=getJson("https://api.github.com/repos/CammyOnDrugs/Afri-TierTagger/releases/latest").getAsJsonObject();available++;Candidate github=selectGitHub(row,minecraft,current);if(github!=null&&(found==null||newer(github.version(),found.version())))found=github;}catch(Exception ignored){}
            latest=found;status=found!=null?"Update available: "+found.version():available>0?"You have the latest compatible release":"Update check unavailable — try again later";
        }catch(Exception error){status="Could not check updates";}finally{busy.set(false);}});
    }
    static Candidate selectGitHub(JsonObject release,String minecraft,String current){
        if(release.get("draft").getAsBoolean()||release.get("prerelease").getAsBoolean())return null;
        String version=release.get("tag_name").getAsString().replaceFirst("^v","");if(!newer(version,current))return null;
        for(var element:release.getAsJsonArray("assets")){var asset=element.getAsJsonObject();if(!asset.get("name").getAsString().equals("Afri-TiersTagger-"+version+"+mc"+minecraft+".jar")||!asset.has("digest")||asset.get("digest").isJsonNull())continue;String digest=asset.get("digest").getAsString();if(!digest.startsWith("sha256:"))continue;var candidate=new Candidate(version,asset.get("browser_download_url").getAsString(),"SHA-256",digest.substring(7));if(valid(candidate))return candidate;}return null;
    }
    static boolean newer(String candidate,String current){try{String[] a=candidate.replaceFirst("^v","").split("\\+")[0].split("\\."),b=current.replaceFirst("^v","").split("\\+")[0].split("\\.");if(a.length!=3||b.length!=3)return false;for(int i=0;i<3;i++){int difference=Integer.compare(Integer.parseInt(a[i]),Integer.parseInt(b[i]));if(difference!=0)return difference>0;}return false;}catch(RuntimeException error){return false;}}
    private static boolean valid(Candidate candidate){return (candidate.url().startsWith("https://cdn.modrinth.com/data/")||candidate.url().startsWith("https://github.com/CammyOnDrugs/Afri-TierTagger/releases/download/"))&&candidate.hash().matches(candidate.algorithm().equals("SHA-512")?"[a-fA-F0-9]{128}":"[a-fA-F0-9]{64}");}
    private static JsonElement getJson(String url)throws Exception{var response=client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).header("User-Agent","Afri-TiersTagger-Updater/2.8.5").GET().build(),网络.limitedUtf8(1_000_000));if(response.statusCode()!=200)throw new java.io.IOException("Update source unavailable");return JsonParser.parseString(response.body());}
    static void verify(byte[] bytes,Candidate candidate)throws Exception{String actual=HexFormat.of().formatHex(MessageDigest.getInstance(candidate.algorithm()).digest(bytes));if(!actual.equalsIgnoreCase(candidate.hash()))throw new java.io.IOException("Download checksum did not match");}
    public static void install(){
        Candidate candidate=latest;if(candidate==null){check();return;}if(!busy.compareAndSet(false,true))return;
        if(FabricLoader.getInstance().isDevelopmentEnvironment()){status="Updates can be installed in a normal Minecraft launch";busy.set(false);return;}
        status="Downloading and verifying update…";
        CompletableFuture.runAsync(()->{try{
            Path target=FabricLoader.getInstance().getModContainer("afri-tierstagger-tester").orElseThrow().getOrigin().getPaths().getFirst().toRealPath();if(!Files.isRegularFile(target)||!target.toString().endsWith(".jar"))throw new java.io.IOException("Installed JAR could not be located");String originalHash=UpdateInstaller.hash(target);
            var response=client.send(HttpRequest.newBuilder(URI.create(candidate.url())).timeout(Duration.ofSeconds(60)).GET().build(),网络.limitedBytes(32_000_000));if(response.statusCode()!=200)throw new java.io.IOException("Download unavailable");byte[] bytes=response.body();verify(bytes,candidate);
            Path directory=FabricLoader.getInstance().getConfigDir().resolve("afritiers-updates");Files.createDirectories(directory);Path pending=Files.createTempFile(directory,"update-",".pending");Files.write(pending,bytes);
            try(var jar=new JarFile(pending.toFile())){var entry=jar.getJarEntry("fabric.mod.json");if(entry==null)throw new java.io.IOException("Not a Fabric mod");byte[] metadata=jar.getInputStream(entry).readNBytes(65537);if(metadata.length>65536)throw new java.io.IOException("Invalid mod metadata");var mod=JsonParser.parseString(new String(metadata,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();if(!"afri-tierstagger-tester".equals(mod.get("id").getAsString())||!newer(mod.get("version").getAsString(),version("afri-tierstagger-tester")))throw new java.io.IOException("Unexpected mod or version");}
            Path helper=directory.resolve("installer.jar");String className="com/skalpha/tiertagger/config/UpdateInstaller.class";
            try(var source=UpdateInstaller.class.getResourceAsStream("/"+className);var output=new JarOutputStream(Files.newOutputStream(helper))){if(source==null)throw new java.io.IOException("Installer missing");output.putNextEntry(new JarEntry(className));source.transferTo(output);output.closeEntry();}
            String java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"javaw.exe":"java").toString();
            new ProcessBuilder(java,"-cp",helper.toString(),UpdateInstaller.class.getName(),Long.toString(ProcessHandle.current().pid()),target.toString(),pending.toString(),originalHash,UpdateInstaller.hash(pending)).redirectErrorStream(true).redirectOutput(directory.resolve("installer.log").toFile()).start();status="Update ready — it will install after Minecraft closes";latest=null;
        }catch(Exception error){status="Update not installed: "+error.getMessage();}finally{busy.set(false);}});
    }
}
