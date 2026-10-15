package com.example.mcp.config;

import com.example.mcp.tools.AccountsMcpTools;
import com.example.mcp.tools.ServiceMcpTools;
import com.example.mcp.tools.TransactionsMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider bankingToolCallbackProvider(
            AccountsMcpTools accountsTools,
            TransactionsMcpTools transactionsTools,
            ServiceMcpTools serviceTools) {

        return MethodToolCallbackProvider.builder()
                .toolObjects(accountsTools, transactionsTools, serviceTools)
                .build();
    }
}