package com.github.roger.cahce.config;

import com.github.roger.aspect.MultiLayeringCacheAspect;
import com.github.roger.service.impl.UserServiceImpl;
import org.github.roger.MultiLayeringCacheManager;
import org.github.roger.manager.ICacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@Import({RedisConfig.class})
@EnableAspectJAutoProxy
public class ICacheManagerConfig {


    @Bean
    public ICacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        MultiLayeringCacheManager layeringCacheManager = new MultiLayeringCacheManager(redisTemplate);

        return layeringCacheManager;
    }

    @Bean//把切面交给Spring 容器管理
    public MultiLayeringCacheAspect layeringCacheAspect(){
        return new MultiLayeringCacheAspect();
    }

    @Bean
    public UserServiceImpl userService(){
        return new UserServiceImpl();
    }
}
