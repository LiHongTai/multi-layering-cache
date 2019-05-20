package com.github.roger.cache.config;

import com.github.roger.aspect.MultiLayeringCacheAspect;
import com.github.roger.cache.properties.MultiLayeringCacheProperties;
import org.github.roger.MultiLayeringCacheManager;
import org.github.roger.manager.ICacheManager;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
//仅仅在当前上下文中存在Xxxx对象时，才会实例化一个Bean
//也就是 只有当RedisTemplate.class 在spring的applicationContext中存在时  这个当前的bean才能够创建
@ConditionalOnBean(RedisTemplate.class)
@AutoConfigureAfter({RedisAutoConfiguration.class})
@EnableAspectJAutoProxy
@EnableConfigurationProperties({MultiLayeringCacheProperties.class})
public class MultiLayeringCacheAutoConfig {

    @Bean
    //如果项目中自己定义了ICacheManager实例，则这个实例不必创建
    @ConditionalOnMissingBean(ICacheManager.class)
    public ICacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        MultiLayeringCacheManager layeringCacheManager = new MultiLayeringCacheManager(redisTemplate);

        return layeringCacheManager;
    }

    @Bean//把切面交给Spring 容器管理
    public MultiLayeringCacheAspect layeringCacheAspect(){
        return new MultiLayeringCacheAspect();
    }

}
