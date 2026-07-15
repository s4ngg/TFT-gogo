package com.tftgogo.global.config;

import com.tftgogo.domain.community.chat.service.ChatRealtimePublisher;
import com.tftgogo.domain.community.chat.service.impl.RedisChatRealtimeSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@Profile("!local")
@RequiredArgsConstructor
public class ChatRedisConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisChatRealtimeSubscriber subscriber;

    @Bean
    public RedisMessageListenerContainer chatRedisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.setTaskExecutor(chatRedisListenerExecutor());
        container.setSubscriptionExecutor(chatRedisSubscriptionExecutor());
        container.addMessageListener(subscriber, new ChannelTopic(ChatRealtimePublisher.CHANNEL));
        return container;
    }

    @Bean
    public ThreadPoolTaskExecutor chatRedisListenerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("chat-redis-listener-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor chatRedisSubscriptionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("chat-redis-subscription-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.initialize();
        return executor;
    }
}
