package com.example.agent.security;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PiiRedactionAdvisor implements CallAdvisor {

    // Matches 13 to 19 digit credit/debit card numbers (with optional spaces or
    // dashes)
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");

    // Matches Indian PAN numbers (e.g., ABCDE1234F)
    private static final Pattern PAN_PATTERN = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b");

    @Override
    public String getName() {
        return "PiiRedactionAdvisor";
    }

    @Override
    public int getOrder() {
        return 0; // Runs BEFORE MessageChatMemoryAdvisor and BEFORE LLM inference
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String originalUserText = request.prompt().getUserMessage().getText();

        String scrubbedText = CREDIT_CARD_PATTERN.matcher(originalUserText)
                .replaceAll("[REDACTED_CARD_NUMBER]");
        scrubbedText = PAN_PATTERN.matcher(scrubbedText)
                .replaceAll("[REDACTED_PAN]");

        Prompt sanitizedPrompt = request.prompt().augmentUserMessage(scrubbedText);
        ChatClientRequest sanitizedRequest = request.mutate()
                .prompt(sanitizedPrompt)
                .build();

        return chain.nextCall(sanitizedRequest);
    }
}