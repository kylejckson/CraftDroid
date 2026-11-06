package com.mineagent.engine.vision;

import com.mineagent.engine.MineAgentEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Incremental server-thread reads; bounded worker queue performs only snapshot rendering and PNG IO. */
public final class SceneCapture {
    private static final ArrayDeque<Job> QUEUE=new ArrayDeque<>();
    private static final Map<UUID,Long> LAST=new HashMap<>();
    private static final ThreadPoolExecutor RENDER=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(2),r->{var t=new Thread(r,"CraftDroid-software-vision");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static volatile long generation;
    private static final class Job {
        final ServerPlayer actor;final long tick,epoch;final int size=24,minX,minY,minZ;final int[] colors=new int[size*size*size];
        final double eyeX,eyeY,eyeZ;final float yaw,pitch;final boolean rain;final int sky;int cursor;
        final net.minecraft.server.level.ServerLevel level;
        Job(ServerPlayer p){actor=p;level=p.serverLevel();tick=level.getGameTime();epoch=generation;minX=p.blockPosition().getX()-12;minY=p.blockPosition().getY()-8;minZ=p.blockPosition().getZ()-12;var eye=p.getEyePosition();eyeX=eye.x-minX;eyeY=eye.y-minY;eyeZ=eye.z-minZ;yaw=p.getYRot();pitch=p.getXRot();rain=level.isRaining();sky=level.isDay()?(rain?0x718593:0x8cb9e8):0x18253b;}
    }
    private SceneCapture() {}
    public static void reset(){generation++;QUEUE.clear();LAST.clear();RENDER.getQueue().clear();}
    public static String request(ServerPlayer actor) {
        UUID id=actor.getUUID();long tick=actor.level().getGameTime();
        if(QUEUE.size()>=2||RENDER.getQueue().remainingCapacity()==0)throw new IllegalArgumentException("Vision queue busy; continue other work");
        if(LAST.containsKey(id)&&tick>=LAST.get(id)&&tick-LAST.get(id)<600)throw new IllegalArgumentException("Vision cooldown: 30 seconds");
        QUEUE.add(new Job(actor));LAST.put(id,tick);return "queued";
    }
    public static void tick() {
        Job j=QUEUE.peek();if(j==null)return;
        if(j.epoch!=generation||j.actor.isRemoved()||j.actor.serverLevel()!=j.level){QUEUE.remove();return;}
        long deadline=System.nanoTime()+1_000_000;int reads=0;
        while(j.cursor<j.colors.length&&reads++<1024&&System.nanoTime()<deadline) {
            int i=j.cursor++,x=i%j.size,z=(i/j.size)%j.size,y=i/(j.size*j.size);
            var pos=new BlockPos(j.minX+x,j.minY+y,j.minZ+z);
            if(!j.level.isInWorldBounds(pos)||j.level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null){j.colors[i]=0xff554b55;continue;}
            var state=j.level.getBlockState(pos);
            if(!state.isAir()){int rgb=state.getMapColor(j.level,pos).col;j.colors[i]=0xff000000|(rgb==0?0x777777:rgb);}
        }
        if(j.cursor<j.colors.length)return;
        QUEUE.remove();
        VoxelScene snapshot=new VoxelScene(j.size,j.colors,j.eyeX,j.eyeY,j.eyeZ,j.yaw,j.pitch,j.sky,j.rain);
        Path dir=MineAgentEngine.getWorldDataDir();
        try {RENDER.execute(()->{
            try {
                long start=System.nanoTime();byte[] png=SoftwareSceneRenderer.render(snapshot,192,108);long millis=(System.nanoTime()-start)/1_000_000;
                if(dir!=null&&j.epoch==generation){Path file=dir.resolve("craftdroid/scenes/"+j.actor.getUUID()+".png");Files.createDirectories(file.getParent());Files.write(file,png);}
                j.actor.getServer().execute(()->{
                    if(j.epoch!=generation||j.actor.isRemoved())return;
                    MineAgentEngine.getCompanion(j.actor.getUUID()).ifPresent(s->s.loop.observeScene(png,
                            "Software-reconstructed agent view at tick "+j.tick+", yaw "+j.yaw+", pitch "+j.pitch+". Simplified full-block shapes/material colors, not client textures; unknown chunks are gray. Render took "+millis+" ms. Use inspect_sign for exact readable sign text."));
                });
            }catch(Exception e){System.err.println("[CraftDroid] Scene capture failed: "+e.getMessage());}
        });}catch(RejectedExecutionException e){System.err.println("[CraftDroid] Vision queue full; capture discarded");}
    }
}
