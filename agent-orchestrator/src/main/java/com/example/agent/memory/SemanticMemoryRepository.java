package com.example.agent.memory;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SemanticMemoryRepository {

    private final JdbcClient jdbcClient;

    public SemanticMemoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public String getCustomerPreferencesJson(String customerId) {
        return jdbcClient.sql("SELECT preferences::text FROM customer_semantic_memory WHERE customer_id = :cid")
                .param("cid", customerId)
                .query(String.class)
                .optional()
                .orElse("{\"responseStyle\": \"Standard\", \"preferredChannel\": \"Email\", \"avoidChannels\": []}");
    }

    public void upsertCustomerPreferences(String customerId, String partialPreferencesJson) {
        jdbcClient.sql("""
                INSERT INTO customer_semantic_memory (customer_id, preferences, updated_at)
                VALUES (:cid, :prefs::jsonb, CURRENT_TIMESTAMP)
                ON CONFLICT (customer_id)
                DO UPDATE SET preferences = customer_semantic_memory.preferences || EXCLUDED.preferences,
                              updated_at = CURRENT_TIMESTAMP
                """)
                .param("cid", customerId)
                .param("prefs", partialPreferencesJson)
                .update();
    }
}