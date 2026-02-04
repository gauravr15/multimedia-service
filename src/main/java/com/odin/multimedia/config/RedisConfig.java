package com.odin.multimedia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.odin.multimedia.service.StatusImageExpiryListener;

@Configuration
public class RedisConfig {

    /**
     * Configures the Redis message listener container to handle key expiration events.
     * Note: Redis keyspace notifications must be enabled on the server for this to work.
     * Run: CONFIG SET notify-keyspace-events Ex
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                       StatusImageExpiryListener expiryListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // Listen for key expiration events
        container.addMessageListener(expiryListener, new PatternTopic("__keyevent@*__:expired"));
        return container;
    }
}
