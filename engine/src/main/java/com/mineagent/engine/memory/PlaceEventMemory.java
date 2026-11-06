package com.mineagent.engine.memory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**

 * Place Event Memory records what, where and when, inspired by Mr.Steve (ICLR 2025). Remember

 * resource sightings (cows, iron ore, trees) and events (creeper explosions, diamonds), retrieve

 * their last locations and avoid revisiting explored areas. Keep only the newest same-subject

 * record per location; favor important/rare resources during eviction; index directly without

 * full rebuilds; invalidate depleted POIs. Reads and writes use ConcurrentHashMap.

 */
public class PlaceEventMemory {

    /**

     * A place-event record: type (resource/entity/danger/structure/loot), subject, X/Y/Z,

     * dimension (overworld/nether/end), game timestamp in ticks, optional note, importance [0,1],

     * visit count and verified-presence flag.

     */
    public record PlaceEvent(
            String type,
            String subject,
            int x, int y, int z,
            String dimension,
            long timestamp,
            String note,
            float importance,
            int visitCount,
            boolean verified
    ) {
        /** Create a reinforced copy. */
        PlaceEvent reinforced(long newTimestamp) {
            return new PlaceEvent(type, subject, x, y, z, dimension, newTimestamp, note,
                    Math.min(1.0f, importance + 0.05f), visitCount + 1, true);
        }

        /** Mark as invalid. */
        PlaceEvent invalidated() {
            return new PlaceEvent(type, subject, x, y, z, dimension, timestamp, note,
                    importance * 0.3f, visitCount, false);
        }

        /** Return the location key. */
        String locationKey() {
            return subject.toLowerCase(Locale.ROOT) + ":" + x + "," + z
                    + ":" + normalizeDimension(dimension);
        }
    }

    private final List<PlaceEvent> events = Collections.synchronizedList(new ArrayList<>());

    /** Index by subject for faster retrieval. */
    private final Map<String, List<PlaceEvent>> bySubject = new ConcurrentHashMap<>();

    /** Location deduplication index: locationKey to PlaceEvent. */
    private final Map<String, PlaceEvent> locationIndex = new ConcurrentHashMap<>();

    /** Field-of-view coverage: explored chunk coordinates prevent repeated scanning. */
    private final Set<Long> exploredChunks = ConcurrentHashMap.newKeySet();

    /** Maximum number of memory entries. */
    private static final int MAX_EVENTS = 500;

    /** Calculate memory importance. */
    private float calculateImportance(String type, String subject, String note) {
        float score = 0.5f; // Base score.

        // Danger memories receive high importance.
        if ("danger".equals(type)) score += 0.35f;
        if ("loot".equals(type)) score += 0.25f;
        if ("structure".equals(type)) score += 0.15f;

        // Rare-resource bonus.
        String lower = (subject + " " + (note != null ? note : "")).toLowerCase();
        if (lower.contains("diamond") || lower.contains("emerald") ||
            lower.contains("ancient") || lower.contains("netherite") ||
            lower.contains("elytra") || lower.contains("beacon")) {
            score += 0.25f;
        } else if (lower.contains("iron") || lower.contains("gold") ||
                   lower.contains("redstone") || lower.contains("lapis") ||
                   lower.contains("village") || lower.contains("chest")) {
            score += 0.15f;
        }

        return Math.min(1.0f, score);
    }

    /** Record a place event with deduplication. */
    public void remember(String type, String subject,
                          int x, int y, int z, String dimension,
                          long timestamp, String note) {
        if (subject == null || subject.isBlank()) return;
        String normalizedDimension = normalizeDimension(dimension);
        String locKey = subject.toLowerCase(Locale.ROOT) + ":" + x + "," + z
                + ":" + normalizedDimension;

        // Check for the same subject at the same location.
        PlaceEvent existing = locationIndex.get(locKey);
        if (existing != null) {
            // Update the existing memory, increasing visits and importance.
            float newImportance = Math.max(existing.importance(),
                    calculateImportance(type, subject, note));
            PlaceEvent updated = new PlaceEvent(type, subject, x, y, z, normalizedDimension,
                    timestamp, note, newImportance, existing.visitCount() + 1, true);

            // Remove the old location entry and add the new record.
            removeFromIndices(existing);
            addToIndices(updated);
            return;
        }

        // Create a new memory.
        float importance = calculateImportance(type, subject, note);
        PlaceEvent e = new PlaceEvent(type, subject, x, y, z, normalizedDimension,
                timestamp, note, importance, 1, true);
        addToIndices(e);

        // Over capacity, evict the least important entry.
        while (events.size() > MAX_EVENTS) {
            PlaceEvent leastImportant = findLeastImportant();
            if (leastImportant != null) {
                removeFromIndices(leastImportant);
            } else {
                break;
            }
        }
    }

    /** Add to indexes; both reads and writes of bySubject lists must synchronize on the list. */
    private void addToIndices(PlaceEvent e) {
        events.add(e);
        List<PlaceEvent> list = bySubject.computeIfAbsent(e.subject().toLowerCase(Locale.ROOT),
                k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.add(e);
        }
        locationIndex.put(e.locationKey(), e);
    }

    /** Remove from indexes. */
    private void removeFromIndices(PlaceEvent e) {
        events.remove(e);
        locationIndex.remove(e.locationKey());
        List<PlaceEvent> list = bySubject.get(e.subject().toLowerCase(Locale.ROOT));
        if (list != null) {
            synchronized (list) {
                list.remove(e);
                if (list.isEmpty()) {
                    bySubject.remove(e.subject().toLowerCase(Locale.ROOT), list);
                }
            }
        }
    }

    /** Find the least important memory. */
    private PlaceEvent findLeastImportant() {
        if (events.isEmpty()) return null;

        PlaceEvent least = null;
        float minScore = Float.MAX_VALUE;

        synchronized (events) {
            for (PlaceEvent e : events) {
                // Combined score: importance*0.5 + visits*0.2 + time decay*0.2 + verification*0.1.
                long age = System.currentTimeMillis() - e.timestamp();
                float timeFactor = Math.max(0.1f, 1.0f - age / 7200000f); // Two-hour decay.
                float verifiedBonus = e.verified() ? 0.1f : 0f;
                float score = e.importance() * 0.5f +
                             Math.min(e.visitCount() / 10f, 1.0f) * 0.2f +
                             timeFactor * 0.2f +
                             verifiedBonus;

                if (score < minScore) {
                    minScore = score;
                    least = e;
                }
            }
        }
        return least;
    }

    /** Invalidate a resource location, for example after mining it. */
    public void invalidate(String subject, int x, int y, int z, String dimension) {
        if (subject == null || subject.isBlank()) return;
        String locKey = subject.toLowerCase(Locale.ROOT) + ":" + x + "," + z
                + ":" + normalizeDimension(dimension);
        PlaceEvent existing = locationIndex.get(locKey);
        if (existing != null) {
            PlaceEvent invalidated = existing.invalidated();
            removeFromIndices(existing);
            addToIndices(invalidated);
        }
    }

    /** Return the most recent PlaceEvent for a subject such as iron_ore, cow or village, or null. */
    public PlaceEvent findLastSeen(String subject) {
        if (subject == null || subject.isBlank()) return null;
        List<PlaceEvent> list = bySubject.get(subject.toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) return null;
        synchronized (list) {
            // Filter invalid entries and return the latest valid record.
            for (int i = list.size() - 1; i >= 0; i--) {
                PlaceEvent e = list.get(i);
                if (e.verified()) return e;
            }
            return list.get(list.size() - 1); // If all entries are invalid, return the latest one.
        }
    }

    /** Find all sightings of a subject within a dimension. */
    public List<PlaceEvent> findAll(String subject, String dimension) {
        if (subject == null || subject.isBlank()) return Collections.emptyList();
        List<PlaceEvent> list = bySubject.get(subject.toLowerCase(Locale.ROOT));
        if (list == null) return Collections.emptyList();
        String normalizedDimension = normalizeDimension(dimension);
        List<PlaceEvent> result = new ArrayList<>();
        synchronized (list) {
            for (PlaceEvent e : list) {
                if (e.dimension().equals(normalizedDimension) && e.verified()) {
                    result.add(e);
                }
            }
        }
        // Sort by importance.
        result.sort((a, b) -> Float.compare(b.importance(), a.importance()));
        return result;
    }

    /** Mark a chunk as explored. */
    public void markExplored(int chunkX, int chunkZ) {
        exploredChunks.add(chunkKey(chunkX, chunkZ));
    }

    /** Check whether a chunk has been explored. */
    public boolean isExplored(int chunkX, int chunkZ) {
        return exploredChunks.contains(chunkKey(chunkX, chunkZ));
    }

    /** Render only recent, relevant memories for the LLM to limit token usage. */
    public String summarizeForPrompt() {
        return summarizeForPrompt("", 10);
    }

    /** Return only locations semantically related to the active objective. */
    public String summarizeForPrompt(String query, int limit) {
        if (limit <= 0) return "";
        if (events.isEmpty()) return "(No location memories yet)";
        String needle = query == null ? "" : query.trim();

        StringBuilder sb = new StringBuilder();
        sb.append("## Location memories (recent resource/event sightings)\n");

        // Group by subject and keep only the latest valid record per group.
        Map<String, PlaceEvent> latest = new LinkedHashMap<>();
        synchronized (events) {
            for (PlaceEvent e : events) {
                if (e.verified()) {
                    latest.put(e.subject().toLowerCase(Locale.ROOT), e); // Later entries overwrite earlier ones.
                }
            }
        }

        List<ScoredPlace> sorted = latest.values().stream()
                .map(event -> new ScoredPlace(event, placeScore(needle, event)))
                .filter(value -> needle.isBlank() || value.score() > 0.0)
                .sorted(Comparator.comparingDouble(ScoredPlace::score).reversed()
                        .thenComparing(value -> value.event().timestamp(),
                                Comparator.reverseOrder()))
                .toList();
        if (sorted.isEmpty()) return "";

        int shown = 0;
        for (ScoredPlace value : sorted) {
            if (shown >= Math.min(10, limit)) break;
            PlaceEvent e = value.event();
            sb.append("- ").append(e.subject())
              .append(" at (").append(e.x()).append(",").append(e.y()).append(",").append(e.z()).append(")")
              .append(" [").append(e.dimension()).append("]");
            if (e.note() != null && !e.note().isBlank()) {
                sb.append(" — ").append(e.note());
            }
            sb.append("\n");
            shown++;
        }

        // Number of explored chunks.
        sb.append("(Explored ").append(exploredChunks.size()).append(" chunks)\n");

        return sb.toString();
    }

    private record ScoredPlace(PlaceEvent event, double score) {}

    private static double placeScore(String query, PlaceEvent event) {
        double semantic = query.isBlank() ? 0.25 : TextSimilarity.score(query,
                event.type() + " " + event.subject() + " "
                        + (event.note() == null ? "" : event.note()));
        if (!query.isBlank() && semantic <= 0.0) return 0.0;
        return semantic + event.importance() * 0.20
                + Math.min(0.08, event.visitCount() * 0.01);
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /** Clear all memories for testing or reset. */
    public void clear() {
        events.clear();
        bySubject.clear();
        locationIndex.clear();
        exploredChunks.clear();
    }

    // Persistence support.

    /** Export all memory events for persistence. */
    public List<PlaceEvent> exportAll() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    /** Import persisted events, replacing all current data. */
    public void importAll(List<PlaceEvent> imported) {
        events.clear();
        bySubject.clear();
        locationIndex.clear();
        if (imported == null) return;
        for (PlaceEvent e : imported) {
            if (e != null && e.subject() != null && e.dimension() != null) {
                String dimension = normalizeDimension(e.dimension());
                float importance = Float.isFinite(e.importance())
                        ? Math.max(0.0f, Math.min(1.0f, e.importance())) : 0.5f;
                addToIndices(new PlaceEvent(
                        e.type() != null ? e.type() : "event", e.subject(),
                        e.x(), e.y(), e.z(), dimension, e.timestamp(), e.note(),
                        importance, Math.max(0, e.visitCount()), e.verified()));
            }
        }
        // After import, evict by importance if over capacity.
        while (events.size() > MAX_EVENTS) {
            PlaceEvent leastImportant = findLeastImportant();
            if (leastImportant != null) {
                removeFromIndices(leastImportant);
            } else {
                break;
            }
        }
    }

    /** Return the number of memory entries. */
    public int size() {
        return events.size();
    }

    private static String normalizeDimension(String dimension) {
        if (dimension == null || dimension.isBlank() || "overworld".equals(dimension)) {
            return "minecraft:overworld";
        }
        if ("nether".equals(dimension)) return "minecraft:the_nether";
        if ("end".equals(dimension)) return "minecraft:the_end";
        return dimension;
    }
}
