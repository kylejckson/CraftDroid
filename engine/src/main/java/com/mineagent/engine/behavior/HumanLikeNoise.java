package com.mineagent.engine.behavior;

import java.util.Random;

/**

 * Behavior noise adds human-like timing and motion. Human players vary action intervals, turn

 * smoothly, hesitate in complex situations and occasionally correct small mistakes. Inspired by

 * anti-cheat detection research. Stateless, thread-safe helpers provide Gaussian interval jitter,

 * ease-in-out turns, probabilistic hesitation and optional recoverable mistakes.

 */
public final class HumanLikeNoise {

    private HumanLikeNoise() {}

    private static final Random RNG = new Random();

    /** Standard deviation of Gaussian interval jitter (15%). */
    private static final double INTERVAL_JITTER_STD = 0.15;

    /** Probability of a thinking pause (3%). */
    private static final double HESITATE_PROB = 0.03;

    /** Probability of a small mistake (1.5%). */
    private static final double MISTAKE_PROB = 0.015;

    /** Add 15% Gaussian jitter to a base interval in milliseconds; return the adjusted interval. */
    public static long jitterInterval(long baseIntervalMs) {
        double noise = RNG.nextGaussian() * INTERVAL_JITTER_STD;
        double adjusted = baseIntervalMs * (1.0 + noise);
        return Math.max(1, (long) adjusted);
    }

    /**

     * Map linear progress in [0, 1] to ease-in-out progress. Unlike constant 15-degree-per-tick

     * turning, this smooths the start and end of a turn.

     */
    public static float easeInOutTurn(float linearProgress) {
        // Classic ease-in-out: 3*t^2 - 2*t^3 (smoothstep).
        float p = clamp(linearProgress, 0f, 1f);
        return p * p * (3f - 2f * p);
    }

    /**

     * Return this tick's turn angle from the remaining difference, wrapped to [-180, 180]

     * degrees, and the per-tick maximum. Turn quickly at a distance and fine-tune near the

     * target.

     */
    public static float adaptiveTurn(float remainingDiff, float maxPerTick) {
        float absDiff = Math.abs(remainingDiff);
        if (absDiff < 0.5f) return 0;

        // Turn at full speed far from the target and slow down nearby.
        // Use full speed when the angular difference exceeds 45 degrees.
        // Use ease-out deceleration below 45 degrees.
        float speedRatio;
        if (absDiff > 45f) {
            speedRatio = 1.0f;
        } else {
            // Square-root ease-out, (diff/45)^0.5, gives more natural deceleration.
            float t = absDiff / 45f;
            speedRatio = (float) Math.sqrt(t);
        }

        float turn = Math.signum(remainingDiff) * Math.min(maxPerTick, absDiff * speedRatio);
        // Add tiny jitter to simulate imperfect hand stability.
        turn += (RNG.nextFloat() - 0.5f) * 0.8f;
        return turn;
    }

    /**

     * Return true about 3% of the time; callers should insert a 200-500 ms thinking pause in

     * complex situations.

     */
    public static boolean maybeHesitate() {
        return RNG.nextDouble() < HESITATE_PROB;
    }

    /** Duration of a hesitation pause (200-500 ms). */
    public static int hesitationDurationMs() {
        return 200 + RNG.nextInt(301); // 200-500ms
    }

    /**

     * Return true about 1.5% of the time. Callers may introduce a small mistake, such as an extra

     * step, then immediately correct it to avoid mechanically perfect execution.

     */
    public static boolean maybeMistake() {
        return RNG.nextDouble() < MISTAKE_PROB;
    }

    /** Add small movement-speed jitter to a base speed in [0, 1]; return speed with 5% variation. */
    public static float jitterSpeed(float baseSpeed) {
        // Zero is an intentional stop while pathing turns in place. Adding
        // positive noise to zero makes the companion walk in its old facing
        // direction and can push it off an edge before alignment is safe.
        if (baseSpeed <= 0.0f) return 0.0f;
        float noise = (float) (RNG.nextGaussian() * 0.05);
        return clamp(baseSpeed + noise, 0f, 1f);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
