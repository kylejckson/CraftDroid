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
}
