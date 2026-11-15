package com.skalpha.tiertagger.config;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
public final class UpdateInstaller {
    private UpdateInstaller(){}
    public static void main(String[] args)throws Exception{
        long parent=Long.parseLong(args[0]),deadline=System.nanoTime()+java.time.Duration.ofDays(1).toNanos();
        while(ProcessHandle.of(parent).map(ProcessHandle::isAlive).orElse(false)){if(System.nanoTime()>deadline)return;Thread.sleep(1000);}
        Path target=Path.of(args[1]),pending=Path.of(args[2]),report=pending.resolveSibling("install-result.txt");
        try{install(target,pending,args[3],args[4]);Files.writeString(report,"Update installed. The previous JAR was kept as a backup.");}
        catch(Exception error){Files.writeString(report,"Update not installed: "+error.getMessage());}
    }
    static void install(Path target,Path pending,String oldHash,String newHash)throws Exception{
        target=target.toRealPath();pending=pending.toRealPath();
        boolean inMods=false;for(Path part:target)if(part.toString().equalsIgnoreCase("mods"))inMods=true;
        if(!inMods||!target.getFileName().toString().endsWith(".jar")||!pending.getFileName().toString().endsWith(".pending")||Files.isSameFile(target,pending))throw new IllegalArgumentException("Invalid update paths");
        if(!hash(target).equals(oldHash)||!hash(pending).equals(newHash))throw new IllegalArgumentException("A file changed after verification");
        Path backup=target.resolveSibling(target.getFileName()+"."+System.currentTimeMillis()+".afritiers-backup");
        move(target,backup);try{move(pending,target);}catch(Exception error){move(backup,target);throw error;}
    }
    private static void move(Path from,Path to)throws Exception{try{Files.move(from,to,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException error){Files.move(from,to);}}
    static String hash(Path path)throws Exception{var digest=MessageDigest.getInstance("SHA-256");try(var input=Files.newInputStream(path)){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)digest.update(buffer,0,count);}return HexFormat.of().formatHex(digest.digest());}
}
