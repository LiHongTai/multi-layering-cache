package com.github.roger.cahce.utils;

import com.github.roger.key.DefaultKey;
import org.github.roger.serializer.StringRedisSerializer;
import org.github.roger.utils.RedisCacheKey;
import org.junit.Test;

public class RedisCacheKeyTest {

    @Test
    public void testGetKey() {

        DefaultKey defaultKey = new DefaultKey("arg1","arg2");
        System.out.println("defaultKey = " + defaultKey);
        RedisCacheKey redisCacheKey = new RedisCacheKey(defaultKey,new StringRedisSerializer());
        redisCacheKey.cacheName("cacheName");
        String cacheKeyString = redisCacheKey.getKey();

        System.out.println("缓存key = " + cacheKeyString);
    }
}