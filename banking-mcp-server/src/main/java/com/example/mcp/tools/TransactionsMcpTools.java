package com.example.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TransactionsMcpTools {

    @Tool(name = "getRecentTransactions", description = "Fetch the last N transactions for the authenticated customer")
    public List<Map<String, Object>> getRecentTransactions(
            @ToolParam(description = "Authenticated Customer ID") String customerId,
            @ToolParam(description = "Number of recent transactions to fetch", required = false) Object count) {
        int limit = 5;
        if (count instanceof Number n) {
            limit = n.intValue();
        } else if (count != null) {
            try {
                limit = Integer.parseInt(count.toString().trim());
            } catch (Exception ignored) {}
        }
        return List.of(
                Map.of("txId", "TXN_901", "date", "2026-10-14", "merchant", "Amazon", "amount", 1200.00, "type",
                        "DEBIT"),
                Map.of("txId", "TXN_900", "date", "2026-10-11", "merchant", "Swiggy", "amount", 450.00, "type",
                        "DEBIT"));
    }

    @Tool(name = "requestStatement", description = "Request an account statement PDF for a given date range")
    public Map<String, Object> requestStatement(
            @ToolParam(description = "Authenticated Customer ID") String customerId,
            @ToolParam(description = "Start date in YYYY-MM-DD format") String fromDate,
            @ToolParam(description = "End date in YYYY-MM-DD format") String toDate) {
        return Map.of(
                "customerId", customerId,
                "status", "DISPATCHED",
                "period", fromDate + " to " + toDate);
    }
}