package com.example.agent.orchestrator;

import org.springframework.stereotype.Service;

@Service
public class HybridLlmRouterService {

    public String selectExecutionRoute(String userPrompt) {
        String lower = userPrompt.toLowerCase();
        boolean requiresComplexReasoning = userPrompt.length() > 300
                || (lower.contains("analyze") && lower.contains("compare"));
        return requiresComplexReasoning ? "COMPLEX_REASONING_ROUTE" : "SELF_HOSTED_OLLAMA_ROUTE";
    }
}