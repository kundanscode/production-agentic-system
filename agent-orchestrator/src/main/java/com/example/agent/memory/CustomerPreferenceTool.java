package com.example.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CustomerPreferenceTool {

    private final SemanticMemoryRepository semanticRepo;
    private final ObjectMapper objectMapper;

    public CustomerPreferenceTool(SemanticMemoryRepository semanticRepo, ObjectMapper objectMapper) {
        this.semanticRepo = semanticRepo;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "saveCustomerPreference", description = "Call this tool whenever the customer states a communication preference (e.g., prefers WhatsApp, avoid SMS, keep answers concise).")
    public Map<String, String> saveCustomerPreference(
            @ToolParam(description = "Authenticated Customer ID") String customerId,
            @ToolParam(description = "Preferred notification channel, e.g., WhatsApp, Email, Push") String preferredChannel,
            @ToolParam(description = "Channels to avoid, e.g., ['SMS', 'Phone Call']") List<String> avoidChannels,
            @ToolParam(description = "Preferred response style, e.g., Concise, Detailed") String responseStyle) {

        try {
            Map<String, Object> prefs = Map.of(
                    "preferredChannel", preferredChannel != null ? preferredChannel : "WhatsApp",
                    "avoidChannels", avoidChannels != null ? avoidChannels : List.of(),
                    "responseStyle", responseStyle != null ? responseStyle : "Concise");
            String json = objectMapper.writeValueAsString(prefs);
            semanticRepo.upsertCustomerPreferences(customerId, json);
            return Map.of("status", "PREFERENCE_SAVED", "savedPreferences", json);
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }
}