package com.mineagent.neoforge.gametest;

import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.resident.Residents;
import com.mineagent.engine.protection.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.*;

@Mod("mineagent_test")
@GameTestHolder("mineagent_test")
@PrefixGameTestTemplate(false)
public class ResidentGameTests {
    public ResidentGameTests(net.neoforged.bus.api.IEventBus bus) {
        if(Boolean.getBoolean("craftdroid.modpackTests"))bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event)->event.register(RealModGameTests.class));
        if(Boolean.getBoolean("neoforge.gameTestServer")) {
            try {
                java.io.File report=new java.io.File(System.getProperty("craftdroid.testReport","gametest-results.xml"));
                report.getParentFile().mkdirs();
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(report));
            } catch(javax.xml.parsers.ParserConfigurationException e) {throw new IllegalStateException(e);}
        }
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void residentLifecycleAndPolicy(GameTestHelper helper) {
        var level=helper.getLevel();ensureProfileCache(level.getServer());var pos=helper.absolutePos(new BlockPos(3,1,3));
        var p=Residents.create(level.getServer(),level,pos,"NeoResident",true);
        try {
            var state=MineAgentEngine.getCompanion(p.id()).orElseThrow();
            helper.assertTrue(state.companion.serverPlayerOwner()==null,"Must work without a human owner");
            state.companion.serverPlayer().getInventory().setItem(0,new ItemStack(Items.OAK_LOG,4));
            Residents.update(p.edit("NeoRenamed",null,true,null,null));Residents.capture(state);
            var snapshot=Residents.profile(p.id());MineAgentEngine.despawnCompanion(p.id(),false);
            var body=MineAgentEngine.spawnResident(snapshot,level,pos);
            helper.assertTrue(body.serverPlayer().getUUID().equals(p.id()),"UUID survives rename and restore");
            helper.assertTrue(body.serverPlayer().getName().getString().equals("NeoRenamed"),"Public entity name follows the editable resident identity");
            helper.assertTrue(body.serverPlayer().getInventory().getItem(0).getCount()==4,"Inventory restored on NeoForge");
            var target=body.serverPlayer().blockPosition().below();
            AgentProtection.RULES.put(new RegionRules.Region("neo_test",level.dimension().location().toString(),target.getX(),target.getY(),target.getZ(),target.getX(),target.getY(),target.getZ(),java.util.Set.of(RegionRules.Action.BREAK)));
            helper.assertTrue(!body.serverPlayer().gameMode.destroyBlock(target),"OP protection holds at final NeoForge break");
            helper.assertTrue(!level.getBlockState(target).isAir(),"Protected floor remains");
            var human=helper.makeMockServerPlayerInLevel();
            human.connection=new com.mineagent.engine.entity.fakeplayer.FakePlayerNetworkHandler(level.getServer(),new com.mineagent.engine.entity.fakeplayer.FakeConnection(),human);
            human.setPos(body.posX()+1,body.posY(),body.posZ());
            float before=human.getHealth();
            human.hurt(level.damageSources().playerAttack(body.serverPlayer()),2f);
            helper.assertTrue(human.getHealth()==before,"NeoForge damage event blocks player harm");
            var liveState=MineAgentEngine.getCompanion(p.id()).orElseThrow();
            helper.assertTrue(!liveState.loop.isThinking(),"No paid model call during native testing");
            var property=helper.absolutePos(new BlockPos(1,2,5));var floor=property.below();
            var humanPos=helper.absolutePos(new BlockPos(1,2,3));human.setPos(humanPos.getX()+.5,humanPos.getY(),humanPos.getZ()+.5);
            human.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            human.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_PLANKS,2));
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0,.5,0),net.minecraft.core.Direction.UP,floor,false);
            System.out.println("[CraftDroid property setup] pos="+property+" floor="+level.getBlockState(floor)+" before="+level.getBlockState(property)+" result="+human.gameMode.useItemOn(human,level,human.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit)+" hand="+human.getMainHandItem());
            helper.assertTrue(level.getBlockState(property).is(Blocks.OAK_PLANKS),"Human places an actual block");
            helper.assertTrue(PropertyLedger.isHumanOwned(level,property),"Vanilla placement records human ownership");
            helper.assertTrue(!body.serverPlayer().gameMode.destroyBlock(property),"Resident cannot break new human property");
            PropertyLedger.share(level,property);
            helper.assertTrue(AgentProtection.mayBreak(body.serverPlayer(),property),"Explicitly shared blocks can be maintained by residents");
            body.serverPlayer().setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.LAVA_BUCKET));
            helper.assertTrue(!AgentProtection.mayUse(body.serverPlayer(),target),"Hazard policy checks the offhand too");
            body.serverPlayer().setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var work=liveState.loop.startResidentWork("Check pause","[{\"tool\":\"get_self_status\"},{\"tool\":\"get_self_status\"}]");
            helper.assertTrue(work.accepted(),"Local work can be admitted in the isolated test");
            liveState.lifecycle.pauseByOwner();
            helper.assertTrue(liveState.lifecycle.isPaused(),"OP pause holds body scheduler");
            helper.assertTrue(!liveState.loop.isLocalWorkActive(),"Pause cancels local execution, not only HTTP calls");
            var door=helper.absolutePos(new BlockPos(6,2,3));var top=door.above();
            AgentProtection.RULES.put(new RegionRules.Region("neo_door",level.dimension().location().toString(),top.getX(),top.getY(),top.getZ(),top.getX(),top.getY(),top.getZ(),java.util.Set.of(RegionRules.Action.PLACE)));
            body.serverPlayer().getInventory().setItem(0,new ItemStack(Items.OAK_DOOR));
            helper.assertTrue(!com.mineagent.engine.act.Placement.placeBlock(body.serverPlayer(),door,Blocks.OAK_DOOR.defaultBlockState()),"Two-block placement cannot cross an OP region with its upper half");
            helper.assertTrue(level.getBlockState(top).isAir(),"Protected upper cell stays empty");
            AgentProtection.enforcePosition(body.serverPlayer());
            var forbidden=body.serverPlayer().blockPosition().offset(2,0,0);
            AgentProtection.RULES.put(new RegionRules.Region("neo_enter",level.dimension().location().toString(),forbidden.getX(),forbidden.getY(),forbidden.getZ(),forbidden.getX(),forbidden.getY()+2,forbidden.getZ(),java.util.Set.of(RegionRules.Action.ENTER)));
            body.serverPlayer().teleportTo(forbidden.getX()+.5,forbidden.getY(),forbidden.getZ()+.5);
            AgentProtection.enforcePosition(body.serverPlayer());
            helper.assertTrue(!body.serverPlayer().blockPosition().equals(forbidden),"Movement rolls back at the final body boundary");
            com.mineagent.engine.vision.SceneCapture.request(body.serverPlayer());
            java.util.UUID id=p.id();
            helper.runAfterDelay(80,()->{
                try {
                    var file=MineAgentEngine.getWorldDataDir().resolve("craftdroid/scenes/"+id+".png");
                    helper.assertTrue(java.nio.file.Files.exists(file),"Server-only scene capture produced a PNG");
                    var image=javax.imageio.ImageIO.read(file.toFile());
                    helper.assertTrue(image.getWidth()==192&&image.getHeight()==108,"Bounded software-rendered image size");
                    Residents.remove(id);level.getServer().getPlayerList().remove(human);helper.succeed();
                }catch(Exception e){Residents.remove(id);throw new IllegalStateException(e);}
            });
        } catch(Throwable failure) {Residents.remove(p.id());throw failure;} finally {AgentProtection.RULES.remove("neo_test");AgentProtection.RULES.remove("neo_door");AgentProtection.RULES.remove("neo_enter");}
    }

    @GameTest(template="empty",batch="population",timeoutTicks=240)
    public static void tenResidentsWithoutAHuman(GameTestHelper helper){
        var level=helper.getLevel();var ids=new java.util.ArrayList<java.util.UUID>();
        long start=System.nanoTime();
        try {
            for(int i=0;i<10;i++)ids.add(Residents.create(level.getServer(),level,helper.absolutePos(new BlockPos(2+i%4,1,2+i/4)),"Population"+i,true).id());
            helper.runAfterDelay(100,()->{
                try {
                    for(var id:ids){var state=MineAgentEngine.getCompanion(id).orElseThrow();
                        helper.assertTrue(state.companion.serverPlayerOwner()==null,"No human owner required");
                        helper.assertTrue(state.companion.serverPlayer().isAlive(),"Resident body remains alive");
                        helper.assertTrue(!state.loop.isThinking(),"Load smoke test must not spend API credits");
                    }
                    Runtime runtime=Runtime.getRuntime();
                    System.out.println("[CraftDroid population smoke] residents=10 elapsed_ms="+(System.nanoTime()-start)/1000000+" heap_used_MiB="+(runtime.totalMemory()-runtime.freeMemory())/1048576+" heap_max_MiB="+runtime.maxMemory()/1048576+" loaded_chunks="+level.getChunkSource().getLoadedChunksCount()+" paid_model=false");
                    helper.succeed();
                }finally{for(var id:ids)Residents.remove(id);}
            });
        }catch(Throwable e){for(var id:ids)Residents.remove(id);throw e;}
    }

    static void ensureProfileCache(net.minecraft.server.MinecraftServer server){
        if(server.getProfileCache()!=null)return;
        // Vanilla GameTestServer intentionally omits authentication cache services. Real machines expect a UUID cache.
        try {
            var field=net.minecraft.server.MinecraftServer.class.getDeclaredField("services");field.setAccessible(true);
            var old=(net.minecraft.server.Services)field.get(server);
            var cache=new net.minecraft.server.players.GameProfileCache(old.profileRepository(),new java.io.File("test-usercache.json"));
            cache.setExecutor(Runnable::run);
            field.set(server,new net.minecraft.server.Services(old.sessionService(),old.servicesKeySet(),old.profileRepository(),cache));
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot initialize native test UUID cache",e);}
    }
}
