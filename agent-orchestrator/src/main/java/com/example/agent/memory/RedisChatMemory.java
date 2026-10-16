package com.example.agent.memory;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisChatMemory(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty()) {
            return;
        }

        String key = buildKey(conversationId);
        List<RedisMessageDto> dtos = messages.stream()
                .filter(Objects::nonNull)
                .map(RedisMessageDto::fromMessage)
                .toList();

        for (RedisMessageDto dto : dtos) {
            redisTemplate.opsForList().rightPush(key, dto);
        }
        redisTemplate.expire(key, DEFAULT_TTL);
    }

    @Override
    public List<Message> get(String conversationId) {
        if (conversationId == null) {
            return Collections.emptyList();
        }

        String key = buildKey(conversationId);
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Message> messages = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof RedisMessageDto dto) {
                messages.add(dto.toMessage());
            } else if (item instanceof Map<?, ?> map) {
                String typeStr = (String) map.get("messageType");
                String text = (String) map.get("text");
                messages.add(createMessage(typeStr, text));
            }
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null) {
            return;
        }
        redisTemplate.delete(buildKey(conversationId));
    }

    private String buildKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    private static Message createMessage(String messageType, String text) {
        MessageType type = messageType != null ? MessageType.valueOf(messageType) : MessageType.USER;
        return switch (type) {
            case USER -> new UserMessage(text != null ? text : "");
            case ASSISTANT -> new AssistantMessage(text != null ? text : "");
            case SYSTEM -> new SystemMessage(text != null ? text : "");
            default -> new UserMessage(text != null ? text : "");
        };
    }

    public record RedisMessageDto(String messageType, String text) implements Serializable {
        public static RedisMessageDto fromMessage(Message message) {
            return new RedisMessageDto(
                    message.getMessageType() != null ? message.getMessageType().name() : MessageType.USER.name(),
                    message.getText() != null ? message.getText() : "");
        }

        public Message toMessage() {
            return createMessage(this.messageType, this.text);
        }
    }
}
