package com.mineagent.engine.memory;

import com.mineagent.engine.knowledge.MinecraftKnowledgeGraph;
import com.mineagent.engine.persona.EmotionState;
import com.mineagent.engine.persona.PersonaProfile;
import com.mineagent.engine.skill.SkillLibrary;
import com.mineagent.engine.theory.TheoryOfMind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Protect the English output contract while multilingual input remains supported. */
final class EnglishRuntimeTextTest {
    @Test void generatedPersonalityAndEmotionPromptsUseEnglish() {
        PersonaProfile persona = new PersonaProfile(0.8f, 0.8f, 0.8f, 0.8f, 0.8f,
                new String[]{"helpful"});
        String prompt = persona.toPromptString();
        assertTrue(prompt.contains("Exploration: 80%"));
        assertTrue(prompt.contains("You are cooperative"));
        assertEnglish(prompt);
        assertEnglish(PersonaProfile.random().toPromptString());

        EmotionState emotion = new EmotionState();
        assertEquals("calm", emotion.label());
        emotion.onTaskComplete();
        assertTrue(emotion.toPromptString().contains("Current emotion:"));
        assertEnglish(emotion.toPromptString());
    }

    @Test void failureLessonsAndMemoryHeadingsUseEnglish() {
        ReflectionSystem reflections = new ReflectionSystem();
        reflections.recordToolFailure("goto", "{}", "target not found", "reach target");
        String summary = reflections.summarizeForPrompt();
        assertTrue(summary.contains("Target not found"));
        assertTrue(summary.contains("look_around"));
        assertEnglish(summary);
        assertTrue(new PlaceEventMemory().summarizeForPrompt().startsWith("(No location"));
    }

    @Test void knowledgeSkillsAndPlayerIntentUseEnglish() {
        String knowledge = new MinecraftKnowledgeGraph().summarizeForPrompt();
        assertTrue(knowledge.contains("Minecraft crafting knowledge"));
        assertEnglish(knowledge);
        assertEquals("(No learned skills yet)", new SkillLibrary().summarizeForPrompt());
        assertEquals("mining", TheoryOfMind.PlayerIntent.MINING.desc());
        assertEnglish(new TheoryOfMind().toPromptString());
    }

    private static void assertEnglish(String text) {
        assertFalse(text.codePoints().anyMatch(cp ->
                Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN), text);
    }
}
