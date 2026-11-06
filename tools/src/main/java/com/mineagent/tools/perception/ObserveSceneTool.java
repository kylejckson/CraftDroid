package com.mineagent.tools.perception;
import com.google.gson.JsonObject;
import com.mineagent.api.agent.tool.*;
import com.mineagent.api.entity.AgentPlayer;
import java.util.Map;
import java.util.function.Consumer;
public final class ObserveSceneTool implements Tool {
    public String name(){return "observe_scene";}
    public String description(){return "Request an on-demand perspective PNG reconstructed entirely inside the server from your eye position and facing. Simplified geometry and material colors; not exact mod textures. Image arrives as a later observation. 30-second cooldown; do not poll. Continue useful work or explicitly wait. Requires an image-capable OpenAI-compatible provider such as deepseek-flash.";}
    public Map<String,Object> parameterSchema(){return Schema.none();}
    public void onServerCall(String id,JsonObject args,AgentPlayer p,Consumer<String> reply){
        try {var loop=com.mineagent.engine.task.TaskContext.agentLoop(p);if(loop==null||!java.util.Set.of("deepseek","openai","openai-compatible").contains(loop.getProviderId()))throw new IllegalArgumentException("Image transport is not enabled for this provider");
            com.mineagent.engine.vision.SceneCapture.request(com.mineagent.engine.task.TaskContext.serverPlayer(p));reply.accept("{\"success\":true,\"status\":\"queued; image will arrive as an observation\"}");
        }catch(IllegalArgumentException e){reply.accept(ToolArgs.errorJson(e.getMessage()));}
    }
}
