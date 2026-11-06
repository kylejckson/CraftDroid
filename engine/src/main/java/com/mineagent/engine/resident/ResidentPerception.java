package com.mineagent.engine.resident;

import com.mineagent.engine.MineAgentEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import java.util.*;

/** Server-thread, loaded-only ambient observations. One slice per resident every two seconds. */
public final class ResidentPerception {
    private static final Map<UUID,Integer> CURSORS=new HashMap<>();
    private static final Map<UUID,String> WEATHER=new HashMap<>();
    private static final Map<UUID,LinkedHashMap<Long,String>> SIGNS=new HashMap<>();
    private static final Map<UUID,Set<UUID>> NEARBY=new HashMap<>();
    private ResidentPerception(){}
    public static void reset(){CURSORS.clear();WEATHER.clear();SIGNS.clear();NEARBY.clear();}
    public static void forget(UUID id){CURSORS.remove(id);WEATHER.remove(id);SIGNS.remove(id);NEARBY.remove(id);}
    public static Set<UUID> nearby(UUID id){return NEARBY.getOrDefault(id,Set.of());}
    public static void tick(long tick){
        if(tick%40!=0)return;
        for(var p:Residents.profiles())MineAgentEngine.getCompanion(p.id()).ifPresent(s->{
            var body=s.companion.serverPlayer();var level=body.serverLevel();if(!body.isAlive())return;
            Set<UUID> near=new HashSet<>();for(var player:level.players())if(player!=body&&player.distanceToSqr(body)<=32*32)near.add(player.getUUID());
            NEARBY.put(p.id(),Set.copyOf(near));
            String weather=(level.isRainingAt(body.blockPosition())?"rain falling here":level.isRaining()?"raining outside; currently sheltered":"dry weather")+", "+(level.isDay()?"daylight":"night");
            String old=WEATHER.put(p.id(),weather);
            if(!Objects.equals(old,weather))s.loop.onAmbientEvent("Weather/time observation: "+weather+". Your response and mood are your choice.");
            int cursor=CURSORS.getOrDefault(p.id(),0);var known=SIGNS.computeIfAbsent(p.id(),k->new LinkedHashMap<>());
            for(int n=0;n<32;n++){
                int cell=(cursor+n)%1183;var pos=body.blockPosition().offset(cell%13-6,(cell/13)%7-3,cell/91-6);
                if(!level.isLoaded(pos)||!(level.getBlockEntity(pos) instanceof SignBlockEntity sign))continue;
                if(body.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>64||com.mineagent.engine.act.BlockTargeting.findVisibleHit(body,pos,8).isEmpty())continue;
                String text=String.join(" | ",Arrays.stream(sign.getText(sign.isFacingFrontText(body)).getMessages(false)).map(c->ResidentProfile.bounded(c.getString(),160)).toList());
                if(!Objects.equals(known.put(pos.asLong(),text),text)&&!text.isBlank())
                    s.loop.onAmbientEvent("Visible sign at "+pos.toShortString()+" (author unknown, world text): "+text);
                while(known.size()>32)known.remove(known.keySet().iterator().next());
            }
            CURSORS.put(p.id(),(cursor+32)%1183);
        });
    }
}
