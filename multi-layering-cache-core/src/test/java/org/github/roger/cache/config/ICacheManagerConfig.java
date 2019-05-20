package org.github.roger.cache.config;

import org.github.roger.MultiLayeringCacheManager;
import org.github.roger.manager.ICacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@Import({RedisConfig.class})
public class ICacheManagerConfig {


    @Bean
    public ICacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        MultiLayeringCacheManager layeringCacheManager = new MultiLayeringCacheManager(redisTemplate);

        return layeringCacheManager;
    }
}
