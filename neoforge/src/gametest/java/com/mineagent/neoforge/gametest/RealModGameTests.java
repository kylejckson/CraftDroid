package com.mineagent.neoforge.gametest;

import com.mineagent.api.platform.Services;
import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.resident.Residents;
import com.mineagent.engine.protection.PropertyLedger;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Only registered by the opt-in pinned modpack run. No test dependencies are shipped in production. */
@PrefixGameTestTemplate(false)
public class RealModGameTests {
    @GameTest(template="empty",templateNamespace="mineagent_test",batch="machines",timeoutTicks=600)
    public static void realMachinePorts(GameTestHelper helper){
        var level=helper.getLevel();ResidentGameTests.ensureProfileCache(level.getServer());var origin=helper.absolutePos(new BlockPos(2,1,2));
        var profile=Residents.create(level.getServer(),level,origin,"ModTester",true);
        try {
            var state=MineAgentEngine.getCompanion(profile.id()).orElseThrow();var actor=state.companion.serverPlayer();
            var smelter=helper.absolutePos(new BlockPos(4,2,3));
            var blockId=ResourceLocation.parse("mekanism:energized_smelter");
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(blockId),"Real Mekanism must load");
            var machineBlock=BuiltInRegistries.BLOCK.get(blockId);
            actor.getInventory().setItem(0,new ItemStack(machineBlock.asItem()));
            helper.assertTrue(com.mineagent.engine.act.Placement.placeBlock(actor,smelter,machineBlock.defaultBlockState()),"Resident places Mekanism through real item interactions");
            PropertyLedger.share(level,smelter);
            helper.runAfterDelay(10,()->{
            try {
            System.out.println("[CraftDroid machine setup] state="+level.getBlockState(smelter)+" tile="+level.getBlockEntity(smelter)+" loaded="+level.isLoaded(smelter));
            actor.getInventory().setItem(0,new ItemStack(Items.RAW_IRON));
            boolean inserted=false,powered=false;
            for(var face:Direction.values()){
                var energy=level.getCapability(Capabilities.EnergyStorage.BLOCK,smelter,face);
                System.out.println("[CraftDroid Mekanism capability] face="+face+" energy="+(energy==null?"unavailable":energy.getEnergyStored()+"/"+energy.getMaxEnergyStored()+" receive="+energy.canReceive()));
                if(energy!=null)powered|=energy.receiveEnergy(100000,false)>0;
                if(!inserted){var result=Services.platform().transferBlockItems(state.companion,smelter.getX(),smelter.getY(),smelter.getZ(),face.getName(),0,"minecraft:raw_iron",1,true);inserted=result.verified()&&result.moved()==1;}
            }
            helper.assertTrue(powered,"Mekanism accepts real public FE energy");
            helper.assertTrue(inserted&&actor.getInventory().getItem(0).isEmpty(),"Input moves from resident inventory into real Mekanism port");
            // AE2's charger exposes an actual sided automation inventory.
            var charger=helper.absolutePos(new BlockPos(4,2,5));var chargerId=ResourceLocation.parse("ae2:charger");
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(chargerId),"Real AE2 must load");
            var aeBlock=BuiltInRegistries.BLOCK.get(chargerId);
            actor.getInventory().setItem(0,new ItemStack(aeBlock.asItem()));
            helper.assertTrue(com.mineagent.engine.act.Placement.placeBlock(actor,charger,aeBlock.defaultBlockState()),"Resident places AE2 through real item interactions");
            PropertyLedger.share(level,charger);
            var quartz=ResourceLocation.parse("ae2:certus_quartz_crystal");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(quartz),"AE2 item registry available");
            actor.getInventory().setItem(1,new ItemStack(BuiltInRegistries.ITEM.get(quartz)));
            boolean aeInserted=false;
            for(var face:Direction.values())if(!aeInserted){var result=Services.platform().transferBlockItems(state.companion,charger.getX(),charger.getY(),charger.getZ(),face.getName(),1,quartz.toString(),1,true);aeInserted=result.verified()&&result.moved()==1;}
            helper.assertTrue(aeInserted,"AE2 accepts a resident's actual item through its public port");
            helper.runAfterDelay(400,()->{
                try {
                    boolean extracted=false;
                    for(var face:Direction.values())if(!extracted){var result=Services.platform().transferBlockItems(state.companion,smelter.getX(),smelter.getY(),smelter.getZ(),face.getName(),2,"minecraft:iron_ingot",1,false);extracted=result.verified()&&result.moved()==1;}
                    helper.assertTrue(extracted&&actor.getInventory().getItem(2).is(Items.IRON_INGOT),"Real powered Mekanism processes raw iron and returns an actual ingot to the resident");
                    System.out.println("[CraftDroid real mods] Mekanism powered smelting and AE2 public inventory transfer passed");helper.succeed();
                }finally{Residents.remove(profile.id());PropertyLedger.forget(level,smelter);PropertyLedger.forget(level,charger);}
            });
            }catch(Throwable e){Residents.remove(profile.id());throw e;}
            });
        }catch(Throwable e){Residents.remove(profile.id());throw e;}
    }
}
