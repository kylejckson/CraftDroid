package com.mineagent.engine.skin;

import com.google.gson.JsonObject;
import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.network.MineAgentNetwork;
import net.minecraft.server.level.ServerPlayer;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Server validates and caches PNGs; CraftDroid clients receive bounded texture chunks, with no third-party skin service. */
public final class ResidentSkins {
    private static final Map<UUID,byte[]> CACHE=new HashMap<>();
    private static final Map<UUID,Long> VERSIONS=new HashMap<>();
    private static final ThreadPoolExecutor WORK=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(32),r->{Thread t=new Thread(r,"CraftDroid-skins");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static final HttpClient HTTP=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(8)).build();
    private static long epoch;
    private ResidentSkins() {}
    public static void reset(){epoch++;CACHE.clear();VERSIONS.clear();WORK.getQueue().clear();}
    public static void forget(UUID id){
        CACHE.remove(id);VERSIONS.merge(id,1L,Long::sum);
        MineAgentEngine.getCompanion(id).ifPresent(s->s.companion.serverPlayer().getServer().getPlayerList().getPlayers().stream()
                .filter(p->!MineAgentEngine.isCompanionPlayer(p.getUUID())).forEach(p->MineAgentNetwork.sendUiActionTo(p,id,"resident_skin_remove","")));
    }
    public static void prepare(ServerPlayer actor,String url) {
        UUID id=actor.getUUID();long world=epoch,version=VERSIONS.merge(id,1L,Long::sum);var server=actor.getServer();Path file=MineAgentEngine.getWorldDataDir().resolve("craftdroid/skins/"+id+".png");
        try{WORK.execute(()->{
            try {
                byte[] png=(url!=null&&url.startsWith("https://"))?download(URI.create(url)):
                        Files.exists(file)?SkinPng.canonical(Files.readAllBytes(file)):SkinPng.generated(id);
                if(world!=epoch)return;
                // Disk/cache publication is version-checked on the server thread.
                server.execute(()->{
                    if(world!=epoch||actor.isRemoved()||!Objects.equals(VERSIONS.get(id),version))return;
                    CACHE.put(id,png);
                    for(var viewer:server.getPlayerList().getPlayers())if(!MineAgentEngine.isCompanionPlayer(viewer.getUUID()))send(viewer,id,png);
                    try{Files.createDirectories(file.getParent());Files.write(file,png);}catch(Exception e){System.err.println("[CraftDroid] Skin cache write failed: "+e.getMessage());}
                });
            }catch(Exception e){server.execute(()->{if(world==epoch&&!actor.isRemoved())actor.getServer().getPlayerList().getPlayers().stream().filter(p->p.hasPermissions(2)&&!MineAgentEngine.isCompanionPlayer(p.getUUID())).forEach(p->p.sendSystemMessage(net.minecraft.network.chat.Component.literal("[CraftDroid] Skin update failed: "+e.getMessage())));});}
        });}catch(RejectedExecutionException e){throw new IllegalArgumentException("Skin queue full; retry later");}
    }
    public static void sync(ServerPlayer viewer){CACHE.forEach((id,png)->send(viewer,id,png));}
    private static void send(ServerPlayer viewer,UUID id,byte[] png) {
        String base64=Base64.getEncoder().encodeToString(png);int count=(base64.length()+2999)/3000;
        for(int i=0;i<count;i++) {
            JsonObject part=new JsonObject();part.addProperty("part",i);part.addProperty("count",count);part.addProperty("version",VERSIONS.getOrDefault(id,0L));part.addProperty("data",base64.substring(i*3000,Math.min(base64.length(),(i+1)*3000)));
            MineAgentNetwork.sendUiActionTo(viewer,id,"resident_skin_part",part.toString());
        }
    }
    public static URI validatePublicUrl(URI uri) throws java.io.IOException {
        if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getRawUserInfo()!=null||uri.getFragment()!=null
                ||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.toString().length()>2048)throw new java.io.IOException("Use a public HTTPS PNG URL on port 443");
        for(InetAddress address:InetAddress.getAllByName(uri.getHost()))if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||address.isMulticastAddress()||(address.getAddress().length==16&&(address.getAddress()[0]&0xfe)==0xfc))throw new java.io.IOException("Private/internal skin destinations are forbidden");
        return uri;
    }
    private static byte[] download(URI uri) throws Exception {
        for(int redirects=0;redirects<4;redirects++) {
            validatePublicUrl(uri);
            var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).GET().build();
            var response=HTTP.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try(var input=response.body()) {
                if(response.statusCode()>=300&&response.statusCode()<400){uri=uri.resolve(response.headers().firstValue("location").orElseThrow());continue;}
                if(response.statusCode()!=200)throw new java.io.IOException("PNG host returned "+response.statusCode());
                byte[] bytes=input.readNBytes(131073);return SkinPng.canonical(bytes);
            }
        }
        throw new java.io.IOException("Too many skin redirects");
    }
}
