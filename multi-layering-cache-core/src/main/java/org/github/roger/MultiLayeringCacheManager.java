package org.github.roger;

import org.github.roger.cache.ICache;
import org.github.roger.cache.caffeine.CaffeineCache;
import org.github.roger.cache.redis.RedisCache;
import org.github.roger.manager.AbstractCacheManager;
import org.github.roger.settings.MultiLayeringCacheSetting;
import org.springframework.data.redis.core.RedisTemplate;

public class MultiLayeringCacheManager extends AbstractCacheManager {

    public MultiLayeringCacheManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        cacheManagers.add(this);
    }

    @Override
    protected ICache getMissingCache(String name, MultiLayeringCacheSetting multilayeringCacheSetting) {
        // 创建一级缓存
        CaffeineCache caffeineCache = new CaffeineCache(name, multilayeringCacheSetting.getFirstCacheSetting());
        // 创建二级缓存
        RedisCache redisCache = new RedisCache(name, redisTemplate, multilayeringCacheSetting.getSecondaryCacheSetting());
        return new MultiLayeringCache(redisTemplate, caffeineCache, redisCache,  multilayeringCacheSetting);
    }

}
