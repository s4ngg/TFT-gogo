package com.tftgogo.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String META_DECKS = "metaDecks";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(META_DECKS);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(50));
        // @Transactional 메서드의 @CacheEvict가 커밋 전에 실행되어 동시 조회가
        // 무효화 직전 값을 재캐싱하지 않도록 캐시 삭제를 커밋 이후로 지연시킨다.
        return new TransactionAwareCacheManagerProxy(cacheManager);
    }
}
