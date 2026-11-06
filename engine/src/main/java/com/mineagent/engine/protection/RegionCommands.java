package com.mineagent.engine.protection;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.*;
import static net.minecraft.commands.Commands.*;
import static com.mojang.brigadier.arguments.StringArgumentType.*;

/** Selections retain their dimension and cannot silently combine corners from different worlds. */
public final class RegionCommands {
    private record Point(String dimension,BlockPos pos) {}
    private static final Map<String,Point> FIRST=new HashMap<>(),SECOND=new HashMap<>();
    private RegionCommands() {}
    public static void clear(){FIRST.clear();SECOND.clear();}
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        var root=literal("zone").executes(c->{c.getSource().sendSuccess(()->Component.literal("zone pos1/pos2 [x y z], save <name> <enter,break,place,use>, list, remove <name>"),false);return 1;});
        for(boolean first:new boolean[]{true,false})root.then(literal(first?"pos1":"pos2")
            .executes(c->select(c.getSource(),first,BlockPos.containing(c.getSource().getPosition())))
            .then(argument("pos",BlockPosArgument.blockPos()).executes(c->select(c.getSource(),first,BlockPosArgument.getBlockPos(c,"pos")))));
        root.then(literal("save").then(argument("region",word()).then(argument("deny",word()).executes(c->{
            var s=c.getSource();String key=identity(s);
            try {
                var a=FIRST.get(key);var b=SECOND.get(key);
                if(a==null||b==null||!a.dimension().equals(b.dimension()))throw new IllegalArgumentException("Select two corners in the same dimension");
                var actions=EnumSet.noneOf(RegionRules.Action.class);
                for(String flag:getString(c,"deny").split(","))actions.add(RegionRules.Action.valueOf(flag.replace("deny-","").toUpperCase(Locale.ROOT)));
                var r=new RegionRules.Region(getString(c,"region"),a.dimension(),a.pos().getX(),a.pos().getY(),a.pos().getZ(),b.pos().getX(),b.pos().getY(),b.pos().getZ(),actions);
                AgentProtection.RULES.put(r);AgentProtection.save();s.sendSuccess(()->Component.literal("Saved region "+r.name()+"; active actions are checked immediately"),true);return 1;
            }catch(Exception e){s.sendFailure(Component.literal(e.getMessage()==null?"Invalid zone":e.getMessage()));return 0;}
        }))));
        root.then(literal("list").executes(c->{c.getSource().sendSuccess(()->Component.literal(AgentProtection.RULES.regions().toString()),false);return 1;}));
        root.then(literal("remove").then(argument("region",word()).executes(c->{try{boolean removed=AgentProtection.RULES.remove(getString(c,"region"));AgentProtection.save();return removed?1:0;}catch(Exception e){c.getSource().sendFailure(Component.literal("Region could not be saved"));return 0;}})));
        d.register(literal("craftdroid").requires(s->s.hasPermission(2)).then(root));
    }
    private static String identity(CommandSourceStack s){return s.getEntity()==null?"console":s.getEntity().getUUID().toString();}
    private static int select(CommandSourceStack s,boolean first,BlockPos p){(first?FIRST:SECOND).put(identity(s),new Point(s.getLevel().dimension().location().toString(),p.immutable()));s.sendSuccess(()->Component.literal("Selected "+p.toShortString()),false);return 1;}
}
