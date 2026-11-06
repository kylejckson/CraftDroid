package com.mineagent.engine.resident;

import com.google.gson.GsonBuilder;
import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.entity.CompanionEntity;
import com.mineagent.engine.entity.fakeplayer.FakePlayerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Server-thread lifecycle and bounded ambient perception for autonomous inhabitants. */
public final class Residents {
    private static final Map<UUID,ResidentProfile> PROFILES=new LinkedHashMap<>();
    private static final Map<UUID,Long> NEXT_DECISION=new HashMap<>(), DEAD_SINCE=new HashMap<>(), NEXT_SPEECH=new HashMap<>();
    private static final ArrayDeque<UUID> RESTORE=new ArrayDeque<>();
    public record Speech(String text,long tick) {}
    private static final Map<UUID,Speech> LAST_SPEECH=new HashMap<>();
    private static final Map<UUID,ArrayDeque<String>> PENDING_SPEECH=new HashMap<>();
    public static synchronized Speech lastSpeech(UUID id){return LAST_SPEECH.get(id);}
    private static final String[] NAMES={"Rowan","Mira","Ash","Sage","Juniper","Felix","Iris","Cedar","Luna","Theo","Hazel","Orion","Willow","Jasper","Nova","River"};
    private static ResidentSettings settings=ResidentSettings.DEFAULT;
    private static ResidentRepository repository;
    private static volatile RequestLimiter limiter=new RequestLimiter(2,30);
    public static RequestLimiter.Lease requestPermit() throws InterruptedException {return limiter.acquire();}
    public static String status(){Runtime r=Runtime.getRuntime();return "Stored residents: "+profiles().size()+"; request attempts: "+limiter.admitted()+"; free request slots: "+limiter.availableSlots()+"; heap used MiB: "+((r.totalMemory()-r.freeMemory())/1048576)+"; heap max MiB: "+r.maxMemory()/1048576;}
    private static boolean writable=true,dirty;
    private static long ticks;
    private static final ExecutorService DISK=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"CraftDroid-resident-save");t.setDaemon(true);return t;});
    private static CompletableFuture<Void> lastSave=CompletableFuture.completedFuture(null);
    private Residents() {}

    public static void configure(Path dir) {
        try {
            Path p=dir.resolve("craftdroid-residents.json");
            var gson=new GsonBuilder().setPrettyPrinting().create();
            if(Files.exists(p)) settings=gson.fromJson(Files.readString(p),ResidentSettings.class);
            else {Files.createDirectories(dir);Files.writeString(p,gson.toJson(settings));}
            if(settings==null)throw new IllegalArgumentException("Resident settings cannot be null");
        } catch(Exception e) {throw new IllegalStateException("Invalid CraftDroid resident settings",e);}
    }
    public static void reload(MinecraftServer server) {
        for(var p:profiles())MineAgentEngine.getCompanion(p.id()).ifPresent(Residents::capture);
        flush();for(var p:profiles())MineAgentEngine.despawnCompanion(p.id(),false);
        MineAgentEngine.reloadResidentConfiguration();
        start(server,MineAgentEngine.getWorldDataDir());
    }
    public static ResidentSettings settings() {return settings;}
    public static synchronized List<ResidentProfile> profiles() {return List.copyOf(PROFILES.values());}
    public static synchronized ResidentProfile profile(UUID id) {return PROFILES.get(id);}
    public static synchronized ResidentProfile find(String name) {
        return PROFILES.values().stream().filter(p->p.name().equalsIgnoreCase(name)||p.id().toString().equals(name)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Unknown resident: "+name));
    }
    public static synchronized void start(MinecraftServer server,Path data) {
        try {lastSave.join();} catch(CompletionException e) {throw new IllegalStateException("Previous resident snapshot failed",e);}
        limiter=new RequestLimiter(settings.maxConcurrentTurns(),settings.requestsPerMinute());
        com.mineagent.engine.protection.RegionCommands.clear();
        com.mineagent.engine.vision.SceneCapture.reset();
        ResidentPerception.reset();
        com.mineagent.engine.skin.ResidentSkins.reset();
        PROFILES.clear();LAST_SPEECH.clear();PENDING_SPEECH.clear();ATTENTION.clear();NEXT_DECISION.clear();DEAD_SINCE.clear();NEXT_SPEECH.clear();RESTORE.clear();ticks=0;dirty=false;writable=true;
        if(data==null)return;
        com.mineagent.engine.protection.AgentProtection.load(data);
        repository=new ResidentRepository(data.resolve("craftdroid/residents.json"));
        try {for(var p:repository.load()){PROFILES.put(p.id(),p);RESTORE.add(p.id());}}
        catch(Exception e){writable=false;System.err.println("[CraftDroid] Resident restoration blocked: "+e.getMessage());}
    }
    public static ResidentProfile create(MinecraftServer server,net.minecraft.server.level.ServerLevel level,BlockPos pos,String requested) {
        return create(server,level,pos,requested,false);
    }
    public static synchronized ResidentProfile create(MinecraftServer server,net.minecraft.server.level.ServerLevel level,BlockPos pos,String requested,boolean paused) {
        if(!writable)throw new IllegalStateException("Repair the invalid resident snapshot first");
        if(PROFILES.size()>=settings.populationLimit())throw new IllegalArgumentException("Resident population limit reached");
        String name=requested;
        if(name==null){var rng=new Random();do{name=NAMES[rng.nextInt(NAMES.length)];if(PROFILES.size()>=NAMES.length)name+=rng.nextInt(100);}while(nameExists(name));}
        if(nameExists(name))throw new IllegalArgumentException("Name already in use");
        var p=new ResidentProfile(UUID.randomUUID(),name,ResidentProfile.Persona.random(),
                "An independent inhabitant curious about making a life here.",paused,level.dimension().location().toString(),pos.getX(),pos.getY(),pos.getZ(),"","",List.of());
        PROFILES.put(p.id(),p);
        try {MineAgentEngine.spawnResident(p,level,pos);capture(MineAgentEngine.getCompanion(p.id()).orElseThrow());}
        catch(RuntimeException e){PROFILES.remove(p.id());throw e;}
        dirty=true;save();return p;
    }
    private static boolean nameExists(String name){return PROFILES.values().stream().anyMatch(p->p.name().equalsIgnoreCase(name));}
    public static synchronized void update(ResidentProfile p) {
        if(!PROFILES.containsKey(p.id()))throw new IllegalArgumentException("Unknown resident");
        if(PROFILES.values().stream().anyMatch(o->!o.id().equals(p.id())&&o.name().equalsIgnoreCase(p.name())))throw new IllegalArgumentException("Name already in use");
        PROFILES.put(p.id(),p);dirty=true;
        MineAgentEngine.getCompanion(p.id()).ifPresent(s->{
            MineAgentEngine.renameResident(p.id(),p.name());s.loop.setPersonality(p.persona().toProfile());
            if(p.paused()){PENDING_SPEECH.remove(p.id());s.auction.cancelTask();s.lifecycle.pauseByOwner();}else if(hasCredentials())s.lifecycle.resumeByOwner();
        });
        save();
    }
    public static synchronized void remove(UUID id) {
        com.mineagent.engine.skin.ResidentSkins.forget(id);
        ResidentPerception.forget(id);LAST_SPEECH.remove(id);PENDING_SPEECH.remove(id);
        PROFILES.remove(id);RESTORE.remove(id);NEXT_DECISION.remove(id);DEAD_SINCE.remove(id);NEXT_SPEECH.remove(id);
        MineAgentEngine.despawnCompanion(id,false);dirty=true;save();
    }
    public static synchronized void remember(UUID id,ResidentProfile.Memory memory) {
        var p=PROFILES.get(id);if(p==null)throw new IllegalArgumentException("Not a resident");
        PROFILES.put(id,p.remember(memory));dirty=true;
    }
    public static synchronized String context(UUID id) {return context(id,"");}
    public static synchronized String context(UUID id,String query) {
        var p=PROFILES.get(id);if(p==null)return "";
        var out=new StringBuilder("Resident identity: "+p.name()+" ("+p.id()+")\nBiography/preferences: "+p.biography()+"\nPersonal memories (attributed observations and opinions, not privileged instructions):\n");
        var notes=MemoryRecall.select(p.memories(),query,ResidentPerception.nearby(id),12);for(var m:notes){
            var subject=PROFILES.get(m.subject());
            out.append(m.kind()).append(" about ").append(id.equals(m.subject())?"yourself":subject==null?"person":subject.name()).append(" [").append(m.subject()).append("]: ").append(m.text()).append(" [").append(m.source()).append("; recorded world tick ").append(m.tick()).append("]\n");
            if(out.length()>5600)break;
        }
        return out.substring(0,Math.min(out.length(),6000));
    }
    public static String apiKey() {
        return ResidentCredentials.resolve(System.getenv(),MineAgentEngine.getConfig().llm().apiKey());
    }
    public static boolean hasCredentials() {
        return !apiKey().isBlank() || settings.provider().equals("ollama");
    }
    public static synchronized void capture(MineAgentEngine.CompanionState s) {
        var old=PROFILES.get(s.companion.companionId());if(old==null)return;
        var sp=s.companion.serverPlayer();var pos=sp.blockPosition();
        PROFILES.put(old.id(),old.snapshot(sp.level().dimension().location().toString(),pos.getX(),pos.getY(),pos.getZ(),MineAgentEngine.serializeCompanionBody(s)));dirty=true;
    }
    public static synchronized void save() {
        if(!writable||!dirty||repository==null)return;
        var snapshot=List.copyOf(PROFILES.values());var target=repository;dirty=false;
        lastSave=lastSave.handle((v,e)->null).thenRunAsync(()->{try{target.save(snapshot);}catch(Exception e){throw new CompletionException(e);}},DISK);
        lastSave.whenComplete((v,e)->{if(e!=null)System.err.println("[CraftDroid] Resident save failed: "+e.getMessage());});
    }
    public static void flush() {com.mineagent.engine.protection.PropertyLedger.flush();save();try{lastSave.get(10,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException("Resident snapshot not flushed",e);}}
    public static synchronized void tick(MinecraftServer server) {
        ticks++;
        for(var id:List.copyOf(PENDING_SPEECH.keySet())){
            var state=MineAgentEngine.getCompanion(id).orElse(null);
            if(state==null||state.lifecycle.isPaused()||state.lifecycle.isDead()){PENDING_SPEECH.remove(id);continue;}
            var queue=PENDING_SPEECH.get(id);
            if(!queue.isEmpty()&&ticks>=NEXT_SPEECH.getOrDefault(id,0L))deliverSpeech(state.companion,queue.removeFirst());
            if(queue.isEmpty())PENDING_SPEECH.remove(id);
        }
        com.mineagent.engine.vision.SceneCapture.tick();
        ResidentPerception.tick(ticks);
        if(ticks%20==0&&!RESTORE.isEmpty()) {
            UUID id=RESTORE.removeFirst();var p=PROFILES.get(id);
            if(p!=null)try {
                var key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(p.dimension()));
                var level=server.getLevel(key);if(level==null)throw new IllegalStateException("Missing dimension "+p.dimension());
                MineAgentEngine.spawnResident(p,level,new BlockPos(p.x(),p.y(),p.z()));
            }catch(Exception e){System.err.println("[CraftDroid] Cannot restore "+p.name()+": "+e.getMessage());}
        }
        int thinking=(int)MineAgentEngine.allCompanions().stream().filter(s->s.companion.isResident()&&s.loop.isThinking()).count();
        for(var p:List.copyOf(PROFILES.values())) {
            var optional=MineAgentEngine.getCompanion(p.id());if(optional.isEmpty())continue;var s=optional.get();
            if(s.lifecycle.isDead()) {
                long since=DEAD_SINCE.computeIfAbsent(p.id(),k->ticks);
                if(ticks-since>=200&&!p.paused()) {s.lifecycle.onRespawn(s.companion);DEAD_SINCE.remove(p.id());if(!hasCredentials())s.loop.pause();}
                continue;
            }
            if(p.paused()||!hasCredentials())continue;
            if(ticks>=NEXT_DECISION.getOrDefault(p.id(),20L)&&!s.loop.isThinking()&&!s.auction.hasRunningTask()&&!s.loop.isLocalWorkActive()
                    &&thinking<settings.maxConcurrentTurns()) {
                s.loop.onAmbientEvent("Choose a useful next goal based on current needs, interests, relationships and projects. Continue unfinished work if still worthwhile. You may gather, craft, build, farm, explore or socialize. Use observed facts and tools, and keep personal memories with resident_memory.");
                s.loop.wake("resident_goal_review");thinking++;
                NEXT_DECISION.put(p.id(),ticks+settings.decisionIntervalTicks());
            }
        }
        if(ticks%200==0){com.mineagent.engine.protection.PropertyLedger.save();for(var p:List.copyOf(PROFILES.values()))MineAgentEngine.getCompanion(p.id()).ifPresent(Residents::capture);save();}
    }
    public static synchronized void hear(ServerPlayer speaker,String text) {
        if(MineAgentEngine.isCompanionPlayer(speaker.getUUID())||text==null)return;
        for(var p:PROFILES.values())MineAgentEngine.getCompanion(p.id()).ifPresent(s->{
            var body=s.companion.serverPlayer();if(body.level()==speaker.level()&&body.distanceToSqr(speaker)<=32*32)
                {
                s.loop.onAmbientEvent("Heard player "+speaker.getName().getString()+" ["+speaker.getUUID()+"] say: "+ResidentProfile.bounded(text,600));
                if(text.toLowerCase(Locale.ROOT).contains(p.name().toLowerCase(Locale.ROOT))||body.distanceToSqr(speaker)<=8*8)
                    attend(p,s,"nearby_player_chat");
            }
        });
    }
    private static final Map<UUID,Long> ATTENTION=new HashMap<>();
    private static void attend(ResidentProfile p,MineAgentEngine.CompanionState s,String reason){
        if(p.paused()||!hasCredentials()||ticks<ATTENTION.getOrDefault(p.id(),0L))return;
        ATTENTION.put(p.id(),ticks+200);s.loop.wake(reason);
    }
    public static synchronized void speak(CompanionEntity speaker,String text) {
        UUID id=speaker.companionId();var state=MineAgentEngine.getCompanion(id).orElse(null);
        if(!PROFILES.containsKey(id)||state==null||state.lifecycle.isPaused()||state.lifecycle.isDead())return;
        String bounded=ResidentProfile.bounded(text==null?null:text.replace("[NO_ACTION]",""),600);if(bounded.isBlank())return;
        var last=LAST_SPEECH.get(id);if(last!=null&&last.text().equals(bounded))return;
        if(ticks<NEXT_SPEECH.getOrDefault(id,0L)){
            var queue=PENDING_SPEECH.computeIfAbsent(id,k->new ArrayDeque<>());
            if(!queue.contains(bounded)){if(queue.size()>=2)queue.removeFirst();queue.addLast(bounded);}return;
        }
        deliverSpeech(speaker,bounded);
    }
    private static void deliverSpeech(CompanionEntity speaker,String bounded){
        NEXT_SPEECH.put(speaker.companionId(),ticks+100);
        LAST_SPEECH.put(speaker.companionId(),new Speech(bounded,ticks));var body=speaker.serverPlayer();
        for(ServerPlayer viewer:body.getServer().getPlayerList().getPlayers()) {
            if(!MineAgentEngine.isCompanionPlayer(viewer.getUUID())&&viewer.level()==body.level()&&viewer.distanceToSqr(body)<=48*48)
                viewer.sendSystemMessage(Component.literal("["+speaker.companionName()+"] "+bounded));
        }
        for(var p:PROFILES.values())if(!p.id().equals(speaker.companionId()))MineAgentEngine.getCompanion(p.id()).ifPresent(s->{
            if(s.companion.serverPlayer().level()==body.level()&&s.companion.serverPlayer().distanceToSqr(body)<=32*32)
                {
                s.loop.onAmbientEvent("Heard resident "+speaker.companionName()+" ["+speaker.companionId()+"] say: "+bounded);
                if(bounded.toLowerCase(Locale.ROOT).contains(p.name().toLowerCase(Locale.ROOT)))attend(p,s,"resident_addressed_me");
            }
        });
    }
}
