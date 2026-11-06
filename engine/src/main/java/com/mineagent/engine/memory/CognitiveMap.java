package com.mineagent.engine.memory;

import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**

 * Hippocampus-inspired spatial memory. Record resource, hazard and structure POIs; query exact

 * categories or prefixes and nearby locations; render compact prompt summaries. Nearby

 * same-category points merge and strengthen. Importance preserves rare resources and hazards,

 * verification invalidates depleted points, and visit counts/recency guide eviction. Inspired by

 * place cells and grid cells for spatial reasoning. Reads and writes use ConcurrentHashMap.

 */
public class CognitiveMap {

    /** Default maximum number of POIs. */
    private static final int DEFAULT_MAX_POIS = 200;

    /** Deduplication radius in blocks; nearby same-category POIs represent one place. */
    private static final int DEDUP_RADIUS = 16;

    /**

     * Point of interest with category, display label, X/Y/Z, first-discovery timestamp in

     * milliseconds, importance [0,1], visit count, verified-presence flag and last-reinforcement

     * time in milliseconds.

     */
    public record PointOfInterest(
            String category,
            String label,
            int x, int y, int z,
            String dimension,
            long timestamp,
            float importance,
            int visitCount,
            boolean verified,
            long lastVisitTime
    ) {
        /** Create a reinforced copy. */
        PointOfInterest reinforced(long now) {
            return new PointOfInterest(category, label, x, y, z, dimension, timestamp,
                    Math.min(1.0f, importance + 0.05f), visitCount + 1, true, now);
        }

        /** Mark as invalid. */
        PointOfInterest invalidated() {
            return new PointOfInterest(category, label, x, y, z, dimension, timestamp,
                    importance * 0.3f, visitCount, false, lastVisitTime);
        }
    }

    /** POI storage: key to PointOfInterest. */
    private final Map<String, PointOfInterest> pois = new ConcurrentHashMap<>();
    private final int maxPOIs;

    public CognitiveMap() {
        this(DEFAULT_MAX_POIS);
    }

    public CognitiveMap(int maxPOIs) {
        if (maxPOIs <= 0) throw new IllegalArgumentException("maxPOIs must be positive");
        this.maxPOIs = maxPOIs;
    }

    /**

     * Calculate importance from category and label: hazards, rare resources, common resources,

     * then entities.

     */
    private float calculateImportance(String category, String label) {
        float score = 0.5f; // Base score.

        String lowerCat = category != null ? category.toLowerCase(Locale.ROOT) : "";
        String lowerLabel = label != null ? label.toLowerCase(Locale.ROOT) : "";
        String combined = lowerCat + " " + lowerLabel;

        // Category base score.
        if (lowerCat.startsWith("hazard")) score += 0.3f;
        else if (lowerCat.startsWith("structure")) score += 0.15f;
        else if (lowerCat.startsWith("resource")) score += 0.1f;

        // Rarity bonus.
        if (combined.contains("diamond") || combined.contains("emerald")
                || combined.contains("ancient") || combined.contains("netherite")
                || combined.contains("\u94bb\u77f3")) {
            score += 0.25f;
        } else if (combined.contains("iron") || combined.contains("gold")
                || combined.contains("village") || combined.contains("chest")
                || combined.contains("redstone") || combined.contains("lapis")) {
            score += 0.15f;
        }

        return Math.min(1.0f, score);
    }

    /**

     * Record a POI with deduplication. Reinforce an existing same-category POI within 16 blocks

     * rather than creating another; inputs are position, category, label and discovery timestamp

     * in milliseconds.

     */
    public void recordPoi(BlockPos pos, String category, String label, long timestamp) {
        recordPoi(pos, category, label, "minecraft:overworld", timestamp);
    }

    public void recordPoi(BlockPos pos, String category, String label,
                          String dimension, long timestamp) {
        Objects.requireNonNull(pos, "pos");
        String cat = category != null ? category.toLowerCase(Locale.ROOT) : "event:unknown";
        String dim = normalizeDimension(dimension);

        // Merge nearby same-category observations into an existing POI.
        String existingKey = findNearbyKey(
                cat, dim, pos.getX(), pos.getZ(), DEDUP_RADIUS);
        if (existingKey != null) {
            PointOfInterest existing = pois.get(existingKey);
            if (existing != null) {
                pois.put(existingKey, existing.reinforced(timestamp));
            }
            return;
        }

        float importance = calculateImportance(cat, label);
        PointOfInterest poi = new PointOfInterest(cat, label,
                pos.getX(), pos.getY(), pos.getZ(),
                dim,
                timestamp, importance, 1, true, timestamp);
        pois.put(poiKey(cat, dim, pos.getX(), pos.getZ()), poi);

        evictIfNeeded();
    }

    /** Return verified POIs within a block radius of X/Z, sorted by descending importance. */
    public List<PointOfInterest> findNearby(int x, int z, int radius) {
        return findNearby(x, z, radius, null);
    }

    public List<PointOfInterest> findNearby(int x, int z, int radius, String dimension) {
        String dim = dimension == null ? null : normalizeDimension(dimension);
        long r2 = (long) radius * radius;
        List<PointOfInterest> result = new ArrayList<>();
        for (PointOfInterest poi : pois.values()) {
            if (!poi.verified()) continue;
            if (dim != null && !dim.equals(poi.dimension())) continue;
            long dx = poi.x() - x;
            long dz = poi.z() - z;
            if (dx * dx + dz * dz <= r2) {
                result.add(poi);
            }
        }
        result.sort((a, b) -> Float.compare(b.importance(), a.importance()));
        return result;
    }

    /**

     * Return verified POIs sorted by importance. Support exact categories such as

     * resource:iron_ore and prefixes such as resource.

     */
    public List<PointOfInterest> findByCategory(String category) {
        return findByCategory(category, null);
    }

    public List<PointOfInterest> findByCategory(String category, String dimension) {
        String query = category != null ? category.toLowerCase(Locale.ROOT) : "";
        String dim = dimension == null ? null : normalizeDimension(dimension);
        List<PointOfInterest> result = new ArrayList<>();
        for (PointOfInterest poi : pois.values()) {
            if (!poi.verified()) continue;
            if (dim != null && !dim.equals(poi.dimension())) continue;
            String cat = poi.category();
            // Exact or prefix match (resource matches resource:iron_ore).
            if (cat.equals(query) || cat.startsWith(query + ":")) {
                result.add(poi);
            }
        }
        result.sort((a, b) -> Float.compare(b.importance(), a.importance()));
        return result;
    }

    /** Invalidate nearby POIs, for example after mining a resource. */
    public void invalidateNearby(int x, int z, int radius) {
        invalidateNearby(x, z, radius, null);
    }

    public void invalidateNearby(int x, int z, int radius, String dimension) {
        String dim = dimension == null ? null : normalizeDimension(dimension);
        long r2 = (long) radius * radius;
        for (Map.Entry<String, PointOfInterest> entry : pois.entrySet()) {
            PointOfInterest poi = entry.getValue();
            if (dim != null && !dim.equals(poi.dimension())) continue;
            long dx = poi.x() - x;
            long dz = poi.z() - z;
            if (dx * dx + dz * dz <= r2) {
                entry.setValue(poi.invalidated());
            }
        }
    }

    /**

     * Render a compact spatial-memory summary for the LLM, grouped by category and limited to

     * verified important POIs.

     */
    public String summarizeForPrompt() {
        return summarizeForPrompt("", 8);
    }

    /** Return a bounded spatial-memory packet relevant to the active goal. */
    public String summarizeForPrompt(String query, int limit) {
        if (limit <= 0) return "";
        String needle = query == null ? "" : query.trim();
        List<ScoredPoi> verified = pois.values().stream()
                .filter(PointOfInterest::verified)
                .map(poi -> new ScoredPoi(poi, spatialScore(needle, poi)))
                .filter(value -> needle.isBlank() || value.score() > 0.0)
                .sorted(Comparator.comparingDouble(ScoredPoi::score).reversed()
                        .thenComparing(value -> value.poi().lastVisitTime(),
                                Comparator.reverseOrder()))
                .toList();
        if (verified.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("## Spatial memory\n");
        int shown = 0;
        Map<String, Integer> perTopCategory = new HashMap<>();
        for (ScoredPoi value : verified) {
            if (shown >= Math.min(8, limit)) break;
            PointOfInterest poi = value.poi();
            // Show at most three entries per broad category (resource/hazard/...) to preserve diversity.
            String topCat = poi.category().split(":")[0];
            int count = perTopCategory.getOrDefault(topCat, 0);
            if (count >= 2) continue;
            sb.append("- ").append(poi.label())
              .append(" at (").append(poi.x()).append(",")
              .append(poi.y()).append(",").append(poi.z()).append(")")
              .append(" [").append(poi.dimension()).append("]");
            if (poi.visitCount() > 2) {
                sb.append(" [confirmed x").append(poi.visitCount()).append("]");
            }
            sb.append("\n");
            perTopCategory.put(topCat, count + 1);
            shown++;
        }
        return sb.toString();
    }

    private record ScoredPoi(PointOfInterest poi, double score) {}

    private static double spatialScore(String query, PointOfInterest poi) {
        double semantic = query.isBlank() ? 0.25 : TextSimilarity.score(query,
                poi.category() + " " + poi.label());
        if (!query.isBlank() && semantic <= 0.0) return 0.0;
        return semantic + poi.importance() * 0.20
                + Math.min(0.08, poi.visitCount() * 0.01);
    }

    // Persistence support.

    /** Export all POIs for persistence. */
    public List<PointOfInterest> exportAll() {
        return new ArrayList<>(pois.values());
    }

    /** Import persisted POIs, replacing all current data. */
    public void importAll(List<PointOfInterest> imported) {
        pois.clear();
        if (imported == null) return;
        for (PointOfInterest poi : imported) {
            if (poi != null && poi.category() != null) {
                String dimension = normalizeDimension(poi.dimension());
                float importance = Float.isFinite(poi.importance())
                        ? Math.max(0.0f, Math.min(1.0f, poi.importance())) : 0.5f;
                PointOfInterest normalized = new PointOfInterest(
                        poi.category().toLowerCase(Locale.ROOT), poi.label(),
                        poi.x(), poi.y(), poi.z(), dimension, poi.timestamp(),
                        importance, Math.max(0, poi.visitCount()), poi.verified(),
                        poi.lastVisitTime());
                pois.put(poiKey(normalized.category(), normalized.dimension(),
                        normalized.x(), normalized.z()), normalized);
            }
        }
        evictIfNeeded();
    }

    // Private helpers.

    /** Generate the POI storage key. */
    private String poiKey(String category, String dimension, int x, int z) {
        return dimension + ":" + category + ":" + x + "," + z;
    }

    /** Find an existing same-category POI within the radius. */
    private String findNearbyKey(String category, String dimension,
                                 int x, int z, int radius) {
        long r2 = (long) radius * radius;
        for (Map.Entry<String, PointOfInterest> entry : pois.entrySet()) {
            PointOfInterest poi = entry.getValue();
            if (!poi.category().equals(category) || !poi.dimension().equals(dimension)) continue;
            long dx = poi.x() - x;
            long dz = poi.z() - z;
            if (dx * dx + dz * dz <= r2) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static String normalizeDimension(String dimension) {
        if (dimension == null || dimension.isBlank() || "overworld".equals(dimension)) {
            return "minecraft:overworld";
        }
        if ("nether".equals(dimension)) return "minecraft:the_nether";
        if ("end".equals(dimension)) return "minecraft:the_end";
        return dimension;
    }

    /**

     * Evict by combined score: importance*0.4 + visit frequency*0.2 + recency*0.2 +

     * verification*0.2.

     */
    private void evictIfNeeded() {
        while (pois.size() > maxPOIs) {
            String leastKey = null;
            float minScore = Float.MAX_VALUE;
            long now = System.currentTimeMillis();

            for (Map.Entry<String, PointOfInterest> entry : pois.entrySet()) {
                PointOfInterest poi = entry.getValue();
                long age = now - poi.lastVisitTime();
                float recencyFactor = Math.max(0.1f, 1.0f - age / 7200000f); // Two-hour decay.
                float verifiedBonus = poi.verified() ? 0.2f : 0f;
                float score = poi.importance() * 0.4f
                        + Math.min(poi.visitCount() / 10f, 1.0f) * 0.2f
                        + recencyFactor * 0.2f
                        + verifiedBonus;

                if (score < minScore) {
                    minScore = score;
                    leastKey = entry.getKey();
                }
            }

            if (leastKey != null) {
                pois.remove(leastKey);
            } else {
                break;
            }
        }
    }

    /** Return the POI count. */
    public int size() {
        return pois.size();
    }

    /** Clear all data. */
    public void clear() {
        pois.clear();
    }
}
