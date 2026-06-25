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

@Configuration
@Profile("!local & !dev")
@RequiredArgsConstructor
public class ChatRedisConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisChatRealtimeSubscriber subscriber;

    @Bean
    public RedisMessageListenerContainer chatRedisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(ChatRealtimePublisher.CHANNEL));
        return container;
    }
}
