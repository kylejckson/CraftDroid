package com.mineagent.engine.memory;

import com.mineagent.api.task.TaskSnapshot;
import com.mineagent.api.task.TaskState;
import com.mineagent.engine.skill.SkillLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextSimilarityTest {
    @Test void chineseCharacterNgramsOutrankUnrelatedText() {
        double related = TextSimilarity.score("\u6536\u96c6\u6728\u6750\u5efa\u9020\u623f\u5b50", "\u780d\u6811\u6536\u96c6\u6728\u6750\u7528\u4e8e\u5efa\u9020");
        double unrelated = TextSimilarity.score("\u6536\u96c6\u6728\u6750\u5efa\u9020\u623f\u5b50", "\u5bfb\u627e\u94bb\u77f3\u5e76\u4e0e\u50f5\u5c38\u6218\u6597");
        assertTrue(related > unrelated);
    }

    @Test void learnedParameterizedSkillCanBeRetrievedInChinese() {
        SkillLibrary library = new SkillLibrary();
        library.registerSequence("wood_collection", "\u780d\u6811\u5e76\u6536\u96c6\u6728\u6750", "\u9700\u8981\u6728\u6750\u5efa\u9020\u65f6",
                "[{\"tool\":\"auto_mine\",\"args\":{\"block_id\":\"minecraft:oak_log\"}}]",
                true);
        assertFalse(library.retrieve("\u8bf7\u6536\u96c6\u4e00\u4e9b\u6728\u6750\u6765\u5efa\u9020\u623f\u5c4b").isEmpty());
    }

    @Test void successfulRelevantExperienceIsNotHiddenByUnrelatedFailure() {
        ExperienceStore store = new ExperienceStore();
        store.record("1", "auto_mine", "\u6536\u96c6\u6728\u6750", TaskState.SUCCESS,
                TaskSnapshot.running("complete", "logs collected"), "oak logs", 10L);
        store.record("2", "goto", "\u5bfb\u627e\u6751\u5e84", TaskState.FAILED,
                TaskSnapshot.running("navigate", "failed"), "path timeout", 20L);
        String summary = store.summarizeForPrompt("\u6536\u96c6\u6728\u6750\u5efa\u9020");
        assertTrue(summary.contains("auto_mine"));
        assertFalse(summary.contains("path timeout"));
    }

    @Test void failedAdaptationDoesNotOverwriteVerifiedSkillTrace() {
        SkillLibrary library = new SkillLibrary();
        library.registerSequence("bridge", "safe bridge", "cross gap",
                "[{\"tool\":\"build\",\"args\":{\"block\":\"stone\"}}]", true);
        library.registerSequence("bridge", "unsafe retry", "cross gap",
                "[{\"tool\":\"build\",\"args\":{\"block\":\"sand\"}}]", false);
        assertTrue(library.getActionSequence("bridge").contains("stone"));
        assertFalse(library.getActionSequence("bridge").contains("sand"));
    }
}
