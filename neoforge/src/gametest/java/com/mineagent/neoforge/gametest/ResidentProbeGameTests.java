package com.mineagent.neoforge.gametest;

import com.google.gson.GsonBuilder;
import com.mineagent.api.llm.provider.*;
import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.resident.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Runs the actual AgentLoop, tools, local executor, vanilla crafting/building, personal memory and image feedback. */
@PrefixGameTestTemplate(false)
public class ResidentProbeGameTests {
    @GameTest(template="empty",templateNamespace="mineagent_test",batch="resident_probe",timeoutTicks=2600)
    public static void residentPlansCraftsBuildsAndRemembers(GameTestHelper helper){
        var level=helper.getLevel();ResidentGameTests.ensureProfileCache(level.getServer());
        boolean live=System.getProperty("craftdroid.residentProbe","").equals("live");
        if(live&&!Residents.hasCredentials())throw new IllegalStateException("Live probe requires a configured DeepSeek secret");
        var profile=Residents.create(level.getServer(),level,helper.absolutePos(new BlockPos(3,2,3)),"ProbeRowan",true);
        var state=MineAgentEngine.getCompanion(profile.id()).orElseThrow();
        BlockPos[] cells={helper.absolutePos(new BlockPos(5,2,3)),helper.absolutePos(new BlockPos(5,2,4)),helper.absolutePos(new BlockPos(5,2,5))};
        var original=LLMProviderRegistry.get("deepseek").orElseThrow();var probe=new ResidentProbeProvider(live,profile.id(),cells);
        LLMProviderRegistry.register(probe);
        var done=new AtomicBoolean();long started=System.nanoTime();
        Runnable cleanup=()->{if(done.compareAndSet(false,true)){Residents.remove(profile.id());LLMProviderRegistry.register(original);writeReport(probe,cells,started);}};
        state.companion.serverPlayer().getInventory().clearContent();
        state.companion.serverPlayer().getInventory().setItem(0,new ItemStack(Items.OAK_LOG,2));
        String target=Arrays.stream(cells).map(p->"["+p.getX()+","+p.getY()+","+p.getZ()+"]").reduce((a,b)->a+","+b).orElseThrow();
        state.loop.onAmbientEvent("A short controlled gameplay check: you have 2 oak logs. Please craft oak planks using minecraft:oak_planks (one recipe batch), then place one oak plank at each of these exact air cells: "+target+". They have stone support and are within reach. Use resident_work for a craft/build sequence with grounded coordinates. This is a small marker, not a house. Do not mine, explore or place elsewhere during this check. Once the marker is verified, record your promise to stay away from the orchard using resident_memory, request observe_scene and briefly describe the image when it arrives. Your own immutable UUID is "+profile.id()+".");
        state.lifecycle.resumeByOwner();
        helper.onEachTick(()->{
            if(done.get())return;
            // GameTest normally advances as fast as possible. Pace this opt-in behavioral probe at roughly normal gameplay speed.
            java.util.concurrent.locks.LockSupport.parkNanos(50_000_000L);
            try {
                if(!probe.failure.isEmpty())throw new IllegalStateException(probe.failure);
                boolean built=Arrays.stream(cells).allMatch(p->level.getBlockState(p).is(Blocks.OAK_PLANKS));
                if(built&&!probe.built){probe.built=true;state.loop.onAmbientEvent("The three marker blocks have now been verified in the world. Please remember the orchard boundary, request observe_scene, and call resident_say to describe the returned image aloud when it arrives. Ordinary assistant text cannot be heard.");state.loop.wake("probe_marker_verified");}
                boolean remembered=Residents.profile(profile.id()).memories().stream().anyMatch(m->m.text().toLowerCase(Locale.ROOT).contains("orchard"));
                var speech=Residents.lastSpeech(profile.id());
                String imagePrefix=probe.imageDescription.substring(0,Math.min(50,probe.imageDescription.length()));
                boolean imageSpoken=!imagePrefix.isBlank()&&speech!=null&&speech.text().contains(imagePrefix);
                if(built&&remembered&&imageSpoken&&!state.loop.isLocalWorkActive()){
                    helper.assertTrue(!speech.text().contains("PRIVATE_ACTION_PREAMBLE"),"Only deliberate resident_say dialogue reaches the world");
                    helper.assertTrue(state.loop.localWorkSnapshot().status()==com.mineagent.engine.skill.SkillRuntime.Status.SUCCEEDED,"Procedure must verify successfully, not merely stop after placing blocks");
                    var runStatus=com.mineagent.tools.management.TaskStatusTool.getTaskInfo(profile.id(),state.loop.localWorkSnapshot().runId());
                    helper.assertTrue(runStatus!=null&&runStatus.state==com.mineagent.api.task.TaskState.SUCCESS,"The local procedure ID remains queryable after success");
                    helper.assertTrue(state.companion.serverPlayer().getInventory().countItem(Items.OAK_LOG)==1,"A real log was consumed, without creative inventory");
                    helper.assertTrue(probe.samples.stream().anyMatch(s->((List<?>)s.getOrDefault("tools",List.of())).contains("resident_work")),"The actual model/tool route admitted local work");
                    probe.outcome="passed";cleanup.run();helper.succeed();
                }else if(System.nanoTime()-started>java.util.concurrent.TimeUnit.SECONDS.toNanos(115))throw new IllegalStateException("Resident probe exceeded wall-clock budget; built="+built+", remembered="+remembered+", image_responses="+probe.completedImages.get()+", image_spoken="+imageSpoken);
            }catch(Throwable failure){probe.outcome="failed";cleanup.run();throw failure;}
        });
    }

    @GameTest(template="empty",templateNamespace="mineagent_test",batch="resident_social_probe",timeoutTicks=2400)
    public static void residentsConverseAndKeepIndividualRelationshipNotes(GameTestHelper helper){
        if(!System.getProperty("craftdroid.residentProbe","").equals("live"))throw new IllegalStateException("Social probe is live-only");
        var level=helper.getLevel();ResidentGameTests.ensureProfileCache(level.getServer());
        var rowan=Residents.create(level.getServer(),level,helper.absolutePos(new BlockPos(2,2,3)),"SocialRowan",true);
        var mira=Residents.create(level.getServer(),level,helper.absolutePos(new BlockPos(4,2,5)),"SocialMira",true);
        var original=LLMProviderRegistry.get("deepseek").orElseThrow();var probe=new ResidentProbeProvider(true,rowan.id(),new BlockPos[0]);LLMProviderRegistry.register(probe);
        var done=new AtomicBoolean();var restoring=new AtomicBoolean();var recovered=new AtomicBoolean();long started=System.nanoTime();
        Runnable cleanup=()->{
            if(done.compareAndSet(false,true)){
                MineAgentEngine.getCompanion(rowan.id()).ifPresent(s->s.loop.pause());MineAgentEngine.getCompanion(mira.id()).ifPresent(s->s.loop.pause());
                var notes=new LinkedHashMap<String,Object>();notes.put("scenario_status",probe.outcome);notes.put("restart_recovered",recovered.get());notes.put("rowan_notes",Residents.profile(rowan.id()).memories());notes.put("mira_notes",Residents.profile(mira.id()).memories());notes.put("heard_resident_request_count",probe.heardResidentInputs.get());notes.put("requests",probe.samples);notes.put("attempts",probe.attempts.get());notes.put("failure",probe.failure);notes.put("elapsed_ms",(System.nanoTime()-started)/1000000);
                Residents.remove(rowan.id());Residents.remove(mira.id());LLMProviderRegistry.register(original);
                notes.put("attempts",probe.attempts.get());notes.put("failure",probe.failure);
                try{Files.writeString(Path.of("resident-social-probe.json"),new GsonBuilder().setPrettyPrinting().create().toJson(notes));}catch(Exception e){throw new IllegalStateException(e);}
            }
        };
        for(var pair:List.of(new ResidentProfile[]{rowan,mira},new ResidentProfile[]{mira,rowan})){
            var self=pair[0];var other=pair[1];var state=MineAgentEngine.getCompanion(self.id()).orElseThrow();
            Residents.update(self.edit(null,self.id().equals(rowan.id())?"Likes quiet stone houses, libraries, and careful planning.":"Likes gardens, colorful houses, playful conversations, and trying new ideas.",true,null,null));
            state.loop.onAmbientEvent("A short social gameplay check: "+other.name()+" ["+other.id()+"] is nearby and can hear ordinary chat. You two are discussing a possible community town. Address the other by their full name, share one personal preference and ask or answer a concrete question. Listen to their response and use resident_memory to record your own developing opinion or relationship note with subject="+other.id()+". You may agree or disagree; do not invent mutual feelings or agreements. During this check, stay nearby and use chat and memory, without gathering or building. Your own identity is "+self.name()+" ["+self.id()+"].");
        }
        Residents.update(Residents.profile(rowan.id()).edit(null,null,false,null,null));
        helper.runAfterDelay(20,()->Residents.update(Residents.profile(mira.id()).edit(null,null,false,null,null)));
        helper.onEachTick(()->{
            if(done.get())return;java.util.concurrent.locks.LockSupport.parkNanos(50_000_000L);
            try {
                if(!probe.failure.isEmpty())throw new IllegalStateException(probe.failure);
                boolean rowanNote=Residents.profile(rowan.id()).memories().stream().anyMatch(m->mira.id().equals(m.subject()));
                boolean miraNote=Residents.profile(mira.id()).memories().stream().anyMatch(m->rowan.id().equals(m.subject()));
                if(rowanNote&&miraNote&&probe.heardResidentInputs.get()>0&&!restoring.get()){
                    for(var id:List.of(rowan.id(),mira.id())){
                        Residents.update(Residents.profile(id).edit(null,null,true,null,null));
                        Residents.capture(MineAgentEngine.getCompanion(id).orElseThrow());
                    }
                    Residents.flush();
                    MineAgentEngine.despawnCompanion(rowan.id(),false);MineAgentEngine.despawnCompanion(mira.id(),false);
                    Residents.start(level.getServer(),MineAgentEngine.getWorldDataDir());restoring.set(true);
                }else if(restoring.get()&&MineAgentEngine.getCompanion(rowan.id()).isPresent()&&MineAgentEngine.getCompanion(mira.id()).isPresent()){
                    helper.assertTrue(rowanNote&&miraNote,"Independent relationship notes survive snapshot reload and body recreation");
                    helper.assertTrue(MineAgentEngine.getCompanion(rowan.id()).orElseThrow().lifecycle.isPaused(),"Restored probe residents remain paused, preventing surprise paid calls");
                    recovered.set(true);probe.outcome="passed";cleanup.run();helper.succeed();
                }else if(System.nanoTime()-started>java.util.concurrent.TimeUnit.SECONDS.toNanos(100))throw new IllegalStateException("Social probe exceeded wall-clock budget; rowan_note="+rowanNote+", mira_note="+miraNote+", heard_requests="+probe.heardResidentInputs.get());
            }catch(Throwable failure){probe.outcome="failed";cleanup.run();throw failure;}
        });
    }
    private static void writeReport(ResidentProbeProvider probe,BlockPos[] cells,long started){
        try {
            var report=new LinkedHashMap<String,Object>();report.put("mode",probe.live?"live":"offline");report.put("scenario_status",probe.outcome);report.put("image_description",probe.imageDescription);report.put("paid_requests",probe.live?Math.min(12,probe.attempts.get()):0);report.put("attempts",probe.attempts.get());report.put("elapsed_ms",(System.nanoTime()-started)/1000000);report.put("built",probe.built);report.put("image_responses",probe.completedImages.get());report.put("failure",probe.failure);report.put("requests",probe.samples);
            Files.writeString(Path.of("resident-probe.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception e){throw new IllegalStateException("Cannot persist resident probe evidence",e);}
    }
}
