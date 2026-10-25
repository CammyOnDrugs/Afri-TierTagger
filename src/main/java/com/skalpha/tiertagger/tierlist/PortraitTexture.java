package com.skalpha.tiertagger.tierlist;

import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.model.PortraitSource;
import com.skalpha.tiertagger.net.PublicHttp;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

final class PortraitTexture implements AutoCloseable {
    private record Cached(byte[] bytes,long expires){}
    private static final java.util.Map<String,Cached> CACHE=new java.util.LinkedHashMap<>(16,0.75f,true);
    private static synchronized byte[] cached(String source){Cached value=CACHE.get(source);if(value!=null&&value.expires()>System.nanoTime())return value.bytes();CACHE.remove(source);return null;}
    private static synchronized void remember(String source,byte[] bytes){while(CACHE.size()>=16)CACHE.remove(CACHE.keySet().iterator().next());CACHE.put(source,new Cached(bytes,System.nanoTime()+Duration.ofMinutes(5).toNanos()));}
    private final Identifier id=Identifier.of("tiertagger","portrait/"+UUID.randomUUID().toString().replace("-",""));
    private CompletableFuture<byte[]> future;
    private boolean closed,loaded;
    private int imageWidth=256,imageHeight=256;
    private String error="Loading portrait...";
    PortraitTexture(String source){load(PortraitSource.allowed(source)?source:PortraitSource.FALLBACK,true);}
    private void load(String source,boolean fallback){
        byte[] existing=cached(source);
        future=existing!=null?CompletableFuture.completedFuture(existing):TierTagger.getClient().sendAsync(HttpRequest.newBuilder(URI.create(source)).timeout(Duration.ofSeconds(15)).GET().build(),PublicHttp.limitedBytes(1048576)).thenApply(response->{if(response.statusCode()!=200)throw new IllegalStateException();return response.body();});
        future.whenComplete((bytes,failure)->MinecraftClient.getInstance().execute(()->{
            if(closed)return;
            if(failure!=null){if(fallback&&!source.equals(PortraitSource.FALLBACK))load(PortraitSource.FALLBACK,false);else error="Portrait unavailable";return;}
            try{
                if(bytes.length<24||java.nio.ByteBuffer.wrap(bytes).getLong()!=0x89504E470D0A1A0AL||java.nio.ByteBuffer.wrap(bytes,16,4).getInt()<1||java.nio.ByteBuffer.wrap(bytes,20,4).getInt()<1||java.nio.ByteBuffer.wrap(bytes,16,4).getInt()>1024||java.nio.ByteBuffer.wrap(bytes,20,4).getInt()>1024)throw new IllegalArgumentException();
                NativeImage image=NativeImage.read(bytes);
                if(image.getWidth()<1||image.getHeight()<1||image.getWidth()>1024||image.getHeight()>1024){image.close();throw new IllegalArgumentException();}
                imageWidth=image.getWidth();imageHeight=image.getHeight();remember(source,bytes);
                MinecraftClient.getInstance().getTextureManager().registerTexture(id,new NativeImageBackedTexture(()->"Afri-Tiers website portrait",image));loaded=true;
            }catch(Exception exception){if(fallback&&!source.equals(PortraitSource.FALLBACK))load(PortraitSource.FALLBACK,false);else error="Portrait unavailable";}
        }));
    }
    void draw(DrawContext graphics,int x,int y,int width,int height){
        if(!loaded){graphics.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,error,x+width/2,y+height/2,0xFFAAAAAA);return;}
        double scale=Math.min((double)width/imageWidth,(double)height/imageHeight);int w=(int)(imageWidth*scale),h=(int)(imageHeight*scale);
        graphics.drawTexture(RenderPipelines.GUI_TEXTURED,id,x+(width-w)/2,y+(height-h)/2,0,0,w,h,imageWidth,imageHeight,imageWidth,imageHeight);
    }
    void thumbnail(DrawContext graphics,int x,int y,int width,int height){if(loaded)draw(graphics,x,y,width,height);}
    public void close(){closed=true;if(future!=null)future.cancel(true);if(loaded)MinecraftClient.getInstance().getTextureManager().destroyTexture(id);}
}
