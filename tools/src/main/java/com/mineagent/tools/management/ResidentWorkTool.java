package com.mineagent.tools.management;

import com.google.gson.*;
import com.mineagent.api.agent.tool.*;
import com.mineagent.api.entity.AgentPlayer;
import com.mineagent.engine.skill.SkillRuntime;
import java.util.*;
import java.util.function.Consumer;

/** Typed bounded procedures, retaining the legacy JSON-string input for compatibility. */
public final class ResidentWorkTool implements Tool {
    public String name(){return "resident_work";}
    public boolean dispatchesAsyncTask(){return true;}
    public String description(){
        return "Run 1-24 checked physical steps locally without an LLM call between steps. Supply goal and a steps array, e.g. "
                + "[{\"tool\":\"craft\",\"args\":{\"recipe_id\":\"minecraft:oak_planks\",\"count\":1}}]. "
                + "Each step needs tool and args. Normally omit preconditions and expected_effects: the action executor already verifies actual crafting, placement and task outcomes. "
                + "Only add semantic conditions when you have observed their exact subject/predicate keys; invented facts can prevent otherwise successful work from verifying. "
                + "Conditions are arrays of objects, never prose or string arrays. Stops on failure; learns only verified successes. "
                + "Normal inventory, reach, permissions and OP protection always apply. No arbitrary code, commands or nested control tools. Legacy steps_json is also accepted instead of steps.";
    }
    public Map<String,Object> parameterSchema(){
        Map<String,Object> condition=Map.of("type","object","properties",Map.of(
                "subject",Map.of("type","string","description","Exact observed semantic subject key"),
                "predicate",Map.of("type","string","description","Exact observed semantic predicate key"),
                "value",Map.of("type","string"),
                "comparison",Map.of("type","string","enum",List.of("equals","at_least","at_most","present")),
                "minimum_confidence",Map.of("type","number","minimum",0,"maximum",1)),
                "required",List.of("subject","predicate","value"),"additionalProperties",false);
        Map<String,Object> conditions=Map.of("type","array","items",condition,"maxItems",16,
                "description","Optional: only exact previously observed fact keys; omit when unnecessary");
        Map<String,Object> step=Map.of("type","object","properties",Map.of(
                "tool",Map.of("type","string","enum",SkillRuntime.proposedActionTools().stream().sorted().toList()),
                "args",Map.of("type","object","additionalProperties",true),
                "preconditions",conditions,"expected_effects",conditions,
                "on_failure",Map.of("type","string","enum",List.of("replan","abort","retry_once"))),
                "required",List.of("tool","args"),"additionalProperties",false);
        return Map.of("type","object","properties",Map.of(
                "goal",Map.of("type","string","maxLength",256),
                "steps",Map.of("type","array","items",step,"minItems",1,"maxItems",24),
                "steps_json",Map.of("type","string","maxLength",32768,"description","Legacy alternative; use steps instead when possible")),
                "required",List.of("goal"),"anyOf",List.of(Map.of("required",List.of("steps")),Map.of("required",List.of("steps_json"))));
    }
    static String sequence(JsonObject args){
        if(args.has("steps")&&!args.get("steps").isJsonNull()){
            if(args.has("steps_json")&&!args.get("steps_json").isJsonNull())throw new IllegalArgumentException("Supply steps or steps_json, not both");
            if(!args.get("steps").isJsonArray())throw new IllegalArgumentException("steps must be a JSON array");
            var array=args.getAsJsonArray("steps");
            if(array.isEmpty()||array.size()>24||array.toString().length()>32768)throw new IllegalArgumentException("Procedure exceeds step or size bounds");
            return array.toString();
        }
        return ToolArgs.getString(args,"steps_json");
    }
    public void onServerCall(String id,JsonObject args,AgentPlayer player,Consumer<String> reply){
        try {
            String sequence=sequence(args);
            var loop=com.mineagent.engine.task.TaskContext.agentLoop(player);
            if(loop==null){reply.accept(ToolArgs.errorJson("Loop unavailable"));return;}
            var result=loop.startResidentWork(ToolArgs.getString(args,"goal"),sequence);
            var out=new JsonObject();
            if(result.accepted()){out.addProperty("success",true);out.addProperty("async",true);out.addProperty("skill_run_id",result.runId());out.addProperty("message",result.message());}
            else out.addProperty("error",result.message());reply.accept(out.toString());
        }catch(IllegalArgumentException error){reply.accept(ToolArgs.errorJson(error.getMessage()));}
    }
}
