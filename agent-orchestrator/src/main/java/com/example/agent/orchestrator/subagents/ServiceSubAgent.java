package com.example.agent.orchestrator.subagents;

import com.example.agent.memory.ShortTermSessionStateService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Service
public class ServiceSubAgent {

    private final ChatClient serviceClient;
    private final ShortTermSessionStateService sessionStateService;

    public ServiceSubAgent(
            ChatClient.Builder builder,
            ToolCallbackProvider mcpToolProvider,
            ShortTermSessionStateService sessionStateService) {

        this.sessionStateService = sessionStateService;

        // Filter MCP tools: ServiceSubAgent ONLY sees increaseCreditCardLimit &
        // requestChequeBook
        ToolCallback[] serviceTools = Arrays.stream(mcpToolProvider.getToolCallbacks())
                .filter(t -> {
                    String name = t.getToolDefinition().name();
                    return name.endsWith("increaseCreditCardLimit") || name.endsWith("requestChequeBook");
                })
                .toArray(ToolCallback[]::new);

        this.serviceClient = builder
                .defaultSystem(
                        """
                                You are the Banking Service Sub-Agent.
                                Always pass the exact customerId and customerTier to increaseCreditCardLimit.
                                For requestChequeBook, pass addressConfirmed=true ONLY if the user explicitly confirmed their address.
                                """)
                .defaultTools((Object[]) serviceTools)
                .build();
    }

    @Tool(name = "consultServiceSubAgent", description = "Delegate credit card limit increases and cheque book delivery requests to the Service Sub-Agent.")
    public String handleServiceRequest(
            @ToolParam(description = "Authenticated Customer ID") String customerId,
            @ToolParam(description = "Authenticated Customer Tier (PRIVILEGED, PREMIUM, STANDARD)") String customerTier,
            @ToolParam(description = "Active Session ID") String sessionId,
            @ToolParam(description = "Whether the customer has explicitly confirmed their delivery address") boolean addressConfirmed,
            @ToolParam(description = "The specific service request to execute") String requestDetails) {

        String result = serviceClient.prompt()
                .user("""
                        Customer ID: %s
                        Customer Tier: %s
                        Address Confirmed: %s
                        Request: %s
                        """.formatted(customerId, customerTier, addressConfirmed, requestDetails))
                .call()
                .content();

        sessionStateService.updateSessionState(customerId, sessionId, Map.of(
                "lastSubAgentCalled", "ServiceSubAgent",
                "addressConfirmed", addressConfirmed,
                "lastServiceResult", result != null ? result : ""));

        return result;
    }
}