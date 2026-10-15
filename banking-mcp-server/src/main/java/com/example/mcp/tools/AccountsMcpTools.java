package com.example.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AccountsMcpTools {

    @Tool(name = "getAccountBalance", description = "Fetch savings account balance for the authenticated customer")
    public Map<String, Object> getAccountBalance(
            @ToolParam(description = "Authenticated Customer ID") String customerId) {
        return Map.of(
                "customerId", customerId,
                "accountType", "SAVINGS",
                "balanceInr", 52340.00,
                "currency", "INR");
    }
}