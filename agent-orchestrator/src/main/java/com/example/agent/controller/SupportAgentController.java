package com.example.agent.controller;

import com.example.agent.memory.EpisodicMemoryService;
import com.example.agent.orchestrator.CoordinatorAgentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/support")
public class SupportAgentController {

    private final CoordinatorAgentService coordinatorAgentService;

    public SupportAgentController(CoordinatorAgentService coordinatorAgentService) {
        this.coordinatorAgentService = coordinatorAgentService;
    }

    public record ChatRequest(String sessionId, String message) {
    }

    public record ChatResponse(String customerId, String tier, String sessionId, String reply) {
    }

    @PostMapping("/chat")
    public ChatResponse chat(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ChatRequest request) {

        String customerId = jwt.getSubject();
        String customerTier = jwt.getClaimAsString("customer_tier");
        if (customerTier == null || customerTier.isBlank()) {
            customerTier = "STANDARD";
        }

        String reply = coordinatorAgentService.handleCustomerQuery(
                customerId,
                customerTier,
                request.sessionId(),
                request.message());

        return new ChatResponse(customerId, customerTier, request.sessionId(), reply);
    }

    @PostMapping("/session/{sessionId}/close")
    public EpisodicMemoryService.EpisodeExtraction closeSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String sessionId) {

        String customerId = jwt.getSubject();
        return coordinatorAgentService.closeAndArchiveSession(customerId, sessionId);
    }
}