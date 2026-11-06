package com.mineagent.engine.resident;

import java.util.Map;

/** Resolve only at runtime. Never persist or log the selected value. */
public final class ResidentCredentials {
    private ResidentCredentials() {}

    public static String resolve(Map<String, String> environment, String configuredKey) {
        for (String name : new String[]{"CRAFTDROID_DEEPSEEK_API_KEY", "DEEPSEEK_API_KEY"}) {
            String value = environment.get(name);
            if (value != null && !value.isBlank()) return value;
        }
        return configuredKey == null ? "" : configuredKey;
    }
}
