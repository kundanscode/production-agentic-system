package com.example.agent.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.stereotype.Component;

@Component
public class TokenCostTrackerHandler implements ObservationHandler<ChatModelObservationContext> {

    private static final Logger log = LoggerFactory.getLogger(TokenCostTrackerHandler.class);

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }

    @Override
    public void onStop(ChatModelObservationContext context) {
        if (context.getResponse() != null && context.getResponse().getMetadata().getUsage() != null) {
            var usage = context.getResponse().getMetadata().getUsage();
            String model = context.getRequest().getOptions() != null
                    ? context.getRequest().getOptions().getModel()
                    : "ollama";
            log.info(
                    "[AI_OBSERVABILITY & COST_TRACKER] Model={} | PromptTokens={} | CompletionTokens={} | TotalTokens={}",
                    model,
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens());
        }
    }
}