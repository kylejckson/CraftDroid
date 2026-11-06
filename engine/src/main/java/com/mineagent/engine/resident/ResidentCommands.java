package com.mineagent.engine.resident;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.entity.fakeplayer.FakePlayerFactory;
import java.util.UUID;
import static net.minecraft.commands.Commands.*;

/** OP-only administration. No command exposes provider secrets. */
public final class ResidentCommands {
    private ResidentCommands() {}
    @FunctionalInterface private interface Action {String run(CommandSourceStack s) throws Exception;}
    private static int run(CommandContext<CommandSourceStack> c,Action action) {
        try {String result=action.run(c.getSource());c.getSource().sendSuccess(()->Component.literal(result),true);return 1;}
        catch(Exception e){c.getSource().sendFailure(Component.literal(e.getMessage()==null?"Operation failed":e.getMessage()));return 0;}
    }
    private static String value(CommandContext<CommandSourceStack> c,String arg){return StringArgumentType.getString(c,arg);}
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("craftdroid").requires(s->s.hasPermission(2))
            .executes(c->run(c,s->"CraftDroid: spawn [name], list, inspect <name>, rename <name> <newName>, pause/resume/remove <name>, biography <name> <text>, personality <name> <trait> <0..1>, skin <name> <player>, remember <name> <target UUID> <kind> <text>. Settings: config/craftdroid-residents.json"))
             .then(literal("share").then(argument("pos",net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).executes(c->run(c,s->{
                com.mineagent.engine.protection.PropertyLedger.share(s.getLevel(),net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(c,"pos"));return "Block shared with residents; OP zones still apply";
            }))))
            .then(literal("status").executes(c->run(c,s->Residents.status())))
            .then(literal("reload").executes(c->run(c,s->{Residents.reload(s.getServer());return "Settings reloaded; saved residents queued for restoration";})))
            .then(literal("populate").then(argument("count",IntegerArgumentType.integer(1,32)).executes(c->run(c,s->{
                int count=IntegerArgumentType.getInteger(c,"count");
                if(Residents.profiles().size()+count>Residents.settings().populationLimit())throw new IllegalArgumentException("Requested group exceeds the population limit");
                var created=new java.util.ArrayList<UUID>();
                try {
                    var origin=s.getEntity()==null?s.getLevel().getSharedSpawnPos():BlockPos.containing(s.getPosition());
                    for(int i=0;i<count;i++)created.add(Residents.create(s.getServer(),s.getLevel(),origin.offset(i%4,0,i/4),null).id());
                    return "Created "+count+" independent residents with persistent random names and personalities";
                }catch(Exception failure){for(var id:created)Residents.remove(id);throw failure;}
            }))))
            .then(literal("invite").then(argument("text",StringArgumentType.greedyString()).executes(c->run(c,s->{
                String text=value(c,"text");if(text.length()>1000)throw new IllegalArgumentException("Keep the invitation within 1000 characters");
                for(var p:Residents.profiles()){
                    Residents.remember(p.id(),new ResidentProfile.Memory(p.id(),"community_project",text,"Invitation from OP "+s.getTextName(),s.getLevel().getGameTime()));
                    MineAgentEngine.getCompanion(p.id()).ifPresent(state->{state.loop.onAmbientEvent("Community invitation (you choose how to respond): "+text);if(!p.paused()&&Residents.hasCredentials())state.loop.wake("community_invitation");});
                }
                Residents.save();return "Invitation recorded for all residents as a soft community project";
            }))))
            .then(literal("spawn").executes(c->spawn(c,null))
                .then(argument("name",StringArgumentType.word()).executes(c->spawn(c,value(c,"name")))))
            .then(literal("list").executes(c->run(c,s->Residents.profiles().stream().map(p->p.name()+" ["+p.id()+"] "+(p.paused()?"paused":"active")+" "+p.dimension()).reduce((a,b)->a+"\n"+b).orElse("No residents"))))
            .then(literal("inspect").then(argument("name",StringArgumentType.word()).executes(c->run(c,s->{var p=Residents.find(value(c,"name"));return Residents.context(p.id())+"\nState: "+(p.paused()?"paused":"active")+"; credentials "+(Residents.hasCredentials()?"configured":"missing; model turns paused");}))))
            .then(literal("rename").then(argument("name",StringArgumentType.word()).then(argument("newName",StringArgumentType.word()).executes(c->run(c,s->{var p=Residents.find(value(c,"name"));Residents.update(p.edit(value(c,"newName"),null,null,null,null));return "Renamed; identity preserved: "+p.id();})))))
            .then(literal("pause").then(argument("name",StringArgumentType.word()).executes(c->setPaused(c,true))))
            .then(literal("resume").then(argument("name",StringArgumentType.word()).executes(c->setPaused(c,false))))
            .then(literal("remove").then(argument("name",StringArgumentType.word()).executes(c->run(c,s->{var p=Residents.find(value(c,"name"));Residents.remove(p.id());return "Removed "+p.name();}))))
            .then(literal("biography").then(argument("name",StringArgumentType.word()).then(argument("text",StringArgumentType.greedyString()).executes(c->run(c,s->{var p=Residents.find(value(c,"name"));Residents.update(p.edit(null,value(c,"text"),null,null,null));return "Biography/preferences saved";})))))
            .then(literal("personality").then(argument("name",StringArgumentType.word()).then(argument("trait",StringArgumentType.word()).then(argument("value",FloatArgumentType.floatArg(0,1)).executes(c->run(c,s->{
                var p=Residents.find(value(c,"name"));var a=p.persona();float v=FloatArgumentType.getFloat(c,"value");String trait=value(c,"trait");
                if(!java.util.Set.of("openness","conscientiousness","extraversion","agreeableness","neuroticism").contains(trait))throw new IllegalArgumentException("Unknown OCEAN trait");
                var updated=new ResidentProfile.Persona(trait.equals("openness")?v:a.openness(),trait.equals("conscientiousness")?v:a.conscientiousness(),trait.equals("extraversion")?v:a.extraversion(),trait.equals("agreeableness")?v:a.agreeableness(),trait.equals("neuroticism")?v:a.neuroticism(),a.traits());
                Residents.update(p.edit(null,null,null,updated,null));return "Personality saved";
            }))))))
            .then(literal("skinurl").then(argument("name",StringArgumentType.word()).then(argument("url",StringArgumentType.greedyString()).executes(c->run(c,s->{
                var p=Residents.find(value(c,"name"));String url=value(c,"url");var uri=java.net.URI.create(url);
                if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||url.length()>2048)throw new IllegalArgumentException("Expected public HTTPS PNG URL");
                Residents.update(p.edit(null,null,null,null,url));
                MineAgentEngine.getCompanion(p.id()).ifPresent(state->com.mineagent.engine.skin.ResidentSkins.prepare(state.companion.serverPlayer(),url));return "PNG validation/download queued; CraftDroid clients receive the cached texture";
            })))))
            .then(literal("skin").then(argument("name",StringArgumentType.word()).then(argument("player",StringArgumentType.word()).executes(c->run(c,s->{
                String skin=value(c,"player");if(!skin.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Expected a Minecraft skin player name");
                var p=Residents.find(value(c,"name"));Residents.update(p.edit(null,null,null,null,skin));
                com.mineagent.engine.skin.ResidentSkins.forget(p.id());
                MineAgentEngine.getCompanion(p.id()).ifPresent(state->FakePlayerFactory.applySkinAsync(state.companion.serverPlayer(),skin));return "Skin lookup queued";
            })))))
            .then(literal("remember").then(argument("name",StringArgumentType.word()).then(argument("target",StringArgumentType.word()).then(argument("kind",StringArgumentType.word()).then(argument("text",StringArgumentType.greedyString()).executes(c->run(c,s->{
                var p=Residents.find(value(c,"name"));Residents.remember(p.id(),new ResidentProfile.Memory(UUID.fromString(value(c,"target")),value(c,"kind"),value(c,"text"),"OP "+s.getTextName(),s.getLevel().getGameTime()));Residents.save();return "Attributed memory saved";
            })))))))
        );
    }
    private static int spawn(CommandContext<CommandSourceStack> c,String name) {
        return run(c,s->{BlockPos pos=s.getEntity()==null?s.getLevel().getSharedSpawnPos():BlockPos.containing(s.getPosition());var p=Residents.create(s.getServer(),s.getLevel(),pos,name);return "Spawned "+p.name()+" ["+p.id()+"]";});
    }
    private static int setPaused(CommandContext<CommandSourceStack> c,boolean paused) {
        return run(c,s->{if(!paused&&!Residents.hasCredentials())throw new IllegalArgumentException("Configure an LLM key first");var p=Residents.find(value(c,"name"));Residents.update(p.edit(null,null,paused,null,null));return paused?"Paused and cancelled current work":"Resumed";});
    }
}
