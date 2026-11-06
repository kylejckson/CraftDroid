package com.mineagent.api.llm;

import java.util.List;
import java.util.Map;

/**
 * A chat message in the LLM conversation.
 */
public record ChatMessage(String role, String content, List<ToolCallRef> toolCalls, String toolCallId,
                          List<PngImage> images) {

    public ChatMessage {
        images = images == null ? List.of() : List.copyOf(images);
        if (!images.isEmpty() && !"user".equals(role))
            throw new IllegalArgumentException("Images are only permitted in user observations");
        if (images.size() > 2) throw new IllegalArgumentException("At most two images per observation");
    }

    public ChatMessage(String role, String content, List<ToolCallRef> tools, String callId) {
        this(role, content, tools, callId, List.of());
    }

    /** Small reconstructed scene images; never stored as part of ordinary text memories. */
    public record PngImage(String base64) {
        public PngImage {
            if (base64 == null || base64.length() > 2_800_000)
                throw new IllegalArgumentException("Scene image exceeds two MiB");
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            byte[] signature = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
            if (bytes.length < 8 || bytes.length > 2_097_152
                    || !java.util.Arrays.equals(signature, java.util.Arrays.copyOf(bytes, 8)))
                throw new IllegalArgumentException("Expected a bounded PNG image");
        }
        public static PngImage fromBytes(byte[] bytes) {
            return new PngImage(java.util.Base64.getEncoder().encodeToString(bytes));
        }
        public String dataUrl() { return "data:image/png;base64," + base64; }
    }

    /** Create a system message. */
    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, null, null);
    }

    /** Create a user message. */
    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, null);
    }

    /** Create an assistant message. */
    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, null, null);
    }

    /** Create an assistant message with tool calls. */
    public static ChatMessage assistantWithTools(List<ToolCallRef> toolCalls) {
        return new ChatMessage("assistant", null, toolCalls, null);
    }

    /** Create an assistant message with both content and tool calls. */
    public static ChatMessage assistant(String content, List<ToolCallRef> toolCalls) {
        return new ChatMessage("assistant", content, toolCalls, null);
    }

    /** Create a tool result message. */
    public static ChatMessage toolResult(String toolCallId, String content) {
        return new ChatMessage("tool", content, null, toolCallId);
    }

    /** A reference to a tool call in an assistant message. */
    public record ToolCallRef(String id, String name, String arguments) {}
}
