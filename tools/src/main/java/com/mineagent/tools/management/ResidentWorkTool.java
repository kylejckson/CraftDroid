package com.mineagent.tools.management;
import com.google.gson.*;
import com.mineagent.api.agent.tool.*;
import com.mineagent.api.entity.AgentPlayer;
import java.util.Map;
import java.util.function.Consumer;
/** Typed bounded programs, not unrestricted Java or shell execution. */
public final class ResidentWorkTool implements Tool {
    public String name(){return "resident_work";}
    public boolean dispatchesAsyncTask(){return true;}
    public String description(){return "Run a grounded local procedure without another LLM call between steps. Supply a goal and steps_json encoding 1-24 objects with tool, args, optional preconditions, expected_effects and on_failure (replan/abort/retry_once). Use only known exact coordinates/items and actual prerequisites. Actions still use normal inventory, world permissions, OP regions and executor verification. Stops on failure. A successful procedure may become a reusable skill. No arbitrary code, commands or nested control tools.";}
    public Map<String,Object> parameterSchema(){return Schema.object().string("goal","Short personal goal").string("steps_json","JSON array of tool/args steps; at most 24 steps and 32 KiB").build();}
    public void onServerCall(String id,JsonObject args,AgentPlayer p,Consumer<String> reply){
        var loop=com.mineagent.engine.task.TaskContext.agentLoop(p);
        if(loop==null){reply.accept(ToolArgs.errorJson("Loop unavailable"));return;}
        var result=loop.startResidentWork(ToolArgs.getString(args,"goal"),ToolArgs.getString(args,"steps_json"));
        var out=new JsonObject();if(result.accepted()){out.addProperty("success",true);out.addProperty("async",true);out.addProperty("skill_run_id",result.runId());out.addProperty("message",result.message());}
        else out.addProperty("error",result.message());reply.accept(out.toString());
    }
}
