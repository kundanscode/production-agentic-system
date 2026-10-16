package com.example.agent.orchestrator;

import com.example.agent.memory.*;
import com.example.agent.orchestrator.subagents.AccountsSubAgent;
import com.example.agent.orchestrator.subagents.ServiceSubAgent;
import com.example.agent.orchestrator.subagents.TransactionsSubAgent;
import com.example.agent.security.PiiRedactionAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CoordinatorAgentService {

    private final ChatClient coordinatorClient;
    private final ChatClient summarizerClient;
    private final RedisChatMemory redisChatMemory;
    private final ShortTermSessionStateService sessionStateService;
    private final EpisodicMemoryService episodicMemoryService;
    private final SemanticMemoryRepository semanticMemoryRepository;
    private final ProceduralMemoryService proceduralMemoryService;
    private final HybridLlmRouterService llmRouterService;

    public CoordinatorAgentService(
            ChatClient.Builder builder,
            org.springframework.ai.ollama.OllamaChatModel ollamaChatModel,
            RedisChatMemory redisChatMemory,
            PiiRedactionAdvisor piiRedactionAdvisor,
            AccountsSubAgent accountsSubAgent,
            TransactionsSubAgent transactionsSubAgent,
            ServiceSubAgent serviceSubAgent,
            CustomerPreferenceTool customerPreferenceTool,
            ShortTermSessionStateService sessionStateService,
            EpisodicMemoryService episodicMemoryService,
            SemanticMemoryRepository semanticMemoryRepository,
            ProceduralMemoryService proceduralMemoryService,
            HybridLlmRouterService llmRouterService) {

        this.redisChatMemory = redisChatMemory;
        this.sessionStateService = sessionStateService;
        this.episodicMemoryService = episodicMemoryService;
        this.semanticMemoryRepository = semanticMemoryRepository;
        this.proceduralMemoryService = proceduralMemoryService;
        this.llmRouterService = llmRouterService;
        this.summarizerClient = ChatClient.builder(ollamaChatModel).build();

        this.coordinatorClient = builder
                .defaultAdvisors(
                        piiRedactionAdvisor,
                        MessageChatMemoryAdvisor.builder(redisChatMemory).build())
                .defaultTools(
                        accountsSubAgent,
                        transactionsSubAgent,
                        serviceSubAgent,
                        customerPreferenceTool)
                .build();
    }

    public String handleCustomerQuery(
            String authenticatedCustomerId,
            String authenticatedTier,
            String sessionId,
            String userPrompt) {

        // 1. Determine LLM execution route
        String route = llmRouterService.selectExecutionRoute(userPrompt);

        // 2. Load Layer 2: Short-Term Session State from Redis Hash
        Map<String, Object> workingState = sessionStateService.getSessionState(authenticatedCustomerId, sessionId);

        // 3. Load Layer 3: Long-Term Episodic Memory from PostgreSQL + pgvector
        String recentEpisodes = episodicMemoryService.recallRecentEpisodes(authenticatedCustomerId, userPrompt, 14);

        // 4. Load Layer 4: Long-Term Semantic Memory from PostgreSQL JSONB
        String customerPreferences = semanticMemoryRepository.getCustomerPreferencesJson(authenticatedCustomerId);

        // 5. Load Layer 5: Long-Term Procedural Memory from YAML SOPs
        String sopProcedure = proceduralMemoryService.loadProcedureForPrompt(userPrompt);

        String systemPrompt = """
                You are the Enterprise Banking Coordinator Agent (Route: %s).
                Authenticated Customer ID: %s
                Authenticated Customer Tier: %s
                Active Session ID: %s

                [1. SEMANTIC MEMORY - CUSTOMER PREFERENCES]
                %s

                [2. EPISODIC MEMORY - RECENT PAST SESSIONS]
                %s

                [3. SHORT-TERM WORKING SESSION STATE]
                %s

                [4. PROCEDURAL MEMORY - MANDATORY SOP & CONSTRAINTS]
                %s

                ORCHESTRATION RULES:
                - For account balances, call consultAccountsSubAgent.
                - For transactions or statements, call consultTransactionsSubAgent.
                - For credit limit increases or cheque book requests, call consultServiceSubAgent.
                - If a user asks a compound question (e.g. balance AND transactions), call BOTH Sub-Agents and synthesize a single clear answer.
                - If the user expresses a communication preference (e.g. WhatsApp vs SMS), call saveCustomerPreference.
                - Never ask the customer for their Customer ID; always pass '%s' and Tier '%s'.

                RESPONSE FORMAT (MANDATORY):
                - After receiving tool results, you MUST synthesize the data into a friendly, conversational sentence.
                - Example: "Your current account balance is $5,432.10. Is there anything else I can help you with?"
                - NEVER return raw JSON, tool names, or internal data structures to the user.
                - Always end with a helpful follow-up offer.
                """
                .formatted(
                        route,
                        authenticatedCustomerId,
                        authenticatedTier,
                        sessionId,
                        customerPreferences,
                        recentEpisodes,
                        workingState,
                        sopProcedure,
                        authenticatedCustomerId,
                        authenticatedTier);

        String conversationKey = authenticatedCustomerId + ":" + sessionId;

        return coordinatorClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationKey))
                .call()
                .content();
    }

    public EpisodicMemoryService.EpisodeExtraction closeAndArchiveSession(String customerId, String sessionId) {
        var extraction = episodicMemoryService.summarizeAndArchiveSession(
                summarizerClient, redisChatMemory, customerId, sessionId);
        sessionStateService.clearSessionState(customerId, sessionId);
        return extraction;
    }
}