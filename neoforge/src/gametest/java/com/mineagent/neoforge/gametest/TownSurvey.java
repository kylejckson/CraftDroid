package com.mineagent.neoforge.gametest;

import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.resident.Residents;
import com.mineagent.engine.vision.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/** Inspectable server-state imagery, never invented game screenshots or a human camera. */
final class TownSurvey {
    private static final ExecutorService RENDER=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"CraftDroid-town-survey");t.setDaemon(true);return t;});
    private static final int SIZE=32,HEIGHT=14;
    private final String[] initial=new String[SIZE*SIZE*HEIGHT];
    private final BlockPos origin;
    TownSurvey(ServerLevel level,BlockPos origin){this.origin=origin;read(level,true);}
    private record View(String name,double x,double z,int color){}
    private record Snapshot(int[] colors,int[] surface,int added,int removed,int changed,boolean rain,boolean day,long time,List<View> people){}
    private Snapshot read(ServerLevel level,boolean baseline){
        int[] colors=new int[32*32*32],surface=new int[32*32];int added=0,removed=0,changed=0;
        for(int y=0;y<HEIGHT;y++)for(int z=0;z<SIZE;z++)for(int x=0;x<SIZE;x++){
            var pos=origin.offset(x,y-1,z);var state=level.getBlockState(pos);String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();int index=(y*SIZE+z)*SIZE+x;
            if(baseline)initial[index]=id;
            else if(!Objects.equals(id,initial[index])){changed++;if(state.isAir())removed++;else if(initial[index].equals("minecraft:air"))added++;}
            if(!state.isAir()){int rgb=state.getMapColor(level,pos).col;colors[index]=0xff000000|(rgb==0?0x777777:rgb);surface[z*32+x]=rgb==0?0x777777:rgb;}
        }
        var people=new ArrayList<View>();int n=0;
        for(var p:Residents.profiles()){
            var state=MineAgentEngine.getCompanion(p.id()).orElse(null);if(state==null)continue;
            var body=state.companion.serverPlayer();people.add(new View(p.name(),body.getX()-origin.getX(),body.getZ()-origin.getZ(),Color.HSBtoRGB((n++*.31f)%1,.65f,.95f)));
        }
        return new Snapshot(colors,surface,added,removed,changed,level.isRaining(),level.isDay(),level.getDayTime(),List.copyOf(people));
    }
    Map<String,Object> capture(ServerLevel level,int seconds){
        var snapshot=read(level,false);var metadata=Map.<String,Object>of("elapsed_s",seconds,"new_nonair_cells",snapshot.added(),"removed_cells",snapshot.removed(),"changed_block_ids",snapshot.changed());
        RENDER.execute(()->{
            try {
                Path folder=Path.of("town-images");Files.createDirectories(folder);
                int sky=snapshot.day()?(snapshot.rain()?0x718593:0x8cb9e8):0x18253b;
                byte[] view=SoftwareSceneRenderer.render(new VoxelScene(32,snapshot.colors(),16.5,24,1.5,0,48,sky,snapshot.rain()),320,240);
                Files.write(folder.resolve(String.format("town-survey-%04d.png",seconds)),view);
                var map=new BufferedImage(640,640,BufferedImage.TYPE_INT_RGB);var g=map.createGraphics();
                try {
                    g.setColor(new Color(0x15202b));g.fillRect(0,0,640,640);g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,18));g.drawString("CraftDroid town | server-state map | "+seconds+"s",22,28);
                    g.setFont(new Font("SansSerif",Font.PLAIN,13));g.drawString("Block colors, no textures. Changes: +"+snapshot.added()+" / -"+snapshot.removed()+" | "+(snapshot.rain()?"rain":"dry")+" | "+(snapshot.day()?"day":"night"),22,49);
                    int left=64,top=70,cell=16;
                    for(int z=0;z<32;z++)for(int x=0;x<32;x++){g.setColor(new Color(snapshot.surface()[z*32+x]));g.fillRect(left+x*cell,top+z*cell,cell,cell);g.setColor(new Color(0,0,0,35));g.drawRect(left+x*cell,top+z*cell,cell,cell);}
                    g.setColor(new Color(230,90,90));g.setStroke(new BasicStroke(3));g.drawRect(left+26*cell,top+3*cell,5*cell,5*cell);
                    for(var person:snapshot.people()){
                        int x=left+(int)(person.x()*cell),z=top+(int)(person.z()*cell);g.setColor(Color.BLACK);g.fillOval(x-7,z-7,14,14);g.setColor(new Color(person.color()));g.fillOval(x-5,z-5,10,10);
                        g.setFont(new Font("SansSerif",Font.BOLD,13));g.setColor(Color.BLACK);g.drawString(person.name(),x+9,z+1);g.setColor(Color.WHITE);g.drawString(person.name(),x+8,z);
                    }
                    g.setFont(new Font("SansSerif",Font.PLAIN,12));g.setColor(Color.WHITE);g.drawString("Red outline: OP-protected orchard. Dots: actual resident positions.",40,610);g.drawString("Origin "+origin.toShortString()+" | world time "+snapshot.time(),40,630);
                }finally{g.dispose();}
                ImageIO.write(map,"png",folder.resolve(String.format("town-map-%04d.png",seconds)).toFile());
            }catch(Exception e){System.err.println("[CraftDroid town] Survey render failed: "+e.getMessage());}
        });
        return metadata;
    }
    static void flush(){try{RENDER.submit(()->{}).get(15,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException(e);}}
}
