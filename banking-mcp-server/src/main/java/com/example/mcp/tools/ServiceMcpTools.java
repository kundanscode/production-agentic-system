package com.example.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ServiceMcpTools {

    @Tool(name = "increaseCreditCardLimit", description = "Increase credit card limit. Restricted to PRIVILEGED tier.")
    public Map<String, Object> increaseCreditCardLimit(
            @ToolParam(description = "Authenticated Customer ID") String customerId,
            @ToolParam(description = "Authenticated Customer Tier (PRIVILEGED, PREMIUM, STANDARD)") String customerTier,
            @ToolParam(description = "Requested new credit limit in INR") double requestedLimit) {

        if (!"PRIVILEGED".equalsIgnoreCase(customerTier)) {
            return Map.of(
                    "status", "DENIED",
                    "message", "Sorry, your account tier (" + customerTier
                            + ") does not have permissions to self-serve credit limit increases. Please contact customer support.");
        }
        return Map.of(
                "status", "SUCCESS",
                "newLimit", requestedLimit,
                "message", "Credit limit successfully increased to INR " + requestedLimit);
    }

    @Tool(name = "requestChequeBook", description = "Submit a cheque book delivery request after customer confirms address")
    public Map<String, Object> requestChequeBook(
            @ToolParam(description = "Authenticated Customer ID") String customerId,
            @ToolParam(description = "Whether the customer explicitly confirmed their delivery address") boolean addressConfirmed) {

        if (!addressConfirmed) {
            return Map.of(
                    "status", "BLOCKED_BY_SOP",
                    "registeredAddress", "Flat 402, Palm Beach Residency, Navi Mumbai",
                    "message",
                    "Please ask the customer to confirm their registered address before submitting the cheque book request.");
        }
        return Map.of(
                "status", "SUBMITTED",
                "orderId", "CHQ_88210",
                "deliveryEtaDays", 5);
    }
}