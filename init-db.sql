CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Long-Term Semantic Memory Table (Structured Customer Preferences)
CREATE TABLE IF NOT EXISTS customer_semantic_memory (
    customer_id VARCHAR(64) PRIMARY KEY,
    preferences JSONB NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed a sample customer preference record
INSERT INTO customer_semantic_memory (customer_id, preferences)
VALUES (
    'CUST_001',
    '{"responseStyle": "Concise", "preferredChannel": "WhatsApp", "avoidChannels": ["SMS"]}'::jsonb
);