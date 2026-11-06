package com.mineagent.engine.resident;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ResidentCredentialsTest {
    @Test void acceptsStandardDeepSeekBindingAndKeepsExplicitOverridePrecedence() {
        assertEquals("standard-test-value",ResidentCredentials.resolve(Map.of("DEEPSEEK_API_KEY","standard-test-value"),"configured-test-value"));
        assertEquals("specific-test-value",ResidentCredentials.resolve(Map.of("CRAFTDROID_DEEPSEEK_API_KEY","specific-test-value","DEEPSEEK_API_KEY","standard-test-value"),"configured-test-value"));
    }
    @Test void ignoresEmptyBindingsAndDoesNotInventCredentials() {
        assertEquals("configured-test-value",ResidentCredentials.resolve(Map.of("DEEPSEEK_API_KEY"," "),"configured-test-value"));
        assertEquals("",ResidentCredentials.resolve(Map.of(),null));
    }
}
