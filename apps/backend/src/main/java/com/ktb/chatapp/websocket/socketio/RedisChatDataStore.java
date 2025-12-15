package com.ktb.chatapp.websocket.socketio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis implementation of ChatDataStore using Redisson.
 * Provides distributed storage for chat-related data across multiple servers.
 * Active when chat.store.type=redis (default)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "chat.store.type", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisChatDataStore implements ChatDataStore {

    private final RedissonClient redissonClient;

    // TTL for ephemeral connection data (30 minutes, same as session)
    private static final long DEFAULT_TTL_SECONDS = 1800;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(key);
            T value = bucket.get();
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Error getting value from Redis for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void set(String key, Object value) {
        try {
            RBucket<Object> bucket = redissonClient.getBucket(key);
            // Set with TTL to auto-cleanup stale connections
            bucket.set(value, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error setting value in Redis for key: {}", key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            redissonClient.getBucket(key).delete();
        } catch (Exception e) {
            log.error("Error deleting value from Redis for key: {}", key, e);
        }
    }

    @Override
    public int size() {
        // This is expensive in Redis, consider removing or using a counter
        try {
            return (int) redissonClient.getKeys().count();
        } catch (Exception e) {
            log.error("Error getting size from Redis", e);
            return 0;
        }
    }
}
