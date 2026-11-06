package com.mineagent.engine.persona;

import java.util.Random;

/**

 * Stable personality traits for each companion, inspired by the five-factor OCEAN model and

 * VoxelMind's gradual drift. Merely prompting an introverted role is insufficient; structured

 * behavior constraints are needed. Extraversion controls speech/social distance, agreeableness

 * cooperation/aggression, conscientiousness planning/caution, neuroticism emotional

 * volatility/fear, and openness exploration/curiosity. Traits persist across sessions and slowly

 * drift with experience.

 */
public class PersonaProfile {

    // Five OCEAN dimensions in [0,1].
    private float openness;       // Openness: exploration and curiosity.
    private float conscientiousness; // Conscientiousness: planning and caution.
    private float extraversion;   // Extraversion: social activity.
    private float agreeableness;  // Agreeableness: cooperation and gentleness.
    private float neuroticism;    // Neuroticism: emotional volatility.

    /** Personality labels for prompt injection. */
    private final String[] traits;

    /** Drift rate: how much each experience changes personality. */
    private static final float DRIFT_RATE = 0.002f;

    public PersonaProfile(float openness, float conscientiousness,
                           float extraversion, float agreeableness,
                           float neuroticism, String[] traits) {
        this.openness = clamp(openness);
        this.conscientiousness = clamp(conscientiousness);
        this.extraversion = clamp(extraversion);
        this.agreeableness = clamp(agreeableness);
        this.neuroticism = clamp(neuroticism);
        this.traits = traits != null ? traits : new String[0];
    }

    /** Generate a random personality profile. */
    public static PersonaProfile random() {
        Random rng = new Random();
        return new PersonaProfile(
                rng.nextFloat(),
                rng.nextFloat(),
                rng.nextFloat(),
                rng.nextFloat(),
                rng.nextFloat(),
                generateTraits(rng)
        );
    }

    private static String[] generateTraits(Random rng) {
        // Generate natural-language trait descriptions from the five dimensions.
        java.util.List<String> traits = new java.util.ArrayList<>();
        if (rng.nextBoolean()) traits.add("enjoys exploring");
        if (rng.nextBoolean()) traits.add("organized");
        if (rng.nextBoolean()) traits.add("talkative");
        if (rng.nextBoolean()) traits.add("cautious");
        if (rng.nextBoolean()) traits.add("helpful");
        if (traits.isEmpty()) traits.add("easygoing");
        return traits.toArray(new String[0]);
    }

    // Behavior constraints.

    /** Determine whether to initiate speech, adjusting the base probability by extraversion. */
    public boolean shouldSpeak(float baseProbability) {
        // Higher extraversion makes speech more likely.
        float adjusted = baseProbability * (0.5f + extraversion);
        return Math.random() < adjusted;
    }

    /** Ideal social distance: extroverts stand closer, introverts farther away. */
    public float idealSocialDistance() {
        // Extraversion 0 gives four blocks; extraversion 1 gives 1.5 blocks.
        return 4.0f - extraversion * 2.5f;
    }

    /** Exploration tendency increases with openness. */
    public float explorationTendency() {
        return openness;
    }

    /** Planning tendency increases with conscientiousness: prepare before acting. */
    public float planningTendency() {
        return conscientiousness;
    }

    /** Fear response increases with neuroticism: threats are more likely to trigger retreat. */
    public float fearResponse() {
        return neuroticism;
    }

    /** Cooperation increases with agreeableness: help the player and avoid competing for kills. */
    public float cooperationTendency() {
        return agreeableness;
    }

    /**

     * Drift after experience. Positive experiences slightly raise extraversion/agreeableness;

     * negative experiences raise neuroticism and lower agreeableness. The positive parameter

     * distinguishes praise/success from criticism/failure.

     */
    public void drift(boolean positive) {
        if (positive) {
            extraversion = clamp(extraversion + DRIFT_RATE);
            agreeableness = clamp(agreeableness + DRIFT_RATE * 0.5f);
            neuroticism = clamp(neuroticism - DRIFT_RATE * 0.3f);
        } else {
            neuroticism = clamp(neuroticism + DRIFT_RATE);
            agreeableness = clamp(agreeableness - DRIFT_RATE * 0.5f);
            extraversion = clamp(extraversion - DRIFT_RATE * 0.3f);
        }
    }

    /** Generate a personality description for the LLM system prompt. */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Your personality\n");
        for (String trait : traits) {
            sb.append("- ").append(trait).append("\n");
        }
        sb.append(String.format(
                "- Exploration: %.0f%% | Planning: %.0f%% | Sociability: %.0f%% | Cooperation: %.0f%% | Emotional sensitivity: %.0f%%\n",
                openness * 100, conscientiousness * 100,
                extraversion * 100, agreeableness * 100, neuroticism * 100));

        // Behavior guidance makes personality more than descriptive text.
        sb.append("Act according to your personality:");
        if (openness > 0.6f) sb.append(" You are curious and enjoy exploring unfamiliar places.");
        if (conscientiousness > 0.6f) sb.append(" You are organized and prepare your tools before setting out.");
        if (extraversion > 0.6f) sb.append(" You are talkative and initiate conversations with the player.");
        else if (extraversion < 0.3f) sb.append(" You are quiet and answer when asked rather than initiating conversation.");
        if (neuroticism > 0.6f) sb.append(" You are cautious and tend to retreat when facing danger.");
        if (agreeableness > 0.6f) sb.append(" You are cooperative and prioritize helping the player.");
        sb.append("\n");
        return sb.toString();
    }

    // ── Getters ──

    public float openness() { return openness; }
    public float conscientiousness() { return conscientiousness; }
    public float extraversion() { return extraversion; }
    public float agreeableness() { return agreeableness; }
    public float neuroticism() { return neuroticism; }
    public String[] traits() { return traits; }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
