package com.mineagent.engine.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**

 * Dynamic importance evaluation: relevance varies with context (diamonds while mining, health in

 * combat, materials while building). Learn through LLM importance annotations, strong emotion

 * changes and task-failure associations. Lower decay from 0.01 to 0.002 preserves learned

 * weights; multi-field features reduce collisions; task context adjusts relevance.

 */
public class ImportanceEvaluator {

    /** Event feature to weight [0,1]. Features combine the tool name with meaningful result tokens. */
    private final Map<String, Float> featureWeights = new ConcurrentHashMap<>();

    /** Weight adjustment: +0.15 per learning event, -0.002 per decay tick. */
    private static final float LEARN_STEP = 0.15f;
    private static final float DECAY_STEP = 0.002f; // Reduce decay from 0.01 to 0.002.
    private static final float MAX_WEIGHT = 1.0f;
    private static final float MIN_WEIGHT = 0.0f;
    private static final float DEFAULT_WEIGHT = 0.5f;
    private static final float IMPORTANT_THRESHOLD = 0.65f;

    /** Learning count indicates whether enough evidence is available. */
    private final AtomicInteger learnCount = new AtomicInteger(0);

    /** Current task context for dynamic importance adjustments. */
    private volatile String currentTaskContext = "";

    /** Set task context; temporarily favor related features, such as ores during mining. */
    public void setTaskContext(String taskDescription) {
        this.currentTaskContext = taskDescription != null ? taskDescription.toLowerCase() : "";
    }

    /** Extract multi-field features from tool results to reduce collisions. */
    private String extractFeature(String toolName, String resultContent) {
        if (resultContent == null) resultContent = "";

        // Combine multiple fields into a more precise feature.
        String type = extractJsonField(resultContent, "type");
        String name = extractJsonField(resultContent, "name");
        String block = extractJsonField(resultContent, "block");
        String entity = extractJsonField(resultContent, "entity");
        String item = extractJsonField(resultContent, "item");
        String target = extractJsonField(resultContent, "target");

        // Prefer the most specific field in combined features.
        StringBuilder feature = new StringBuilder(toolName).append(":");

        if (block != null && !block.isEmpty()) {
            feature.append("block=").append(block);
        } else if (entity != null && !entity.isEmpty()) {
            feature.append("entity=").append(entity);
        } else if (item != null && !item.isEmpty()) {
            feature.append("item=").append(item);
        } else if (type != null && !type.isEmpty()) {
            feature.append("type=").append(type);
            if (name != null && !name.isEmpty()) {
                feature.append("|name=").append(name);
            }
        } else if (target != null && !target.isEmpty()) {
            feature.append("target=").append(target);
        } else {
            // Fall back to a content hash when no fields match.
            // Hash the first 20 characters to avoid overly specific long-text features.
            String content = resultContent.length() > 20 ?
                    resultContent.substring(0, 20) : resultContent;
            feature.append("content=").append(content.hashCode() % 1000);
        }

        return feature.toString();
    }

    /** Extract a field value from a JSON result. */
    private String extractJsonField(String json, String field) {
        if (json == null || json.isBlank()) return null;
        String key = "\"" + field + "\":\"";
        int idx = json.toLowerCase().indexOf(key);
        if (idx >= 0) {
            int start = idx + key.length();
            int end = json.indexOf('"', start);
            if (end > start) {
                return json.substring(start, end).toLowerCase();
            }
        }
        return null;
    }

    /** Return a tool result's importance weight, dynamically adjusted for task context. */
    public float getWeight(String toolName, String resultContent) {
        String feature = extractFeature(toolName, resultContent);
        float baseWeight = featureWeights.getOrDefault(feature, DEFAULT_WEIGHT);

        // Temporarily boost features relevant to the current task.
        if (!currentTaskContext.isEmpty()) {
            String lowerFeature = feature.toLowerCase();
            // Match task keywords.
            if (isTaskRelevant(lowerFeature)) {
                // Temporarily boost weight by 20%.
                return Math.min(MAX_WEIGHT, baseWeight * 1.2f);
            }
        }

        return baseWeight;
    }

    /** Determine whether a feature is relevant to the current task. */
    private boolean isTaskRelevant(String feature) {
        if (currentTaskContext.isEmpty()) return false;

        // Extract task keywords.
        String[] taskKeywords = currentTaskContext.split("\\s+");
        for (String keyword : taskKeywords) {
            if (keyword.length() >= 3 && feature.contains(keyword)) {
                return true;
            }
        }

        // Match common task types.
        if (currentTaskContext.contains("\u6316") || currentTaskContext.contains("mine") ||
            currentTaskContext.contains("\u91c7\u77ff")) {
            return feature.contains("ore") || feature.contains("block") ||
                   feature.contains("stone") || feature.contains("diamond") ||
                   feature.contains("iron") || feature.contains("coal");
        }
        if (currentTaskContext.contains("\u5efa") || currentTaskContext.contains("build") ||
            currentTaskContext.contains("\u653e\u7f6e")) {
            return feature.contains("block") || feature.contains("place") ||
                   feature.contains("build") || feature.contains("craft");
        }
        if (currentTaskContext.contains("\u6218\u6597") || currentTaskContext.contains("fight") ||
            currentTaskContext.contains("\u653b\u51fb")) {
            return feature.contains("entity") || feature.contains("mob") ||
                   feature.contains("damage") || feature.contains("health");
        }

        return false;
    }

    /** A tool result is important when its weight exceeds the threshold. */
    public boolean isImportant(String toolName, String resultContent) {
        return getWeight(toolName, resultContent) >= IMPORTANT_THRESHOLD;
    }

    /** Learn from LLM result annotations: importance may be high, medium or low. */
    public void learnFromLLMAnnotation(String toolName, String resultContent, String importanceLabel) {
        if (importanceLabel == null) return;
        String feature = extractFeature(toolName, resultContent);
        float target;
        switch (importanceLabel.toLowerCase()) {
            case "high", "\u91cd\u8981", "critical" -> target = 0.9f;
            case "medium", "\u4e2d\u7b49", "normal" -> target = 0.5f;
            case "low", "\u4f4e", "trivial" -> target = 0.2f;
            default -> { return; }
        }
        float current = featureWeights.getOrDefault(feature, DEFAULT_WEIGHT);
        // Move toward the target with a learning rate of 0.3.
        float updated = current + (target - current) * 0.3f;
        featureWeights.put(feature, clamp(updated));
        learnCount.incrementAndGet();
    }

    /** Increase feature weights when events trigger strong emotion changes. */
    public void learnFromEmotion(String toolName, String resultContent, float emotionDelta) {
        // EmotionState changes by at most about 0.09 per event.
        // Negative changes receive a 1.5x multiplier,
        // so the maximum is about 0.135. The previous 0.2 threshold was unreachable,
        // making emotion learning dead code. Use 0.05 to match AgentLoop's
        // learning trigger (emotionDelta > 0.05f).
        if (Math.abs(emotionDelta) < 0.05f) return;  // Ignore emotion changes that are too small.
        String feature = extractFeature(toolName, resultContent);
        float current = featureWeights.getOrDefault(feature, DEFAULT_WEIGHT);
        // Stronger emotion changes produce larger weight increases.
        // A single emotion change is small (0.05-0.13); using it directly would underweight learning.
        // Multiply by three for a clearer effect, still bounded by MAX_WEIGHT.
        float boost = Math.abs(emotionDelta) * 3.0f * LEARN_STEP;
        // Give negative emotions, such as being attacked, extra weight for survival relevance.
        if (emotionDelta < 0) boost *= 1.5f;
        featureWeights.put(feature, clamp(current + boost));
        learnCount.incrementAndGet();
    }

    /** Learn from failures by marking contributing tool-result features important. */
    public void learnFromFailure(String toolName, String resultContent) {
        String feature = extractFeature(toolName, resultContent);
        float current = featureWeights.getOrDefault(feature, DEFAULT_WEIGHT);
        featureWeights.put(feature, clamp(current + LEARN_STEP));
        learnCount.incrementAndGet();
    }

    /**

     * Periodically decay weights toward neutral 0.5 so features do not remain at 1.0 forever.

     * Recommended cadence: every 200 ticks. Lowering the step from 0.01 to 0.002 preserves

     * learning longer; a 0.01 decrease takes about five seconds under the intended cadence.

     */
    public void decayAll() {
        if (featureWeights.isEmpty()) return;
        featureWeights.replaceAll((k, v) -> {
            if (v > DEFAULT_WEIGHT) return Math.max(DEFAULT_WEIGHT, v - DECAY_STEP);
            if (v < DEFAULT_WEIGHT) return Math.min(DEFAULT_WEIGHT, v + DECAY_STEP);
            return v;
        });
    }

    /**

     * Summarize the five highest-weight features for the system prompt so the LLM sees what the

     * AI considers important.

     */
    public String summarizeForPrompt() {
        if (featureWeights.isEmpty() || learnCount.get() < 3) {
            return "";  // Do not inject a summary before enough learning data exists.
        }
        StringBuilder sb = new StringBuilder();
        featureWeights.entrySet().stream()
                .filter(e -> e.getValue() >= IMPORTANT_THRESHOLD)
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(e -> {
                    if (sb.length() > 0) sb.append(", ");
                    // Strip the tool-name prefix to render the feature subject.
                    String feature = e.getKey();
                    int colonIdx = feature.indexOf(':');
                    if (colonIdx >= 0 && colonIdx < feature.length() - 1) {
                        sb.append(feature.substring(colonIdx + 1));
                    } else {
                        sb.append(feature);
                    }
                });
        if (sb.length() == 0) return "";
        return "What you consider important: " + sb;
    }

    public int learnCount() { return learnCount.get(); }
    public int featureCount() { return featureWeights.size(); }

    // Persistence support.

    /** Export learned feature weights for persistence. */
    public Map<String, Float> exportWeights() {
        return new ConcurrentHashMap<>(featureWeights);
    }

    /** Import persisted feature weights, replacing current data. */
    public void importWeights(Map<String, Float> weights, int savedLearnCount) {
        featureWeights.clear();
        if (weights != null) {
            weights.forEach((feature, weight) -> {
                if (feature != null && !feature.isBlank()
                        && weight != null && Float.isFinite(weight)) {
                    featureWeights.put(feature, clamp(weight));
                }
            });
        }
        learnCount.set(Math.max(0, savedLearnCount));
    }

    private static float clamp(float v) {
        return Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, v));
    }
}
