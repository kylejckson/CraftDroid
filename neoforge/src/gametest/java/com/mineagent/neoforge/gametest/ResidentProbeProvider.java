package com.mineagent.neoforge.gametest;

import com.google.gson.*;
import com.mineagent.api.llm.*;
import com.mineagent.api.llm.provider.*;
import com.mineagent.engine.llm.OpenAICompatibleProvider;
import net.minecraft.core.BlockPos;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;

/** Opt-in test transport. Live runs have a hard attempt cap and never log credentials or request bodies. */
final class ResidentProbeProvider implements LLMProvider {
    final boolean live;
    final int attemptLimit;
    final AtomicInteger attempts=new AtomicInteger(), images=new AtomicInteger(), completedImages=new AtomicInteger(), heardResidentInputs=new AtomicInteger();
    final List<Map<String,Object>> samples=new CopyOnWriteArrayList<>();
    final LLMProvider delegate=OpenAICompatibleProvider.deepseek();
    final UUID subject;
    final BlockPos[] positions;
    volatile boolean built,workIssued,memoryIssued,sceneIssued;
    volatile String failure="", imageDescription="", outcome="running";

    ResidentProbeProvider(boolean live,UUID subject,BlockPos[] positions){this(live,subject,positions,live?12:64);}
    ResidentProbeProvider(boolean live,UUID subject,BlockPos[] positions,int attemptLimit){this.live=live;this.subject=subject;this.positions=positions.clone();this.attemptLimit=attemptLimit;}
    @Override public LLMProvider newSession(){
        // A distinct monitor per resident matches production transports; budget and evidence remain shared.
        return new LLMProvider(){
            public String providerId(){return ResidentProbeProvider.this.providerId();}
            public String displayName(){return ResidentProbeProvider.this.displayName();}
            public String defaultBaseUrl(){return ResidentProbeProvider.this.defaultBaseUrl();}
            public boolean supportsModel(String model){return ResidentProbeProvider.this.supportsModel(model);}
            public List<String> defaultModels(){return ResidentProbeProvider.this.defaultModels();}
            public LLMResponse complete(LLMRequest request){return ResidentProbeProvider.this.complete(request);}
            public LLMResponse complete(String url,String key,String model,List<ChatMessage> messages,List<Map<String,Object>> tools,double temperature,int maxTokens,String effort){return ResidentProbeProvider.this.complete(url,key,model,messages,tools,temperature,maxTokens,effort);}
        };
    }
    public String providerId(){return "deepseek";}
    public String displayName(){return live?"Bounded live DeepSeek probe":"Scripted resident rehearsal (no API)";}
    public String defaultBaseUrl(){return "https://api.deepseek.com";}
    public boolean supportsModel(String model){return model.equals("deepseek-flash");}
    public List<String> defaultModels(){return List.of("deepseek-flash");}
    public LLMResponse complete(String url,String key,String model,List<ChatMessage> messages,List<Map<String,Object>> tools,double temperature,int maxTokens,String effort){
        return complete(new LLMRequest(url,key,model,messages,List.of(),tools,temperature,maxTokens,effort,null,null,null,java.time.Duration.ofSeconds(30),false));
    }
    @Override public LLMResponse complete(LLMRequest request){
        int attempt=attempts.incrementAndGet();
        if(attempt>attemptLimit){failure="request_budget_exhausted";throw new LLMProviderException("Resident probe request budget exhausted",null,false,null);}
        if(request.messages().stream().anyMatch(m->m.content()!=null&&m.content().contains("Heard resident ")))heardResidentInputs.incrementAndGet();
        boolean hasImage=request.messages().stream().anyMatch(m->!m.images().isEmpty());if(hasImage)images.incrementAndGet();
        long start=System.nanoTime();
        try {
            LLMResponse response=live?delegate.complete(request):scripted(request);
            if(hasImage)completedImages.incrementAndGet();
            if(completedImages.get()>0){
                if(response.choice()!=null&&response.choice().message()!=null&&response.choice().message().toolCalls()!=null)
                    for(var call:response.choice().message().toolCalls())if(call.name().equals("resident_say"))
                        imageDescription=JsonParser.parseString(call.arguments()).getAsJsonObject().get("text").getAsString().trim();
            }
            var row=new LinkedHashMap<String,Object>();row.put("attempt",attempt);row.put("paid",live);row.put("elapsed_ms",(System.nanoTime()-start)/1000000);row.put("image",hasImage);
            row.put("region_context",request.messages().stream().anyMatch(m->m.content()!=null&&m.content().contains("Nearby mandatory OP regions in ")));
            row.put("finish_reason",Objects.toString(response.finishReason(),""));
            row.put("recent_tool_results",request.messages().stream().filter(m->"tool".equals(m.role())).skip(Math.max(0,request.messages().stream().filter(m->"tool".equals(m.role())).count()-4)).map(m->Objects.toString(m.content(),"")).toList());
            if(response.usage()!=null){row.put("input_tokens",response.usage().promptTokens());row.put("output_tokens",response.usage().completionTokens());row.put("cached_input_tokens",response.usage().cachedPromptTokens());}
            if(response.choice()!=null&&response.choice().message()!=null){var calls=response.choice().message().toolCalls();row.put("tools",calls==null?List.of():calls.stream().map(ChatMessage.ToolCallRef::name).toList());
                if(calls!=null)row.put("tool_arguments",calls.stream().map(c->Map.of("id",c.id(),"tool",c.name(),"arguments",c.arguments())).toList());}
            samples.add(row);return response;
        }catch(RuntimeException error){
            Integer status=error instanceof LLMProviderException p?p.statusCode():null;
            boolean interrupted=Thread.currentThread().isInterrupted();
            for(Throwable cause=error;cause!=null;cause=cause.getCause())interrupted|=cause instanceof InterruptedException;
            if(interrupted){
                samples.add(Map.of("attempt",attempt,"paid",live,"cancelled",true,"elapsed_ms",(System.nanoTime()-start)/1000000));
                throw new LLMProviderException("Resident probe request superseded or stopped",status,false,null);
            }
            failure="provider_"+(status==null?error.getClass().getSimpleName():status);
            samples.add(Map.of("attempt",attempt,"paid",live,"failure",failure,"elapsed_ms",(System.nanoTime()-start)/1000000));
            // Do not relay a remote response body into Minecraft logs.
            throw new LLMProviderException("Resident probe failed: "+failure,status,false,null);
        }
    }
    private LLMResponse scripted(LLMRequest request){
        ChatMessage message;
        if(!workIssued){
            workIssued=true;var steps=new JsonArray();
            var craft=new JsonObject();craft.addProperty("tool","craft");var args=new JsonObject();args.addProperty("recipe_id","minecraft:oak_planks");args.addProperty("count",1);args.addProperty("use_table","inventory");craft.add("args",args);steps.add(craft);
            var build=new JsonObject();build.addProperty("tool","build");var buildArgs=new JsonObject();buildArgs.addProperty("mode","place");buildArgs.addProperty("block_type","minecraft:oak_planks");var cells=new JsonArray();for(var pos:positions){var cell=new JsonArray();cell.add(pos.getX());cell.add(pos.getY());cell.add(pos.getZ());cells.add(cell);}buildArgs.add("positions",cells);build.add("args",buildArgs);steps.add(build);
            var program=new JsonObject();program.addProperty("goal","Build a small three-plank community marker");program.addProperty("steps_json",steps.toString());message=call("work","resident_work",program);
        }else if(built&&!memoryIssued){
            memoryIssued=true;var args=new JsonObject();args.addProperty("mode","remember");args.addProperty("subject",subject.toString());args.addProperty("kind","promise");args.addProperty("text","I promised to stay away from the orchard and not build there.");message=call("memory","resident_memory",args);
        }else if(built&&!sceneIssued){sceneIssued=true;message=call("scene","observe_scene",new JsonObject());}
        else if(request.messages().stream().anyMatch(m->!m.images().isEmpty())){
            var args=new JsonObject();args.addProperty("text","I can see the block-color view of the marker and surrounding ground.");
            message=ChatMessage.assistant("PRIVATE_ACTION_PREAMBLE: image received; this must not become dialogue.",List.of(new ChatMessage.ToolCallRef("probe_speech","resident_say",args.toString())));
        }else message=ChatMessage.assistant("PRIVATE_ACTION_PREAMBLE: local procedure is continuing.");
        return new LLMResponse("resident-probe-"+attempts.get(),"deepseek-flash",new LLMResponse.Choice(0,message,message.toolCalls()==null?"stop":"tool_calls"),new LLMResponse.Usage(0,0,0),message.toolCalls()==null?"stop":"tool_calls");
    }
    private ChatMessage call(String id,String tool,JsonObject args){return ChatMessage.assistantWithTools(List.of(new ChatMessage.ToolCallRef("probe_"+id,tool,args.toString())));}
}
