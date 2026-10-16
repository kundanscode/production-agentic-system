package com.example.agent.memory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ProceduralMemoryService {

    public String loadProcedureForPrompt(String userPrompt) {
        String lower = userPrompt.toLowerCase();
        String procedureFile = "chequebook_request.yaml";
        if (lower.contains("delay") || lower.contains("late") || lower.contains("where is")) {
            procedureFile = "delayed_order.yaml";
        }
        return loadProcedureByName(procedureFile);
    }

    public String loadProcedureByName(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource("procedures/" + fileName);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Follow standard banking security guidelines and verify customer requests before mutating state.";
        }
    }
}