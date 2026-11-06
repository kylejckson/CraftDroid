package com.mineagent.engine.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**

 * Cache LLM decisions for similar situations to reduce calls and latency. Keys combine quantized

 * position, health/hunger tiers, nearest entity type and command hash; values store assistant

 * content and tool_calls. Entries expire to prevent stale behavior; see TTL_MS for the current

 * lifetime.

 */
public class DecisionCache {

    /** Cached entry. */
    private record CacheEntry(
            String decisionContent,
            String toolCallsJson,
            long expiresAt
    ) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**

     * Cache lifetime in milliseconds. Reduced from 30 to 10 seconds to cover repeated actions

     * such as mining without repeating old decisions after the player has left.

     */
    private static final long TTL_MS = 10_000;

    /** Maximum number of cached entries. */
    private static final int MAX_SIZE = 500;

    /**

     * Build a cache key from AI-player X/Y/Z coordinates quantized to four blocks, health tier

     * (0-4 / 5-9 / 10-14 / 15-20), hunger tier, nearest entity type or null, and player command

     * or null.

     */
    public static String buildKey(int posX, int posY, int posZ,
                                    int healthTier, int hungerTier,
                                    String nearestEntity, String command) {
        // Quantize position to four-block precision.
        int qx = posX >> 2; // posX / 4
        int qy = posY >> 2;
        int qz = posZ >> 2;
        return qx + "," + qy + "," + qz + "|"
                + healthTier + "|" + hungerTier + "|"
                + (nearestEntity != null ? nearestEntity : "-") + "|"
                + (command != null ? command.hashCode() : "-");
    }

    /** Return a cached decision, or null on a miss or expiry. */
    public CacheEntry get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) cache.remove(key);
            return null;
        }
        return entry;
    }

    /** Retrieve cached decision text. */
    public String getDecision(String key) {
        CacheEntry entry = get(key);
        return entry != null ? entry.decisionContent() : null;
    }

    /** Retrieve cached tool-call JSON. */
    public String getToolCalls(String key) {
        CacheEntry entry = get(key);
        return entry != null ? entry.toolCallsJson() : null;
    }

    /** Store a decision in the cache. */
    public void put(String key, String decisionContent, String toolCallsJson) {
        // Enforce the capacity limit.
        if (cache.size() >= MAX_SIZE) {
            // First remove expired entries.
            cache.entrySet().removeIf(e -> e.getValue().isExpired());
            // If still too large, remove half the entries.
            if (cache.size() >= MAX_SIZE) {
                int half = cache.size() / 2;
                var iter = cache.keySet().iterator();
                for (int i = 0; i < half && iter.hasNext(); i++) {
                    iter.next();
                    iter.remove();
                }
            }
        }
        cache.put(key, new CacheEntry(
                decisionContent, toolCallsJson,
                System.currentTimeMillis() + TTL_MS));
    }

    /** Clear the cache. */
    public void clear() {
        cache.clear();
    }

    /** Current number of cached entries. */
    public int size() {
        return cache.size();
    }

    /** Remove expired entries. */
    public void cleanExpired() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
