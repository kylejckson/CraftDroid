package com.mineagent.engine.protection;

import com.google.gson.*;
import com.mineagent.engine.MineAgentEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import java.nio.file.*;
import java.util.*;

/** Final gameplay checks, independent of model instructions and stale task admission. */
public final class AgentProtection {
    public static final RegionRules RULES=new RegionRules();
    private static Path file;
    private static volatile boolean valid=true;
    private static final Map<UUID,net.minecraft.world.phys.Vec3> SAFE=new HashMap<>();
    private static final Map<UUID,String> SAFE_DIM=new HashMap<>();
    private AgentProtection() {}
    public static void load(Path data) {
        for(var r:RULES.regions())RULES.remove(r.name());SAFE.clear();SAFE_DIM.clear();valid=true;
        PropertyLedger.load(data);
        file=data.resolve("craftdroid/regions.json");
        if(!Files.exists(file))return;
        try {
            if(Files.size(file)>131072)throw new IllegalArgumentException("Region snapshot too large");
            var entries=new Gson().fromJson(Files.readString(file),RegionRules.Region[].class);
            for(var r:entries)RULES.put(r);
        }catch(Exception e){valid=false;System.err.println("[CraftDroid] Region rules invalid; agent actions blocked: "+e.getMessage());}
    }
    public static void save() throws java.io.IOException {
        if(!valid)throw new java.io.IOException("Repair invalid region file before editing");
        Files.createDirectories(file.getParent());Path tmp=file.resolveSibling("regions.json.tmp");
        Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().create().toJson(RULES.regions()));
        try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
    }
    public static boolean allows(Level level,BlockPos pos,RegionRules.Action action) {
        return valid&&RULES.allows(level.dimension().location().toString(),pos.getX(),pos.getY(),pos.getZ(),action);
    }
    public static boolean isResident(ServerPlayer actor){return MineAgentEngine.getCompanion(actor.getUUID()).map(s->s.companion.isResident()).orElse(false);}
    public static boolean mayBreak(ServerPlayer actor,BlockPos pos) {
        if(!allows(actor.level(),pos,RegionRules.Action.BREAK))return false;
        if(!isResident(actor))return true;
        if(!PropertyLedger.healthy())return false;
        if(PropertyLedger.isHumanOwned(actor.level(),pos))return false;
        if(PropertyLedger.isResidentOwned(actor.level(),pos))return true;
        // Unknown crafted blocks and containers are not mining resources. Existing property needs OP regions.
        var state=actor.level().getBlockState(pos);String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return !state.hasBlockEntity()&&(state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES)||state.is(BlockTags.DIRT)
                ||state.is(BlockTags.BASE_STONE_OVERWORLD)||state.is(BlockTags.BASE_STONE_NETHER)
                ||state.is(BlockTags.SAND)||id.endsWith("_ore")||id.equals("gravel")||id.equals("snow")||id.equals("snow_block"));
    }
    public static boolean mayAttack(ServerPlayer actor,Entity target) {
        return !(target instanceof Player)&&allows(actor.level(),target.blockPosition(),RegionRules.Action.USE);
    }
    public static boolean mayUse(ServerPlayer actor,BlockPos pos) {
        if(!allows(actor.level(),pos,RegionRules.Action.USE))return false;
        if(!isResident(actor))return true;
        for (var stack : List.of(actor.getMainHandItem(),actor.getOffhandItem())) {
            String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if(item.contains("lava_bucket")||item.contains("flint_and_steel")||item.contains("fire_charge")||item.equals("tnt")||item.contains("end_crystal"))return false;
        }
        return PropertyLedger.mayUse(actor,pos);
    }
    public static void enforcePosition(ServerPlayer actor) {
        boolean permitted=positionAllowed(actor,actor.position());
        UUID id=actor.getUUID();String dim=actor.level().dimension().location().toString();
        if(permitted){SAFE.put(id,actor.position());SAFE_DIM.put(id,dim);return;}
        var safe=SAFE.get(id);
        boolean canRestore=safe!=null&&dim.equals(SAFE_DIM.get(id))&&positionAllowed(actor,safe);
        boolean evacuated=!canRestore&&valid&&com.mineagent.engine.entity.SafeTeleport.near(actor,actor.serverLevel(),actor.blockPosition(),actor.getYRot(),actor.getXRot());
        final boolean restored=canRestore||evacuated;
        if(canRestore) {
            actor.teleportTo(safe.x,safe.y,safe.z);actor.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        MineAgentEngine.allCompanions().stream().filter(s->s.companion.serverPlayer()==actor).findFirst().ifPresent(s->{s.auction.cancelTask();
            if(!restored){s.lifecycle.pauseByOwner();s.companion.inputDriver().clear();}
            else s.loop.onAmbientEvent("OP region blocked movement; choose an allowed route.");});
    }
    private static boolean positionAllowed(ServerPlayer actor,net.minecraft.world.phys.Vec3 pos){
        var delta=pos.subtract(actor.position());var box=actor.getBoundingBox().move(delta);
        for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)
            if(!allows(actor.level(),BlockPos.containing(x==0?box.minX:box.maxX-.001,y==0?box.minY:box.maxY-.001,z==0?box.minZ:box.maxZ-.001),RegionRules.Action.ENTER))return false;
        return true;
    }
    public static void forget(UUID id){SAFE.remove(id);SAFE_DIM.remove(id);}
}
