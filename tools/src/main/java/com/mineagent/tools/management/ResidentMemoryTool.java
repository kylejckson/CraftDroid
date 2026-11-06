package com.mineagent.tools.management;

import com.google.gson.JsonObject;
import com.mineagent.api.agent.tool.*;
import com.mineagent.api.entity.AgentPlayer;
import com.mineagent.engine.resident.*;
import java.util.*;
import java.util.function.Consumer;

/** Personal notes can evolve freely; they never edit someone else's memories or policy. */
public final class ResidentMemoryTool implements Tool {
    public String name(){return "resident_memory";}
    public String description(){return "Recall or update your personal memories, preferences, relationships and goals. Use mode=recall or remember. For remember give subject=self, a UUID, or a resident name (omit it for yourself), an open-ended kind, and text. Distinguish opinions, observed events, promises and mutually accepted agreements. Never assume another person's feelings.";}
    public Map<String,Object> parameterSchema(){return Schema.object().string("mode","recall or remember").optionalString("query","Recall notes relevant to this topic").optionalString("subject","self, subject UUID, or resident name").optionalString("kind","Free-form memory category").optionalString("text","Personal note, at most 1000 characters").build();}
    public void onServerCall(String id,JsonObject args,AgentPlayer player,Consumer<String> reply){
        try {
            String mode=ToolArgs.getString(args,"mode");
            if("recall".equals(mode)){var out=new JsonObject();out.addProperty("memory",Residents.context(player.companionId(),ToolArgs.getString(args,"query","")));reply.accept(out.toString());return;}
            if(!"remember".equals(mode))throw new IllegalArgumentException("Unknown memory mode");
            String text=ToolArgs.getString(args,"text"),kind=ToolArgs.getString(args,"kind"),subject=ToolArgs.getString(args,"subject");
            if(text==null||text.isBlank()||text.length()>1000||kind==null||kind.isBlank()||kind.length()>64)throw new IllegalArgumentException("Supply bounded kind and text");
            Residents.remember(player.companionId(),new ResidentProfile.Memory(resolveSubject(subject,player.companionId(),player.companionName()),kind,text,"self reflection",((com.mineagent.engine.entity.CompanionEntity)player).serverPlayer().level().getGameTime()));
            reply.accept("{\"success\":true,\"message\":\"Personal note recorded; this does not establish another person's agreement\"}");
        }catch(IllegalArgumentException e){reply.accept(ToolArgs.errorJson(e.getMessage()));}
    }
    static UUID resolveSubject(String value,UUID self,String ownName){
        if(value==null||value.isBlank()||value.equalsIgnoreCase("self")||value.equalsIgnoreCase("me")||value.equalsIgnoreCase(ownName))return self;
        try{return UUID.fromString(value);}catch(IllegalArgumentException ignored){
            try{return Residents.find(value).id();}catch(IllegalArgumentException unknown){throw new IllegalArgumentException("Unknown subject. Use self, an exact UUID, or a known resident name. Your own name is "+ownName+"; CraftDroid is the software, not your identity.");}
        }
    }
}
