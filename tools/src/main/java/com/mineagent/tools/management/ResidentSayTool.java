package com.mineagent.tools.management;

import com.google.gson.JsonObject;
import com.mineagent.api.agent.tool.*;
import com.mineagent.api.entity.AgentPlayer;
import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.entity.CompanionEntity;
import com.mineagent.engine.resident.Residents;
import java.util.Map;
import java.util.function.Consumer;

/** Public speech is an intentional world interaction, separate from model action preambles. */
public final class ResidentSayTool implements Tool {
    public String name(){return "resident_say";}
    public String description(){return "Say a short in-world message aloud to nearby players and residents. Use for conversation, requests, opinions and meaningful announcements. Ordinary assistant text is private and is not spoken. Avoid narrating routine tool use. Accepted messages may wait in the bounded speech queue.";}
    public Map<String,Object> parameterSchema(){return Schema.object().string("text","English dialogue, at most 600 characters; address neighbors by name when appropriate").build();}
    public void onServerCall(String id,JsonObject args,AgentPlayer player,Consumer<String> reply){
        String text=ToolArgs.getString(args,"text","");
        if(text.isBlank()||text.length()>600){reply.accept(ToolArgs.errorJson("Supply nonblank text of at most 600 characters"));return;}
        var state=MineAgentEngine.getCompanion(player.companionId()).orElse(null);
        if(!(player instanceof CompanionEntity resident)||!resident.isResident()||state==null||state.lifecycle.isPaused()||state.lifecycle.isDead()){
            reply.accept(ToolArgs.errorJson("Only a living active resident can speak"));return;
        }
        Residents.speak(resident,text);
        reply.accept("{\"success\":true,\"accepted\":true,\"message\":\"Speech accepted through the rate-limited nearby chat channel\"}");
    }
}
