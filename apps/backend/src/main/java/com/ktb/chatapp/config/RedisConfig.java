package com.ktb.chatapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL;

/**
 * Redis configuration for multi-server Socket.IO support.
 * Configures Redisson client for distributed data storage and Pub/Sub.
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private Integer redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Redisson client bean for Redis operations.
     * Used for:
     * - Socket.IO distributed store (RedissonStoreFactory)
     * - Session management (RedisSessionStore)
     * - Chat data storage (RedisChatDataStore)
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        String address = String.format("redis://%s:%d", redisHost, redisPort);

        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(10)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500)
                .setKeepAlive(true);

        // Configure ObjectMapper with JavaTimeModule for Java 8 date/time support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Enable polymorphic type handling for proper deserialization
        // Allow only specific packages to prevent security issues and deserialization errors
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.ktb.chatapp.")                    // Our application classes
                .allowIfSubType("java.util.")                           // Java collections
                .allowIfSubType("java.time.")                           // Java time classes
                .allowIfSubType("java.lang.")                           // Java lang classes
                .allowIfSubType("com.corundumstudio.socketio.")         // Socket.IO classes
                .allowIfSubType("org.redisson.")                        // Redisson internal classes
                .build();
        objectMapper.activateDefaultTyping(ptv, NON_FINAL);

        // Use Jackson codec for JSON serialization with custom ObjectMapper
        config.setCodec(new JsonJacksonCodec(objectMapper));

        log.info("Redisson client configured for {}:{}", redisHost, redisPort);

        return Redisson.create(config);
    }
}
