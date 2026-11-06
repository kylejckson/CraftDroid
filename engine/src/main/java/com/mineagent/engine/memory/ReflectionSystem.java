package com.mineagent.engine.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**

 * Extract actionable lessons from failures, inspired by Reflexion (NeurIPS 2023) and Voyager.

 * Micro reflection suggests parameter corrections after tool failures; meso reflection adjusts

 * task strategy; macro reflection adjusts recurring behavior patterns. Classify structured errors

 * with general patterns rather than fixed vocabulary, and generate context-specific advice.

 */
public class ReflectionSystem {

    /**

     * Reflection record: level (micro/meso/macro), trigger (failed tool/task), failure context,

     * lesson, timestamp and applicability scope.

     */
    public record Reflection(
            String level,
            String trigger,
            String context,
            String lesson,
            long timestamp,
            String applicability
    ) {}

    /** Default maximum number of reflections. */
    private static final int DEFAULT_MAX_REFLECTIONS = 30;

    private final Deque<Reflection> reflections = new ArrayDeque<>();
    private final ConcurrentMap<String, Integer> failurePatterns = new ConcurrentHashMap<>();
    private final int maxReflections;

    public ReflectionSystem() {
        this(DEFAULT_MAX_REFLECTIONS);
    }

    public ReflectionSystem(int maxReflections) {
        this.maxReflections = maxReflections;
    }

    /** Record a tool-call failure using general pattern matching rather than fixed keywords. */
    public void recordToolFailure(String toolName, String params, String error, String context) {
        String pattern = categorizeFailure(toolName, error);
        failurePatterns.merge(pattern, 1, Integer::sum);

        String lesson = generateMicroLesson(toolName, error, params);
        String trigger = toolName + "(" + truncate(params, 40) + ")";

        addReflection(new Reflection(
                "micro",
                trigger,
                truncate(context, 60),
                lesson,
                System.currentTimeMillis(),
                toolName
        ));
    }

    /** Record a task failure. */
    public void recordTaskFailure(String taskDescription, String reason, String context) {
        String lesson = generateMesoLesson(taskDescription, reason);

        addReflection(new Reflection(
                "meso",
                taskDescription,
                truncate(context, 60),
                lesson,
                System.currentTimeMillis(),
                "task"
        ));
    }

    /** Record a recurring behavior-pattern failure. */
    public void recordPatternFailure(String pattern, int failureCount, String context) {
        String lesson = "Repeated failure pattern detected: " + pattern +
                "（" + failureCount + " times). Consider changing strategy or asking for help.";

        addReflection(new Reflection(
                "macro",
                pattern,
                truncate(context, 60),
                lesson,
                System.currentTimeMillis(),
                "pattern"
        ));
    }

    // AgentLoop compatibility methods.

    /** Record an AgentLoop failure from task description, tool name and failure reason. */
    public void recordFailure(String taskDesc, String toolName, String failReason) {
        recordToolFailure(toolName, taskDesc, failReason, taskDesc);
    }

    /** Record an AgentLoop task failure from its description, attempted plan and failure reason. */
    public void recordFailedTask(String taskDesc, String attemptedPlan, String failReason) {
        recordTaskFailure(taskDesc, failReason, attemptedPlan);
    }

    /**

     * Return recent lessons for the AgentLoop prompt without requiring a match. The old

     * empty-string match prevented micro/meso lessons from reaching the prompt.

     */
    public String summarizeForPrompt() {
        List<Reflection> recent;
        synchronized (reflections) {
            recent = new ArrayList<>(reflections);
        }
        if (recent.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("## Lessons to remember\n");
        // The three most recent lessons; newest last.
        int start = Math.max(0, recent.size() - 3);
        for (int i = start; i < recent.size(); i++) {
            sb.append("- ").append(recent.get(i).lesson()).append("\n");
        }
        return sb.toString();
    }

    /** Format failure memories relevant to the current task for AgentLoop prompts. */
    public String formatFailuresForPrompt(String currentTask) {
        List<Reflection> relevant = recallRelevantFailures(currentTask, "");
        if (relevant.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("## Relevant failure memories\n");
        for (Reflection r : relevant) {
            sb.append("- Task: ").append(truncate(r.trigger(), 30))
              .append(" | Lesson: ").append(r.lesson()).append("\n");
        }
        return sb.toString();
    }

    /** Add a reflection with synchronization because persistence exportAll can run concurrently. */
    private void addReflection(Reflection r) {
        synchronized (reflections) {
            reflections.addLast(r);
            while (reflections.size() > maxReflections) {
                reflections.pollFirst();
            }
        }
    }

    /** Find reflections relevant to the current task using flexible general matching. */
    public List<Reflection> recallRelevantFailures(String taskDescription, String toolName) {
        List<Reflection> relevant = new ArrayList<>();
        String lowerTask = taskDescription != null ? taskDescription.toLowerCase() : "";

        synchronized (reflections) {
            for (Reflection r : reflections) {
                // Guard against missing fields from deserialization or exceptional paths.
                if (r.applicability() == null || r.trigger() == null || r.level() == null) {
                    continue;
                }
                // Failures involving the same tool type.
                if (r.applicability().equals(toolName)) {
                    relevant.add(r);
                }
                // Match failure patterns with general matching rather than fixed vocabulary.
                else if (isContextMatch(lowerTask, r.trigger().toLowerCase())) {
                    relevant.add(r);
                }
                // Macro reflections are always relevant.
                else if ("macro".equals(r.level())) {
                    relevant.add(r);
                }
            }
        }

        // Return at most the latest three.
        return relevant.size() > 3 ?
                new ArrayList<>(relevant.subList(relevant.size() - 3, relevant.size())) :
                relevant;
    }

    /** Generate a prompt-ready reflection summary. */
    public String summarizeForPrompt(String currentTask) {
        List<Reflection> relevant = recallRelevantFailures(currentTask, "");
        if (relevant.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("## Lessons to remember\n");
        for (Reflection r : relevant) {
            sb.append("- ").append(r.lesson()).append("\n");
        }
        return sb.toString();
    }

    // Private helpers.

    /** Match context using general patterns rather than hard-coded keywords. */
    private boolean isContextMatch(String task, String trigger) {
        // Find shared words of at least three characters.
        String[] taskWords = task.split("\\s+");
        for (String word : taskWords) {
            if (word.length() >= 3 && trigger.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** Classify failure patterns by error structure rather than particular words. */
    private String categorizeFailure(String toolName, String error) {
        if (error == null) return toolName + ":unknown";
        String lower = error.toLowerCase();

        // General error classification by error type.
        // 1. Argument or validation errors.
        if (lower.contains("invalid") || lower.contains("illegal") ||
            lower.contains("missing") || lower.contains("required") ||
            lower.contains("null") || lower.contains("empty")) {
            return toolName + ":invalid_params";
        }
        // 2. Missing resources or objects.
        if (lower.contains("not found") || lower.contains("no such") ||
            lower.contains("doesn't exist") || lower.contains("cannot find") ||
            lower.contains("unable to locate")) {
            return toolName + ":not_found";
        }
        // 3. Permission or access errors.
        if (lower.contains("cannot") || lower.contains("can't") ||
            lower.contains("denied") || lower.contains("forbidden") ||
            lower.contains("not allowed") || lower.contains("unreachable")) {
            return toolName + ":access_denied";
        }
        // 4. Timeouts or performance problems.
        if (lower.contains("timeout") || lower.contains("timed out") ||
            lower.contains("too slow") || lower.contains("expired")) {
            return toolName + ":timeout";
        }
        // 5. Unsatisfied states or conditions.
        if (lower.contains("already") || lower.contains("not ready") ||
            lower.contains("busy") || lower.contains("locked") ||
            lower.contains("in use")) {
            return toolName + ":state_conflict";
        }

        return toolName + ":unknown";
    }

    /** Generate micro-level advice based on the error type. */
    private String generateMicroLesson(String toolName, String error, String params) {
        String lower = error != null ? error.toLowerCase() : "";

        // Argument error.
        if (lower.contains("invalid") || lower.contains("illegal") ||
            lower.contains("missing") || lower.contains("required")) {
            return "Tool " + toolName + " has invalid arguments. Check the format and required fields. Current arguments: " +
                    truncate(params, 30);
        }
        // Target not found.
        if (lower.contains("not found") || lower.contains("no such") ||
            lower.contains("cannot find")) {
            return "Target not found. Use look_around to scan the surroundings and confirm the target exists before retrying " + toolName + ".";
        }
        // Access or permission problem.
        if (lower.contains("cannot") || lower.contains("can't") ||
            lower.contains("unreachable") || lower.contains("denied")) {
            return "Cannot execute " + toolName + ". Check whether the target is reachable, or try moving closer.";
        }
        // Timeout.
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return toolName + " timed out. The target may be too far away or the surroundings too complex; split the task or change strategy.";
        }
        // State conflict.
        if (lower.contains("already") || lower.contains("busy") ||
            lower.contains("locked")) {
            return "Target state conflict. Wait for the current operation to finish or choose another target.";
        }

        // General advice.
        return "Tool " + toolName + " failed: " + truncate(error, 40) +
                ". Check the arguments and confirm the target state.";
    }

    /** Generate meso-level lessons. */
    private String generateMesoLesson(String taskDescription, String reason) {
        String lowerReason = reason != null ? reason.toLowerCase() : "";

        // Task decomposition problem.
        if (lowerReason.contains("too complex") || lowerReason.contains("too big") ||
            lowerReason.contains("overwhelming")) {
            return "Task '" + truncate(taskDescription, 25) + "' is too complex. Try dividing it into smaller subtasks.";
        }
        // Insufficient resources.
        if (lowerReason.contains("lack") || lowerReason.contains("insufficient") ||
            lowerReason.contains("not enough") || lowerReason.contains("missing")) {
            return "Insufficient resources. Gather the required materials first, or use find_block to locate them.";
        }
        // Path or navigation problem.
        if (lowerReason.contains("stuck") || lowerReason.contains("cannot reach") ||
            lowerReason.contains("blocked") || lowerReason.contains("unreachable")) {
            return "Navigation blocked. Use look_around to find another route, or clear the obstacles.";
        }
        // Environment or external factor.
        if (lowerReason.contains("danger") || lowerReason.contains("hostile") ||
            lowerReason.contains("threat")) {
            return "The surroundings are dangerous. Remove threats or find a safe area before continuing.";
        }

        // General advice.
        return "Task '" + truncate(taskDescription, 25) + "' failed: " +
                truncate(reason, 40) + ". Consider changing strategy or dividing the task.";
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    /** Return the number of reflections. */
    public int size() {
        synchronized (reflections) {
            return reflections.size();
        }
    }

    /** Clear all reflections. */
    public void clear() {
        synchronized (reflections) {
            reflections.clear();
        }
        failurePatterns.clear();
    }

    // Persistence support.

    /** Export all reflections for persistence. */
    public List<Reflection> exportAll() {
        synchronized (reflections) {
            return new ArrayList<>(reflections);
        }
    }

    /** Export failure-pattern statistics for persistence. */
    public ConcurrentMap<String, Integer> exportPatterns() {
        return new ConcurrentHashMap<>(failurePatterns);
    }

    /** Import persisted reflections and failure patterns, replacing current data. */
    public void importAll(List<Reflection> imported, Map<String, Integer> patterns) {
        synchronized (reflections) {
            reflections.clear();
            if (imported != null) {
                for (Reflection r : imported) {
                    // Filter incomplete records from older or manually edited files.
                    // Prevent null trigger/applicability values from causing retrieval NPEs.
                    if (r != null && r.lesson() != null && r.trigger() != null
                            && r.applicability() != null && r.level() != null) {
                        reflections.addLast(r);
                    }
                }
                while (reflections.size() > maxReflections) {
                    reflections.pollFirst();
                }
            }
        }
        failurePatterns.clear();
        if (patterns != null) {
            failurePatterns.putAll(patterns);
        }
    }
}
