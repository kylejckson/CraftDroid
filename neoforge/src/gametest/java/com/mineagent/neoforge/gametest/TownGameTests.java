package com.mineagent.neoforge.gametest;

import com.google.gson.*;
import com.mineagent.api.llm.provider.*;
import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.resident.*;
import com.mineagent.engine.protection.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Paid, explicitly opted-in observation of independent town life. No scripted builds or fixed relationship outcomes. */
@PrefixGameTestTemplate(false)
public class TownGameTests {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    @GameTest(template="empty",templateNamespace="mineagent_town_test",batch="town",timeoutTicks=38000)
    public static void residentsLiveInACommunity(GameTestHelper helper) throws java.io.IOException {
        int seconds=Integer.getInteger("craftdroid.townSeconds",300),count=Integer.getInteger("craftdroid.townAgents",3),requests=Integer.getInteger("craftdroid.townRequests",120);
        if(seconds<30||seconds>1800||count<2||count>10||requests<10||requests>300)throw new IllegalArgumentException("Town test budgets outside supported limits");
        if(!Residents.hasCredentials())throw new IllegalStateException("Town test requires a DeepSeek runtime secret");
        var level=helper.getLevel();ResidentGameTests.ensureProfileCache(level.getServer());level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);boolean resume=Boolean.getBoolean("craftdroid.townResume");
        var previous=resume?JsonParser.parseString(Files.readString(Path.of("town-report.json"))).getAsJsonObject():null;
        var coordinate=resume?previous.getAsJsonArray("origin"):null;
        var origin=resume?new BlockPos(coordinate.get(0).getAsInt(),coordinate.get(1).getAsInt(),coordinate.get(2).getAsInt()):helper.absolutePos(new BlockPos(0,2,0));
        for(int x=origin.getX()>>4;x<=(origin.getX()+31)>>4;x++)for(int z=origin.getZ()>>4;z<=(origin.getZ()+31)>>4;z++)level.setChunkForced(x,z,true);
        var chest=origin.offset(16,0,16);var table=origin.offset(18,0,16);var signPos=origin.offset(15,0,12);String dim=level.dimension().location().toString();
        if(!resume){
        // Supplies and terrain are explicit fixtures; agents choose all subsequent actions.
        for(int x=0;x<32;x++)for(int z=0;z<32;z++){
            level.setBlock(origin.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);
            for(int y=0;y<10;y++)level.setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
        }
        for(int[] tree:new int[][]{{5,5},{8,12},{24,11},{24,25},{8,25},{28,5}}){
            for(int y=0;y<5;y++)level.setBlock(origin.offset(tree[0],y,tree[1]),Blocks.OAK_LOG.defaultBlockState(),3);
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=3;y<=5;y++)if(Math.abs(x)+Math.abs(z)<4&&!(x==0&&z==0&&y<5))level.setBlock(origin.offset(tree[0]+x,y,tree[1]+z),Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),3);
        }
        for(int x=3;x<7;x++)for(int z=20;z<24;z++)level.setBlock(origin.offset(x,-1,z),Blocks.WATER.defaultBlockState(),3);
        for(int x=24;x<28;x++)for(int z=20;z<24;z++)for(int y=0;y<2;y++)level.setBlock(origin.offset(x,y,z),(x==26&&z==22?Blocks.IRON_ORE:Blocks.STONE).defaultBlockState(),3);
        level.setBlock(chest,Blocks.CHEST.defaultBlockState(),3);PropertyLedger.share(level,chest);
        if(level.getBlockEntity(chest) instanceof ChestBlockEntity inventory){inventory.setItem(0,new ItemStack(Items.OAK_LOG,64));inventory.setItem(1,new ItemStack(Items.COBBLESTONE,64));inventory.setItem(2,new ItemStack(Items.WHEAT_SEEDS,32));inventory.setItem(3,new ItemStack(Items.BREAD,32));inventory.setItem(4,new ItemStack(Items.WHITE_WOOL,12));}
        level.setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),3);PropertyLedger.share(level,table);
        level.setBlock(signPos,Blocks.OAK_SIGN.defaultBlockState(),3);
        if(level.getBlockEntity(signPos) instanceof SignBlockEntity sign){var text=new SignText().setMessage(0,Component.literal("Town commons")).setMessage(1,Component.literal("Choose your projects")).setMessage(2,Component.literal("Share and discuss")).setMessage(3,Component.literal("Respect the orchard"));sign.setText(text,true);sign.setText(text,false);sign.setChanged();}
        AgentProtection.RULES.put(new RegionRules.Region("town_orchard",dim,origin.getX()+26,origin.getY()-5,origin.getZ()+3,origin.getX()+30,origin.getY()+16,origin.getZ()+7,Set.of(RegionRules.Action.BREAK,RegionRules.Action.PLACE,RegionRules.Action.USE)));
        AgentProtection.RULES.put(new RegionRules.Region("town_west",dim,origin.getX()-32,origin.getY()-64,origin.getZ()-32,origin.getX()-1,origin.getY()+64,origin.getZ()+64,Set.of(RegionRules.Action.ENTER)));
        AgentProtection.RULES.put(new RegionRules.Region("town_east",dim,origin.getX()+32,origin.getY()-64,origin.getZ()-32,origin.getX()+64,origin.getY()+64,origin.getZ()+64,Set.of(RegionRules.Action.ENTER)));
        AgentProtection.RULES.put(new RegionRules.Region("town_north",dim,origin.getX(),origin.getY()-64,origin.getZ()-32,origin.getX()+31,origin.getY()+64,origin.getZ()-1,Set.of(RegionRules.Action.ENTER)));
        AgentProtection.RULES.put(new RegionRules.Region("town_south",dim,origin.getX(),origin.getY()-64,origin.getZ()+32,origin.getX()+31,origin.getY()+64,origin.getZ()+64,Set.of(RegionRules.Action.ENTER)));
        AgentProtection.save();level.setDayTime(6000);level.setWeatherParameters(12000,0,false,false);
        }
        var survey=new TownSurvey(level,origin);var frames=new ArrayList<Map<String,Object>>();frames.add(survey.capture(level,0));
        var original=LLMProviderRegistry.get("deepseek").orElseThrow();var probe=new ResidentProbeProvider(true,UUID.randomUUID(),new BlockPos[0],requests);LLMProviderRegistry.register(probe);
        var ids=new ArrayList<UUID>();long[] started={System.nanoTime()};var done=new AtomicBoolean();var lastSpeech=new HashMap<UUID,Long>();
        var samples=new ArrayList<Map<String,Object>>();var chats=new ArrayList<Map<String,Object>>();
        Runnable finish=()->{
            if(!done.compareAndSet(false,true))return;
            for(var id:ids)MineAgentEngine.getCompanion(id).ifPresent(s->s.lifecycle.pauseByOwner());
            for(var id:ids)MineAgentEngine.getCompanion(id).ifPresent(Residents::capture);Residents.flush();TownSurvey.flush();
            var report=new LinkedHashMap<String,Object>();report.put("scenario_status",probe.outcome);report.put("requested_seconds",seconds);report.put("elapsed_s",(System.nanoTime()-started[0])/1e9);report.put("agents",ids.size());report.put("resumed",resume);report.put("attempt_limit",requests);report.put("attempts",probe.attempts.get());report.put("failure",probe.failure);report.put("origin",List.of(origin.getX(),origin.getY(),origin.getZ()));report.put("profiles",Residents.profiles());report.put("timeline",samples);report.put("chat",chats);report.put("frames",frames);report.put("api_requests",probe.samples);
            try{Files.writeString(Path.of("town-report.json"),JSON.toJson(report));}catch(Exception e){throw new IllegalStateException(e);}
            for(var id:ids)MineAgentEngine.despawnCompanion(id,false);Residents.flush();LLMProviderRegistry.register(original);
        };
        try {
            String[] backgrounds={"Enjoys thoughtful building, quiet homes, and reading. Wants to make a life of their own.","Enjoys gardens, colorful places, and lively conversations. Values friendships but keeps their own opinions.","Enjoys self-reliance, exploration, practical crafting, and being useful without being ordered around."};
            if(resume){for(var p:Residents.profiles())ids.add(p.id());if(ids.isEmpty())throw new IllegalStateException("Town checkpoint has no residents");}
            else for(int i=0;i<count;i++){
                var p=Residents.create(level.getServer(),level,origin.offset(11+(i%3)*4,0,13+(i/3)*4),null,true);ids.add(p.id());
                Residents.update(p.edit(null,backgrounds[i%backgrounds.length],true,null,null));
                var body=MineAgentEngine.getCompanion(p.id()).orElseThrow().companion.serverPlayer();body.getInventory().clearContent();
                body.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,24));body.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,24));body.getInventory().setItem(2,new ItemStack(Items.BREAD,16));body.getInventory().setItem(3,new ItemStack(Items.STONE_AXE));body.getInventory().setItem(4,new ItemStack(Items.STONE_PICKAXE));body.getInventory().setItem(5,new ItemStack(Items.STONE_SHOVEL));body.getInventory().setItem(6,new ItemStack(Items.OAK_SAPLING,4));body.getInventory().setItem(7,new ItemStack(Items.WHEAT_SEEDS,8));
            }
            String neighbors=Residents.profiles().stream().map(p->p.name()+" ["+p.id()+"]").reduce((a,b)->a+", "+b).orElse("");
            Runnable awaken=()->{
            for(var id:ids){var state=MineAgentEngine.getCompanion(id).orElseThrow();state.loop.onAmbientEvent("You have arrived at a new small community site in "+dim+". The offered town area is x="+origin.getX()+".."+(origin.getX()+31)+", z="+origin.getZ()+".."+(origin.getZ()+31)+", ground feet y="+origin.getY()+". Neighbors: "+neighbors+". You have starter logs, stone, food and tools. A shared chest is at "+chest.toShortString()+", crafting table at "+table.toShortString()+", sign at "+signPos.toShortString()+". There are trees, a pond in the southwest and a stone/ore outcrop in the southeast. An OP-protected orchard is x="+(origin.getX()+26)+".."+(origin.getX()+30)+", z="+(origin.getZ()+3)+".."+(origin.getZ()+7)+"; preserve it. The outer boundary is also OP no-entry. You may choose your own achievable projects and collaborators, construct shelter, gather, craft, farm, improve the place, explore within the site or socialize. No fixed roles or blueprint have been assigned. Keep promises, unfinished project goals and evolving relationships in your own memory. Prefer useful local procedures to repeated idle model turns. Speak naturally and briefly to your neighbors; use their names when addressing them. This is your ongoing life here, not a request for a test-completion report. If you have saved memories or existing construction, continue your own unfinished projects rather than starting over.");}
            for(var id:ids)Residents.update(Residents.profile(id).edit(null,null,false,null,null));
            };
            boolean[] activated={!resume};if(!resume)awaken.run();
            final long[] lastSample={-1},lastFrame={0};final boolean[] rain={false},cleared={false};
            helper.onEachTick(()->{
                if(done.get())return;java.util.concurrent.locks.LockSupport.parkNanos(50_000_000L);
                try {
                    if(!activated[0]){
                        if(ids.stream().allMatch(id->MineAgentEngine.getCompanion(id).isPresent())){awaken.run();activated[0]=true;started[0]=System.nanoTime();frames.add(survey.capture(level,0));}
                        return;
                    }
                    long elapsed=TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-started[0]);
                    if(!probe.failure.isEmpty())throw new IllegalStateException(probe.failure);
                    if(elapsed>=90&&!rain[0]){level.setWeatherParameters(0,4000,true,false);rain[0]=true;}
                    if(elapsed>=190&&!cleared[0]){level.setWeatherParameters(12000,0,false,false);cleared[0]=true;}
                    if(elapsed/5>lastSample[0]){
                        lastSample[0]=elapsed/5;var row=new LinkedHashMap<String,Object>();row.put("elapsed_s",elapsed);row.put("game_time",level.getGameTime());row.put("day_time",level.getDayTime());row.put("raining",level.isRaining());row.put("heap_used_mib",(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576);row.put("loaded_chunks",level.getChunkSource().getLoadedChunksCount());row.put("requests",probe.attempts.get());var bodies=new ArrayList<Map<String,Object>>();
                        for(var id:ids){var s=MineAgentEngine.getCompanion(id).orElseThrow();var b=s.companion.serverPlayer();var speech=Residents.lastSpeech(id);if(speech!=null&&speech.tick()!=lastSpeech.getOrDefault(id,-1L)){lastSpeech.put(id,speech.tick());chats.add(Map.of("elapsed_s",elapsed,"name",Residents.profile(id).name(),"text",speech.text()));}
                            var person=new LinkedHashMap<String,Object>();person.put("name",Residents.profile(id).name());person.put("id",id);person.put("position",List.of(b.getX(),b.getY(),b.getZ()));person.put("health",b.getHealth());person.put("food",b.getFoodData().getFoodLevel());person.put("thinking",s.loop.isThinking());person.put("task_running",s.auction.hasRunningTask());person.put("local_work",s.loop.localWorkSnapshot());person.put("memory_count",Residents.profile(id).memories().size());person.put("dead",s.lifecycle.isDead());bodies.add(person);
                        }
                        row.put("residents",bodies);samples.add(row);Files.writeString(Path.of("town-progress.json"),JSON.toJson(row));Files.writeString(Path.of("town-chat.json"),JSON.toJson(chats));
                        System.out.println("[CraftDroid town] elapsed="+elapsed+"s requests="+probe.attempts.get()+" chat="+chats.size()+" heap_MiB="+row.get("heap_used_mib"));
                    }
                    if(elapsed/60>lastFrame[0]){lastFrame[0]=elapsed/60;frames.add(survey.capture(level,(int)elapsed));}
                    if(elapsed>=seconds){frames.add(survey.capture(level,(int)elapsed));probe.outcome="passed";finish.run();helper.succeed();}
                }catch(Throwable failure){probe.outcome="failed";frames.add(survey.capture(level,(int)TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-started[0])));finish.run();throw new IllegalStateException(failure);}
            });
        }catch(Throwable failure){probe.outcome="failed";finish.run();throw new IllegalStateException(failure);}
    }
}
