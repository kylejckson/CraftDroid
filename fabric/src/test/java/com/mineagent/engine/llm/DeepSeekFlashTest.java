package com.mineagent.engine.llm;

import com.google.gson.JsonObject;
import com.mineagent.api.llm.ChatMessage;
import com.mineagent.api.llm.model.ThinkingEffortSpec;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class DeepSeekFlashTest {
    private final OpenAICompatibleProvider provider = OpenAICompatibleProvider.deepseek();

    @Test void officialAliasDisablesThinkingAndPreservesRealLowEffort() {
        for (String off : List.of("off", "none")) {
            JsonObject body = new JsonObject();
            provider.injectThinkingEffort(body, "deepseek-flash", off);
            assertEquals("disabled", body.getAsJsonObject("thinking").get("type").getAsString());
            assertFalse(body.has("reasoning_effort"));
        }
        JsonObject low = new JsonObject();
        provider.injectThinkingEffort(low, "deepseek-flash", "low");
        assertEquals("low", low.get("reasoning_effort").getAsString());
        assertTrue(ThinkingEffortSpec.forModel("deepseek-flash").canDisable());
    }

    @Test void sceneImageUsesUserContentBlocksWhileTextMessagesStayCompatible() {
        byte[] png = {(byte)137,80,78,71,13,10,26,10};
        var scene = new ChatMessage("user", "Inspect this reconstructed view", null, null,
                List.of(ChatMessage.PngImage.fromBytes(png)));
        var body = provider.buildRequestBody("deepseek-flash", List.of(ChatMessage.system("English"), scene),
                List.of(), .7, 256, false, null);
        var messages = body.getAsJsonArray("messages");
        assertEquals("English", messages.get(0).getAsJsonObject().get("content").getAsString());
        var parts = messages.get(1).getAsJsonObject().getAsJsonArray("content");
        assertEquals("image_url", parts.get(1).getAsJsonObject().get("type").getAsString());
        assertTrue(parts.get(1).getAsJsonObject().getAsJsonObject("image_url").get("url").getAsString().startsWith("data:image/png;base64,"));
        assertThrows(IllegalArgumentException.class, () -> new ChatMessage("system", "bad", null, null, scene.images()));
        assertThrows(IllegalArgumentException.class, () -> ChatMessage.PngImage.fromBytes(new byte[]{1,2}));
    }

    @Test void actualTransportSendsNonThinkingVisionAndParsesTools() throws Exception {
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var captured=new java.util.concurrent.atomic.AtomicReference<JsonObject>();
        server.createContext("/v1/chat/completions",exchange->{
            captured.set(com.google.gson.JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
            byte[] response="{\"id\":\"mock\",\"model\":\"deepseek-flash\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"sign1\",\"type\":\"function\",\"function\":{\"name\":\"inspect_sign\",\"arguments\":\"{}\"}}]},\"finish_reason\":\"tool_calls\"}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();
        });server.start();
        try {
            var image=ChatMessage.PngImage.fromBytes(com.mineagent.engine.skin.SkinPng.generated(java.util.UUID.randomUUID()));
            var result=provider.complete("http://127.0.0.1:"+server.getAddress().getPort(),"test-only-key","deepseek-flash",List.of(new ChatMessage("user","Look",null,null,List.of(image))),List.of(),.7,128,"none");
            assertEquals("disabled",captured.get().getAsJsonObject("thinking").get("type").getAsString());
            assertEquals("image_url",captured.get().getAsJsonArray("messages").get(0).getAsJsonObject().getAsJsonArray("content").get(1).getAsJsonObject().get("type").getAsString());
            assertEquals("tool_calls",result.finishReason());assertEquals("inspect_sign",result.choice().message().toolCalls().getFirst().name());
        }finally{server.stop(0);}
    }
}
