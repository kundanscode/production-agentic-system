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
public class AccountsSubAgent {

    private final ChatClient accountsClient;
    private final ShortTermSessionStateService sessionStateService;

    public AccountsSubAgent(
            ChatClient.Builder builder,
            ToolCallbackProvider mcpToolProvider,
            ShortTermSessionStateService sessionStateService) {

        this.sessionStateService = sessionStateService;

        // Filter MCP tools: AccountsSubAgent ONLY sees getAccountBalance
        ToolCallback[] accountTools = Arrays.stream(mcpToolProvider.getToolCallbacks())
                .filter(t -> t.getToolDefinition().name().endsWith("getAccountBalance"))
                .toArray(ToolCallback[]::new);

        this.accountsClient = builder
                .defaultSystem(
                        "You are the Accounts Domain Sub-Agent. Use getAccountBalance to fetch exact account balances.")
                .defaultTools((Object[]) accountTools)
                .build();
    }

    @Tool(name = "consultAccountsSubAgent", description = "Delegate account balance and savings account inquiries to the Accounts Sub-Agent.")
    public String handleAccountQuery(
            @ToolParam(description = "Authenticated Customer ID") String customerId,
            @ToolParam(description = "Active Session ID") String sessionId,
            @ToolParam(description = "The specific account question to answer") String query) {

        String result = accountsClient.prompt()
                .user("Customer ID: " + customerId + " | Question: " + query)
                .call()
                .content();

        sessionStateService.updateSessionState(customerId, sessionId, Map.of(
                "lastSubAgentCalled", "AccountsSubAgent",
                "lastAccountResult", result != null ? result : ""));

        return result;
    }
}