package com.mineagent.engine.protection;

import com.google.gson.*;
import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Bounded provenance for new player builds and communal resident property; OPs can explicitly share old machines. */
public final class PropertyLedger {
    private static final int LIMIT=32768;
    private static final Map<String,UUID> OWNERS=new LinkedHashMap<>();
    private static final ExecutorService DISK=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"CraftDroid-property-save");t.setDaemon(true);return t;});
    private static CompletableFuture<Void> pending=CompletableFuture.completedFuture(null);
    private static Path file;private static boolean healthy=true,dirty;
    private PropertyLedger(){}
    private static String key(Level level,BlockPos pos){return level.dimension().location()+":"+pos.getX()+","+pos.getY()+","+pos.getZ();}
    public static void load(Path data){
        flush();OWNERS.clear();healthy=true;dirty=false;file=data.resolve("craftdroid/property.json");
        if(!Files.exists(file))return;
        try {
            if(Files.size(file)>8388608)throw new IllegalArgumentException("Property ledger exceeds size limit");
            var json=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(json.size()>LIMIT)throw new IllegalArgumentException("Property ledger exceeds entry budget");
            for(var entry:json.entrySet())OWNERS.put(entry.getKey(),UUID.fromString(entry.getValue().getAsString()));
        }catch(Exception e){healthy=false;System.err.println("[CraftDroid] Invalid property ledger; resident mutations blocked: "+e.getMessage());}
    }
    public static boolean healthy(){return healthy;}
    public static boolean isHumanOwned(Level level,BlockPos pos){var id=OWNERS.get(key(level,pos));return id!=null&&!id.equals(CompanionEntity.RESIDENT_COMMUNITY);}
    public static boolean isResidentOwned(Level level,BlockPos pos){return CompanionEntity.RESIDENT_COMMUNITY.equals(OWNERS.get(key(level,pos)));}
    public static boolean mayUse(ServerPlayer actor,BlockPos pos){return healthy&&!isHumanOwned(actor.level(),pos)
            &&(!actor.level().getBlockState(pos).hasBlockEntity()||isResidentOwned(actor.level(),pos));}
    public static boolean mayPlace(ServerPlayer actor,BlockPos pos){
        return AgentProtection.allows(actor.level(),pos,RegionRules.Action.PLACE)&&(!AgentProtection.isResident(actor)
                ||(healthy&&!isHumanOwned(actor.level(),pos)&&(OWNERS.size()<LIMIT||OWNERS.containsKey(key(actor.level(),pos)))));
    }
    public static void placed(ServerPlayer actor,BlockPos pos){
        if(!healthy)return;
        String key=key(actor.level(),pos);
        if(OWNERS.size()>=LIMIT&&!OWNERS.containsKey(key)){healthy=false;System.err.println("[CraftDroid] Property budget reached; resident mutations blocked until the ledger is maintained");return;}
        OWNERS.put(key,AgentProtection.isResident(actor)?CompanionEntity.RESIDENT_COMMUNITY:actor.getUUID());dirty=true;
    }
    public static void share(Level level,BlockPos pos){
        if(!healthy)throw new IllegalArgumentException("Repair the property ledger before editing");
        if(OWNERS.size()>=LIMIT&&!OWNERS.containsKey(key(level,pos)))throw new IllegalArgumentException("Property entry budget reached");
        OWNERS.put(key(level,pos),CompanionEntity.RESIDENT_COMMUNITY);dirty=true;
    }
    public static void forget(Level level,BlockPos pos){if(OWNERS.remove(key(level,pos))!=null)dirty=true;}
    public static void save(){
        if(!healthy||!dirty||file==null||!pending.isDone())return;
        var copy=new LinkedHashMap<>(OWNERS);Path target=file;dirty=false;
        pending=CompletableFuture.runAsync(()->{
            try {
                Files.createDirectories(target.getParent());Path tmp=target.resolveSibling("property.json.tmp");
                Files.writeString(tmp,new Gson().toJson(copy));
                try{Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException e){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}
            }catch(Exception e){throw new CompletionException(e);}
        },DISK);
    }
    public static void flush(){
        try{pending.get(10,TimeUnit.SECONDS);save();pending.get(10,TimeUnit.SECONDS);}
        catch(Exception e){throw new IllegalStateException("Property snapshot failed; original file retained",e);}
    }
}
