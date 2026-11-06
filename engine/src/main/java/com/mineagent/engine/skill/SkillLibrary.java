package com.mineagent.engine.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mineagent.api.agent.tool.ToolRegistry;
import com.mineagent.engine.memory.TextSimilarity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**

 * Reusable parameterized action sequences inspired by Voyager (NVIDIA, TMLR 2024). Retrieve

 * reliable precedents for similar tasks, then let planning adapt parameters using current world

 * evidence. Skill reuse aims to reduce LLM calls over time. Each skill stores a name/description,

 * JSON tool-call sequence, success rate, invocation count and trigger conditions.

 */
public class SkillLibrary {

    private static final int MAX_SEQUENCE_STEPS = 24;
    private static final Set<String> CONTROL_TOOLS = Set.of(
            "execute_skill", "load_skill", "list_learned_skills",
            "query_extra_tools", "explore_mechanism", "todowrite",
            "task_status", "task_stop", "coordinate_team");
    private static final Set<String> OBSERVATION_TOOLS = Set.of(
            "look_around", "scan_blocks", "scan_nearby_entities",
            "get_self_status", "get_owner_status", "get_world_info",
            "resolve_need", "lookup_recipe", "inspect_block",
            "inspect_block_storage", "inspect_gui", "recall_memory",
            "locate_structure", "locate_biome");

    /** A reusable skill. */
    public record Skill(
            String name,
            String description,
            String triggerCondition,
            String actionSequence, // JSON tool-call sequence.
            double successRate,
            int invocations,
            long createdTick
    ) {}

    private final Map<String, Skill> skills = new ConcurrentHashMap<>();
    /**
     * Register a skill; update an existing same-name entry when the new evidence has a higher
     * success rate.
     */
    public void register(String name, String description, String triggerCondition,
                          String actionSequence, boolean success) {
        String normalizedName = name == null ? "" : name.trim();
        String validatedSequence = validateSequence(actionSequence);
        if (normalizedName.isBlank() || vagueLegacyGoal(normalizedName, description,
                triggerCondition) || validatedSequence == null) return;
        Skill existing = skills.get(normalizedName);
        if (existing != null) {
            // Update success rate and invocation count.
            int newInvocations = existing.invocations() + 1;
            double newRate = (existing.successRate() * existing.invocations() + (success ? 1 : 0))
                    / newInvocations;
            // A failed adaptation is evidence about the skill's reliability,
            // but it must not overwrite the last verified parameter trace.
            Skill updated = new Skill(
                    normalizedName, success ? description : existing.description(),
                    success ? triggerCondition : existing.triggerCondition(),
                    success ? validatedSequence : existing.actionSequence(),
                    newRate, newInvocations, existing.createdTick());
            skills.put(normalizedName, updated);
        } else {
            Skill skill = new Skill(
                    normalizedName, description, triggerCondition, validatedSequence,
                    success ? 1.0 : 0.0, 1, System.currentTimeMillis());
            skills.put(normalizedName, skill);
        }

    }

    /**

     * Retrieve at most three skills ranked by similarity to a task description such as "mine

     * stone". Uses lexical matching without an embedding model; only strong matches with success

     * rate above 0.7 are returned.

     */
    public List<Skill> retrieve(String taskDescription) {
        return relevant(taskDescription, 3);
    }

    /** Retrieve a bounded relevant subset; a blank query returns the best skills. */
    public List<Skill> relevant(String taskDescription, int limit) {
        if (limit <= 0 || skills.isEmpty()) return Collections.emptyList();
        String query = taskDescription == null ? "" : taskDescription.trim();
        return skills.values().stream()
                .filter(skill -> skill.successRate() > 0.7)
                .map(skill -> Map.entry(skill, query.isBlank() ? skill.successRate()
                        : TextSimilarity.score(query, skill.name() + " "
                                + skill.description() + " " + skill.triggerCondition())))
                .filter(entry -> query.isBlank() || entry.getValue() > 0.0)
                .sorted(Comparator.<Map.Entry<Skill, Double>>comparingDouble(
                                Map.Entry::getValue).reversed()
                        .thenComparing(entry -> entry.getKey().successRate(),
                                Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().invocations(),
                                Comparator.reverseOrder()))
                .limit(Math.min(8, limit))
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Record a skill's execution outcome. */
    public void recordResult(String name, boolean success) {
        Skill existing = skills.get(name);
        if (existing != null) {
            int newInvocations = existing.invocations() + 1;
            double newRate = (existing.successRate() * existing.invocations() + (success ? 1 : 0))
                    / newInvocations;
            Skill updated = new Skill(
                    existing.name(), existing.description(), existing.triggerCondition(),
                    existing.actionSequence(), newRate, newInvocations, existing.createdTick());
            skills.put(name, updated);

            // Low-success skills are not recommended, but remain stored as failure lessons.
            // retrieve() already filters by successRate > 0.7.
        }
    }

    /**
     * Register a multi-step skill (a sequence of tool calls).
     *
     * <p>This is the Voyager-style skill library core: when the LLM
     * successfully completes a multi-step task (e.g. "mine 3 iron →
     * smelt → craft pickaxe → equip"), the entire sequence is saved
     * as a reusable skill. Next time a similar task comes up, the LLM
     * can call {@code load_skill} to retrieve and replay it, saving
     * multiple LLM round-trips.
     *
     * @param name skill name (unique key)
     * @param description human-readable description for retrieval
     * @param triggerCondition when to use this skill
     * @param actionSequenceJson JSON array of tool calls, e.g.
     *     {@code [{"tool":"auto_mine","args":"..."},{"tool":"smelt","args":"..."}]}
     * @param success whether the sequence completed successfully
     */
    public void registerSequence(String name, String description, String triggerCondition,
                                   String actionSequenceJson, boolean success) {
        register(name, description, triggerCondition, actionSequenceJson, success);
    }

    /**
     * Retrieve the full action sequence JSON for a skill by name.
     * Used by the load_skill tool to replay a skill.
     *
     * @return the action sequence JSON, or null if skill not found
     */
    public String getActionSequence(String skillName) {
        Skill skill = skills.get(skillName);
        return skill != null ? skill.actionSequence() : null;
    }

    /** Resolve a learned skill through the same name used by load_skill. */
    public Optional<Skill> get(String skillName) {
        return Optional.ofNullable(skills.get(skillName));
    }

    /**
     * Install a mechanism adapter without pretending that compilation was a
     * successful body invocation.  Normal {@link #register} updates Bayesian
     * reliability counters; using it during every memory load used to inflate
     * confidence without executing anything.
     */
    public void upsertVerifiedAdaptation(String name, String description,
                                         String triggerCondition,
                                         String actionSequence) {
        String normalizedName = name == null ? "" : name.trim();
        String validated = validateSequence(actionSequence);
        if (normalizedName.isBlank() || validated == null) return;
        Skill existing = skills.get(normalizedName);
        skills.put(normalizedName, new Skill(normalizedName,
                description == null ? "" : description,
                triggerCondition == null ? "" : triggerCondition,
                validated, existing == null ? 1.0 : existing.successRate(),
                existing == null ? 1 : existing.invocations(),
                existing == null ? System.currentTimeMillis() : existing.createdTick()));
    }

    /** Remove a generated adapter as soon as its evidence or environment is stale. */
    public boolean remove(String name) {
        return name != null && skills.remove(name) != null;
    }

    /** Stable snapshot used by memory persistence. */
    public List<Skill> exportAll() {
        return skills.values().stream()
                .sorted(Comparator.comparing(Skill::name))
                .toList();
    }

    /** Replace the library with validated persisted entries and rebuild its index. */
    public void importAll(Collection<Skill> restored) {
        skills.clear();
        if (restored == null) return;
        for (Skill skill : restored) {
            if (skill == null || skill.name() == null || skill.name().isBlank()
                    || skill.actionSequence() == null || skill.actionSequence().isBlank()) {
                continue;
            }
            String validatedSequence = validateSequence(skill.actionSequence());
            if (vagueLegacyGoal(skill.name(), skill.description(), skill.triggerCondition())
                    || validatedSequence == null) continue;
            Skill normalized = new Skill(skill.name().trim(),
                    skill.description() == null ? "" : skill.description(),
                    skill.triggerCondition() == null ? "" : skill.triggerCondition(),
                    validatedSequence,
                    Math.max(0.0, Math.min(1.0, skill.successRate())),
                    Math.max(1, skill.invocations()), Math.max(0L, skill.createdTick()));
            skills.put(normalized.name(), normalized);
        }
    }

    /** Generate a skill-list summary for the LLM. */
    public String summarizeForPrompt() {
        if (skills.isEmpty()) return "(No learned skills yet)";

        StringBuilder sb = new StringBuilder();
        sb.append("## Learned skills (reuse directly without replanning)\n");
        for (Skill skill : skills.values()) {
            if (skill.successRate() > 0.5) {
                sb.append("- **").append(skill.name()).append("**: ")
                  .append(skill.description())
                  .append(" (success rate ").append(String.format("%.0f%%", skill.successRate() * 100))
                  .append(", used ").append(skill.invocations()).append(" times)\n");
            }
        }
        sb.append("Prefer reusing existing skills for similar tasks.\n");
        return sb.toString();
    }

    /** Number of skills in the library. */
    public int size() {
        return skills.size();
    }

    /** Return all learned skills for LLM queries. */
    public java.util.Collection<Skill> allSkills() {
        return skills.values();
    }

    /**
     * Validate persisted traces before the runtime can replay them. A sequence
     * must contain a real action; query-only transcripts merely repeat costly
     * discovery and were the main source of the old skill-file pollution.
     */
    private static String validateSequence(String sequence) {
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(sequence);
        } catch (RuntimeException malformed) {
            return null;
        }
        if (!parsed.isJsonArray()) return null;
        JsonArray steps = parsed.getAsJsonArray();
        if (steps.isEmpty() || steps.size() > MAX_SEQUENCE_STEPS) return null;
        boolean registryReady = ToolRegistry.size() > 0;
        boolean hasAction = false;
        for (JsonElement element : steps) {
            if (!element.isJsonObject()) return null;
            JsonObject step = element.getAsJsonObject();
            if (!step.has("tool") || !step.get("tool").isJsonPrimitive()) return null;
            String tool;
            try {
                tool = step.get("tool").getAsString().trim().toLowerCase(Locale.ROOT);
            } catch (RuntimeException malformedTool) {
                return null;
            }
            if (!tool.matches("[a-z][a-z0-9_]{0,63}") || CONTROL_TOOLS.contains(tool)) {
                return null;
            }
            if (registryReady && ToolRegistry.get(tool).isEmpty()) return null;
            if (step.has("args") && !step.get("args").isJsonObject()) return null;
            if (!OBSERVATION_TOOLS.contains(tool)) hasAction = true;
        }
        return hasAction ? steps.toString() : null;
    }

    private static boolean vagueLegacyGoal(String name, String description,
                                            String triggerCondition) {
        return "general_task".equalsIgnoreCase(name == null ? "" : name.trim())
                || "general_task".equalsIgnoreCase(
                description == null ? "" : description.trim())
                || "general_task".equalsIgnoreCase(
                triggerCondition == null ? "" : triggerCondition.trim());
    }

}
