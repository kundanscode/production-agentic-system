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
public class TransactionsSubAgent {

    private final ChatClient transactionsClient;
    private final ShortTermSessionStateService sessionStateService;

    public TransactionsSubAgent(
            ChatClient.Builder builder,
            ToolCallbackProvider mcpToolProvider,
            ShortTermSessionStateService sessionStateService) {

        this.sessionStateService = sessionStateService;

        // Filter MCP tools: TransactionsSubAgent ONLY sees getRecentTransactions &
        // requestStatement
        ToolCallback[] txTools = Arrays.stream(mcpToolProvider.getToolCallbacks())
                .filter(t -> {
                    String name = t.getToolDefinition().name();
                    return name.endsWith("getRecentTransactions") || name.endsWith("requestStatement");
                })
                .toArray(ToolCallback[]::new);

        this.transactionsClient = builder
                .defaultSystem(
                        "You are the Transactions Domain Sub-Agent. Fetch recent transactions or request statements accurately.")
                .defaultTools((Object[]) txTools)
                .build();
    }

    @Tool(name = "consultTransactionsSubAgent", description = "Delegate recent transaction inquiries, debit checks, and account statement requests to the Transactions Sub-Agent.")
    public String handleTransactionQuery(
            @ToolParam(description = "Authenticated Customer ID") String customerId,
            @ToolParam(description = "Active Session ID") String sessionId,
            @ToolParam(description = "The specific transaction or statement question") String query) {

        String result = transactionsClient.prompt()
                .user("Customer ID: " + customerId + " | Question: " + query)
                .call()
                .content();

        sessionStateService.updateSessionState(customerId, sessionId, Map.of(
                "lastSubAgentCalled", "TransactionsSubAgent",
                "lastTransactionResult", result != null ? result : ""));

        return result;
    }
}