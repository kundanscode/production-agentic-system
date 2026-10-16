package com.example.agent.memory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EpisodicMemoryService {

    private final VectorStore vectorStore;

    public EpisodicMemoryService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public record EpisodeExtraction(
            String summary,
            String customerIntent,
            String customerSentiment,
            String outcome) {
    }

    public void saveEpisodeSummary(
            String customerId,
            String sessionId,
            String summary,
            String intent,
            String sentiment,
            String outcome) {

        long epochSeconds = Instant.now().getEpochSecond();
        Document episodeDoc = new Document(
                summary,
                Map.of(
                        "memory_type", "episodic",
                        "customer_id", customerId,
                        "session_id", sessionId,
                        "intent", intent,
                        "sentiment", sentiment,
                        "outcome", outcome,
                        "created_at_epoch", epochSeconds));
        vectorStore.add(List.of(episodeDoc));
    }

    public String recallRecentEpisodes(String customerId, String currentQuery, int daysBack) {
        long cutoffEpoch = Instant.now().minus(daysBack, ChronoUnit.DAYS).getEpochSecond();
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        var filter = b.and(
                b.eq("customer_id", customerId),
                b.gte("created_at_epoch", cutoffEpoch)).build();

        List<Document> episodes = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(currentQuery)
                        .topK(3)
                        .similarityThreshold(0.60)
                        .filterExpression(filter)
                        .build());

        if (episodes.isEmpty()) {
            return "No recent past episodes found.";
        }

        return episodes.stream()
                .map(doc -> "[Past Episode | Intent: %s | Outcome: %s]: %s".formatted(
                        doc.getMetadata().getOrDefault("intent", "N/A"),
                        doc.getMetadata().getOrDefault("outcome", "N/A"),
                        doc.getText()))
                .collect(Collectors.joining("\n"));
    }

    public EpisodeExtraction summarizeAndArchiveSession(
            ChatClient chatClient,
            ChatMemory chatMemory,
            String customerId,
            String sessionId) {

        String convKey = customerId + ":" + sessionId;
        List<Message> transcript = chatMemory.get(convKey);
        if (transcript.isEmpty()) {
            return new EpisodeExtraction("Empty session", "None", "Neutral", "No action");
        }

        String formattedTranscript = transcript.stream()
                .map(m -> m.getMessageType().name() + ": " + m.getText())
                .collect(Collectors.joining("\n"));

        EpisodeExtraction extraction = chatClient.prompt()
                .system("""
                        Summarize this customer support conversation into a concise episodic memory record.
                        Extract:
                        - summary: 1-2 sentences describing what the customer asked and what actions were taken.
                        - customerIntent: The primary goal (e.g., Check suspicious Amazon debit, Request cheque book).
                        - customerSentiment: e.g., Concerned, Satisfied, Frustrated, Neutral.
                        - outcome: Final resolution status.
                        """)
                .user(formattedTranscript)
                .call()
                .entity(EpisodeExtraction.class);

        saveEpisodeSummary(
                customerId,
                sessionId,
                extraction.summary(),
                extraction.customerIntent(),
                extraction.customerSentiment(),
                extraction.outcome());
        chatMemory.clear(convKey);
        return extraction;
    }
}