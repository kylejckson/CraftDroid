package com.mineagent.engine.resident;

/** Conservative shared budgets. Credentials live in the existing LLM config or environment. */
public record ResidentSettings(int populationLimit, int decisionIntervalTicks, int maxConcurrentTurns,
                               int maxOutputTokens, String provider, String model, String baseUrl, int requestsPerMinute) {
    public static final ResidentSettings DEFAULT = new ResidentSettings(10,1200,2,1024,"deepseek","deepseek-flash","",30);
    public ResidentSettings(int populationLimit,int decisionIntervalTicks,int maxConcurrentTurns,int maxOutputTokens,String provider,String model,String baseUrl) {
        this(populationLimit,decisionIntervalTicks,maxConcurrentTurns,maxOutputTokens,provider,model,baseUrl,30);
    }
    public ResidentSettings {
        if(requestsPerMinute==0)requestsPerMinute=30;
        if(requestsPerMinute<1||requestsPerMinute>300)throw new IllegalArgumentException("Request budget must be 1-300 per minute");
        if (populationLimit<1||populationLimit>32||decisionIntervalTicks<200||decisionIntervalTicks>72000
                ||maxConcurrentTurns<1||maxConcurrentTurns>8||maxOutputTokens<128||maxOutputTokens>4096)
            throw new IllegalArgumentException("Resident budgets outside supported bounds");
        provider=provider==null?"deepseek":provider;
        model=model==null?"deepseek-flash":model;
        baseUrl=baseUrl==null?"":baseUrl;
    }
}
