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
    public ResidentGameTests() {
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
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(3,1,3));
        var p=Residents.create(level.getServer(),level,pos,"NeoResident",true);
        try {
            var state=MineAgentEngine.getCompanion(p.id()).orElseThrow();
            helper.assertTrue(state.companion.serverPlayerOwner()==null,"Must work without a human owner");
            state.companion.serverPlayer().getInventory().setItem(0,new ItemStack(Items.OAK_LOG,4));
            Residents.update(p.edit("NeoRenamed",null,true,null,null));Residents.capture(state);
            var snapshot=Residents.profile(p.id());MineAgentEngine.despawnCompanion(p.id(),false);
            var body=MineAgentEngine.spawnResident(snapshot,level,pos);
            helper.assertTrue(body.serverPlayer().getUUID().equals(p.id()),"UUID survives rename and restore");
            helper.assertTrue(body.serverPlayer().getInventory().getItem(0).getCount()==4,"Inventory restored on NeoForge");
            var target=body.serverPlayer().blockPosition().below();
            AgentProtection.RULES.put(new RegionRules.Region("neo_test",level.dimension().location().toString(),target.getX(),target.getY(),target.getZ(),target.getX(),target.getY(),target.getZ(),java.util.Set.of(RegionRules.Action.BREAK)));
            helper.assertTrue(!body.serverPlayer().gameMode.destroyBlock(target),"OP protection holds at final NeoForge break");
            helper.assertTrue(!level.getBlockState(target).isAir(),"Protected floor remains");
            var human=helper.makeMockServerPlayerInLevel();human.setPos(body.posX()+1,body.posY(),body.posZ());
            float before=human.getHealth();
            human.hurt(level.damageSources().playerAttack(body.serverPlayer()),2f);
            helper.assertTrue(human.getHealth()==before,"NeoForge damage event blocks player harm");
            helper.assertTrue(!state.loop.isThinking(),"No paid model call during native testing");
            com.mineagent.engine.vision.SceneCapture.request(body.serverPlayer());
            java.util.UUID id=p.id();
            helper.runAfterDelay(80,()->{
                try {
                    var file=MineAgentEngine.getWorldDataDir().resolve("craftdroid/scenes/"+id+".png");
                    helper.assertTrue(java.nio.file.Files.exists(file),"Server-only scene capture produced a PNG");
                    var image=javax.imageio.ImageIO.read(file.toFile());
                    helper.assertTrue(image.getWidth()==192&&image.getHeight()==108,"Bounded software-rendered image size");
                    Residents.remove(id);helper.succeed();
                }catch(Exception e){Residents.remove(id);throw new IllegalStateException(e);}
            });
        } catch(Throwable failure) {Residents.remove(p.id());throw failure;} finally {AgentProtection.RULES.remove("neo_test");}
    }
}
