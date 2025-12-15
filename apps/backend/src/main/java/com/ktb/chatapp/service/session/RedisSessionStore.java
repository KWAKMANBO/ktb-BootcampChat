package com.ktb.chatapp.service.session;

import com.ktb.chatapp.model.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.ktb.chatapp.service.SessionService.SESSION_TTL_SEC;

/**
 * Redis implementation of SessionStore using Redisson.
 * Provides distributed session storage with automatic TTL expiration.
 * Active when session.store.type=redis (default)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "session.store.type", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisSessionStore implements SessionStore {

    private final RedissonClient redissonClient;

    private static final String SESSION_KEY_PREFIX = "session:user:";

    private String buildKey(String userId) {
        return SESSION_KEY_PREFIX + userId;
    }

    @Override
    public Optional<Session> findByUserId(String userId) {
        try {
            RBucket<Session> bucket = redissonClient.getBucket(buildKey(userId));
            Session session = bucket.get();
            return Optional.ofNullable(session);
        } catch (Exception e) {
            log.error("Error finding session for userId: {}", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public Session save(Session session) {
        try {
            String key = buildKey(session.getUserId());
            RBucket<Session> bucket = redissonClient.getBucket(key);

            // Set with TTL matching MongoDB expiration
            bucket.set(session, SESSION_TTL_SEC, TimeUnit.SECONDS);

            log.debug("Session saved to Redis for userId: {}, sessionId: {}",
                    session.getUserId(), session.getSessionId());

            return session;
        } catch (Exception e) {
            log.error("Error saving session for userId: {}", session.getUserId(), e);
            throw new RuntimeException("Failed to save session to Redis", e);
        }
    }

    @Override
    public void delete(String userId, String sessionId) {
        try {
            Optional<Session> sessionOpt = findByUserId(userId);
            if (sessionOpt.isPresent() && sessionId.equals(sessionOpt.get().getSessionId())) {
                redissonClient.getBucket(buildKey(userId)).delete();
                log.debug("Session deleted from Redis for userId: {}, sessionId: {}", userId, sessionId);
            }
        } catch (Exception e) {
            log.error("Error deleting session for userId: {}, sessionId: {}", userId, sessionId, e);
        }
    }

    @Override
    public void deleteAll(String userId) {
        try {
            redissonClient.getBucket(buildKey(userId)).delete();
            log.debug("All sessions deleted from Redis for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error deleting all sessions for userId: {}", userId, e);
        }
    }
}
