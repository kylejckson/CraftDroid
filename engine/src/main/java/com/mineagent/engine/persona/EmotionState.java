package com.mineagent.engine.persona;

/**

 * Pleasure-Arousal-Dominance emotion state, inspired by SENTIPOLIS (CMU 2026) dual-speed

 * dynamics. Fast emotion varies on a seconds scale; slower mood provides a lasting baseline;

 * event memories carry emotion labels. Context can describe tone, activity and pauses, but prompt

 * guidance keeps rational decisions above emotional coloring.

 */
public class EmotionState {

    // Fast PAD vector in [-1,1].
    private float pleasure;  // -1: extreme displeasure; +1: extreme pleasure.
    private float arousal;   // -1: calm; +1: highly excited or alert.
    private float dominance; // -1: controlled or helpless; +1: in control or confident.

    // Slow mood baseline in [-0.3,0.3]: weaker but longer lasting.
    private float moodPleasure;
    private float moodArousal;
    private float moodDominance;

    // Decay rates.
    /**
     * Fast emotion decay per tick toward mood. Reduced from 0.04 to 0.015 so emotions last about
     * three seconds instead of 1.25, allowing ImportanceEvaluator.learnFromEmotion to detect
     * changes without prolonged interference with rational decisions.
     */
    private static final float EMOTION_DECAY = 0.015f;
    /** Mood decay per tick toward zero. */
    private static final float MOOD_DECAY = 0.001f;
    /** Strength of the mood baseline's pull on fast emotion. */
    private static final float MOOD_PULL = 0.005f;

    // Latest emotion label for prompt injection.
    private String lastEmotionLabel = "calm";

    public EmotionState() {
        this.pleasure = 0;
        this.arousal = 0;
        this.dominance = 0;
        this.moodPleasure = 0;
        this.moodArousal = 0;
        this.moodDominance = 0;
    }

    /**

     * Trigger an emotion event with pleasure/arousal/dominance changes in [-1,1] and intensity in

     * [0,1], where one is strongest.

     */
    public void triggerEvent(float eventPleasure, float eventArousal,
                              float eventDominance, float intensity) {
        pleasure = clamp(pleasure + eventPleasure * intensity);
        arousal = clamp(arousal + eventArousal * intensity);
        dominance = clamp(dominance + eventDominance * intensity);

        // Strong events also affect mood.
        if (intensity > 0.5f) {
            float moodImpact = (intensity - 0.5f) * 0.1f;
            moodPleasure = clampMood(moodPleasure + eventPleasure * moodImpact);
            moodArousal = clampMood(moodArousal + eventArousal * moodImpact);
            moodDominance = clampMood(moodDominance + eventDominance * moodImpact);
        }

        updateLabel();
    }

    /** Call once per tick: fast emotion approaches mood and mood approaches zero. */
    public void tick() {
        // Decay fast emotion toward the mood baseline.
        pleasure = approach(pleasure, moodPleasure, EMOTION_DECAY);
        arousal = approach(arousal, moodArousal, EMOTION_DECAY);
        dominance = approach(dominance, moodDominance, EMOTION_DECAY);

        // Decay mood toward zero very slowly.
        moodPleasure = approach(moodPleasure, 0, MOOD_DECAY);
        moodArousal = approach(moodArousal, 0, MOOD_DECAY);
        moodDominance = approach(moodDominance, 0, MOOD_DECAY);

        updateLabel();
    }

    /**

     * Describe emotion in the LLM system prompt. Rationality takes priority: expose the current

     * state as context without instructions to speak more or act faster. Emotion affects tone and

     * word choice, not the soundness of decisions.

     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Your current emotion\n");
        sb.append(String.format("Current emotion: %s (pleasure %.0f%% | arousal %.0f%% | confidence %.0f%%)\n",
                lastEmotionLabel,
                (pleasure + 1) * 50, (arousal + 1) * 50, (dominance + 1) * 50));
        // Describe the emotion without prescribing behavior.
        // Rationality first: emotion affects tone, not decisions.
        sb.append("Note: emotion affects only your tone and wording, ");
        sb.append("not your decision logic. ");
        sb.append("Regardless of emotion, always make the best decision through rational analysis.\n");
        return sb.toString();
    }

    // Predefined emotion events.
    // Intensities are reduced to about 60% of their former values to avoid overwhelming rationality.
    // Emotion should add flavor rather than dominate behavior.

    /** Attacked by a monster. */
    public void onAttacked() {
        triggerEvent(-0.4f, 0.5f, -0.3f, 0.5f);
    }

    /** Danger spotted. */
    public void onSeeDanger() {
        triggerEvent(-0.2f, 0.4f, -0.1f, 0.3f);
    }

    /** Valuable resource found, such as diamonds. */
    public void onFindTreasure() {
        triggerEvent(0.5f, 0.3f, 0.2f, 0.4f);
    }

    /** Task completed. */
    public void onTaskComplete() {
        triggerEvent(0.3f, 0.1f, 0.2f, 0.3f);
    }

    /** Praised by the player. */
    public void onPraised() {
        triggerEvent(0.4f, 0.2f, 0.2f, 0.4f);
    }

    /** Criticized by the player. */
    public void onScolded() {
        triggerEvent(-0.3f, 0.1f, -0.2f, 0.3f);
    }

    /** Task failed. */
    public void onTaskFailed() {
        triggerEvent(-0.2f, 0.2f, -0.2f, 0.3f);
    }

    /** Player hurt. */
    public void onOwnerHurt() {
        triggerEvent(-0.2f, 0.4f, -0.2f, 0.3f);
    }

    // ── Getters ──

    public float pleasure() { return pleasure; }
    public float arousal() { return arousal; }
    public float dominance() { return dominance; }
    public String label() { return lastEmotionLabel; }

    // Internal helpers.

    private void updateLabel() {
        // Map PAD values to semantic emotion labels.
        if (pleasure > 0.5f && arousal > 0.3f) lastEmotionLabel = "excited";
        else if (pleasure > 0.5f && arousal < -0.2f) lastEmotionLabel = "content";
        else if (pleasure > 0.3f) lastEmotionLabel = "happy";
        else if (pleasure < -0.5f && arousal > 0.5f) lastEmotionLabel = "angry";
        else if (pleasure < -0.5f && arousal < -0.2f) lastEmotionLabel = "discouraged";
        else if (pleasure < -0.3f && arousal > 0.4f) lastEmotionLabel = "afraid";
        else if (pleasure < -0.3f) lastEmotionLabel = "unhappy";
        else if (arousal > 0.5f) lastEmotionLabel = "alert";
        else if (arousal < -0.3f) lastEmotionLabel = "calm";
        else lastEmotionLabel = "calm";
    }

    private static float approach(float current, float target, float rate) {
        if (current > target) {
            return Math.max(target, current - rate);
        } else {
            return Math.min(target, current + rate);
        }
    }

    private static float clamp(float v) {
        return Math.max(-1f, Math.min(1f, v));
    }

    private static float clampMood(float v) {
        return Math.max(-0.3f, Math.min(0.3f, v));
    }
}
