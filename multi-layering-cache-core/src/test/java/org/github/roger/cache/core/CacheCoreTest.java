package org.github.roger.cache.core;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.github.roger.MultiLayeringCache;
import org.github.roger.cache.ICache;
import org.github.roger.cache.config.ICacheManagerConfig;
import org.github.roger.cache.redis.RedisCache;
import org.github.roger.enumeration.ExpireMode;
import org.github.roger.manager.ICacheManager;
import org.github.roger.settings.FirstCacheSetting;
import org.github.roger.settings.MultiLayeringCacheSetting;
import org.github.roger.settings.SecondaryCacheSetting;
import org.github.roger.utils.RedisCacheKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

// SpringJUnit4ClassRunner再Junit环境下提供Spring TestContext Framework的功能。
@RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration用来加载配置ApplicationContext，其中classes用来加载配置类
@ContextConfiguration(classes = {ICacheManagerConfig.class})
@Slf4j
public class CacheCoreTest {

    @Autowired
    private ICacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private MultiLayeringCacheSetting layeringCacheSetting1;
    private MultiLayeringCacheSetting layeringCacheSetting2;
    private MultiLayeringCacheSetting layeringCacheSetting4;
    private MultiLayeringCacheSetting layeringCacheSetting5;

    @Before
    public void before() {
        // 测试 CacheManager getCache方法
        FirstCacheSetting firstCacheSetting1 = new FirstCacheSetting(10, 1000, 4, TimeUnit.SECONDS, ExpireMode.WRITE);
        SecondaryCacheSetting secondaryCacheSetting1 = new SecondaryCacheSetting(10, 4, TimeUnit.SECONDS, true, true, true, 1);
        layeringCacheSetting1 = new MultiLayeringCacheSetting(firstCacheSetting1, secondaryCacheSetting1);

        // 二级缓存可以缓存null,时间倍率是1
        FirstCacheSetting firstCacheSetting2 = new FirstCacheSetting(10, 1000, 5, TimeUnit.SECONDS, ExpireMode.WRITE);
        SecondaryCacheSetting secondaryCacheSetting2 = new SecondaryCacheSetting(3000, 14, TimeUnit.SECONDS, true, true, true, 1);
        layeringCacheSetting2 = new MultiLayeringCacheSetting(firstCacheSetting2, secondaryCacheSetting2);

        // 二级缓存可以缓存null,时间倍率是10
        FirstCacheSetting firstCacheSetting4 = new FirstCacheSetting(10, 1000, 5, TimeUnit.SECONDS, ExpireMode.WRITE);
        SecondaryCacheSetting secondaryCacheSetting4 = new SecondaryCacheSetting(100, 70, TimeUnit.SECONDS, true, true, true, 10);
        layeringCacheSetting4 = new MultiLayeringCacheSetting(firstCacheSetting4, secondaryCacheSetting4);


        // 二级缓存不可以缓存null
        FirstCacheSetting firstCacheSetting5 = new FirstCacheSetting(10, 1000, 5, TimeUnit.SECONDS, ExpireMode.WRITE);
        SecondaryCacheSetting secondaryCacheSetting5 = new SecondaryCacheSetting(10, 7, TimeUnit.SECONDS, true, false, true, 1);
        layeringCacheSetting5 = new MultiLayeringCacheSetting(firstCacheSetting5, secondaryCacheSetting5);

    }

    @Test
    public void testGetCache() {
        String cacheName = "cache:name";
        ICache cache1 = cacheManager.getCache(cacheName, layeringCacheSetting1);
        ICache cache2 = cacheManager.getCache(cacheName, layeringCacheSetting1);
        Assert.assertEquals(cache1, cache2);

        ICache cache3 = cacheManager.getCache(cacheName, layeringCacheSetting2);
        Collection<ICache> caches = cacheManager.getCache(cacheName);
        Assert.assertTrue(caches.size() == 2);
        Assert.assertNotEquals(cache1, cache3);
    }

    @Test
    public void testCacheExpiration() {
        //一级缓存的有效时间是在写入之后，4秒
        String cacheName = "cache:name";
        String cacheKey1 = "cache:key1";
        MultiLayeringCache cache1 = (MultiLayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting1);
        cache1.get(cacheKey1, () -> initCache(String.class));
        // 测试一级缓存值及过期时间
        String str1 = cache1.getFirstCache().get(cacheKey1, String.class);
        String st2 = cache1.getFirstCache().get(cacheKey1, () -> initCache(String.class));
        log.debug("========================:{}", str1);
        Assert.assertTrue(str1.equals(st2));
        Assert.assertTrue(str1.equals(initCache(String.class)));
        sleep(5);
        Assert.assertNull(cache1.getFirstCache().get(cacheKey1, String.class));
        // 看日志是不是走了二级缓存
        cache1.get(cacheKey1, () -> initCache(String.class));
        log.debug("***********************");
        log.debug("***********************");
        log.debug("***********************");
        //二级缓存的有效时间是10秒，在失效前 4秒强制刷新缓存
        str1 = cache1.getSecondCache().get(cacheKey1, String.class);
        st2 = cache1.getSecondCache().get(cacheKey1, () -> initCache(String.class));
        Assert.assertTrue(st2.equals(str1));
        Assert.assertTrue(str1.equals(initCache(String.class)));
        sleep(5);
        // 看日志是不是走了自动刷新
        RedisCacheKey redisCacheKey = ((RedisCache) cache1.getSecondCache()).getRedisCacheKey(cacheKey1);
        cache1.get(cacheKey1, () -> initCache(String.class));
        sleep(6);
        Long ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        log.debug("========================ttl 1:{}", ttl);
        Assert.assertNotNull(cache1.getSecondCache().get(cacheKey1));
        sleep(5);
        ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        log.debug("========================ttl 2:{}", ttl);
        Assert.assertNull(cache1.getSecondCache().get(cacheKey1));
    }

    @Test
    public void testGetCacheNullUseAllowNullValueTrue() {
        log.info("测试二级缓存允许为NULL，NULL值时间倍率是10");
        //定义缓存名称
        String cacheName = "cache:name:19_1";
        //定义缓存key
        String cacheKey = "cache:key:19_1";
        //根据缓存名称获取对应的多级缓存管理对象
        MultiLayeringCache cache = (MultiLayeringCache) cacheManager.getCache(cacheName,layeringCacheSetting4);
        //根据缓存key获取缓存值,如果没有则调用相应的方法给缓存key设置缓存值，并返回
        cache.get(cacheKey,()->initNullCache());
        // 测试一级缓存值不能缓存NULL
        String str1 = cache.getFirstCache().get(cacheKey, String.class);
        Cache<Object,Object> realCache = (Cache<Object, Object>) cache.getFirstCache().getRealCache();
        Assert.assertTrue(str1 == null);
        // 如果一级缓存可以保存null值，则此时size = 1
        Assert.assertTrue(0 == realCache.asMap().size());

        // 测试二级缓存可以存NULL值，NULL值时间倍率是10，缓存配置的过期时间是100s
        String st2 = cache.getSecondCache().get(cacheKey, String.class);
        RedisCacheKey redisCacheKey = ((RedisCache) cache.getSecondCache()).getRedisCacheKey(cacheKey);
        Long ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        Assert.assertTrue(redisTemplate.hasKey(redisCacheKey.getKey()));
        Assert.assertTrue(st2 == null);
        Assert.assertTrue(ttl <= 10);
        sleep(5);
        st2 = cache.getSecondCache().get(cacheKey, String.class);
        Assert.assertTrue(st2 == null);
        cache.getSecondCache().get(cacheKey, () -> initNullCache());
        sleep(1);
        ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        Assert.assertTrue(ttl <= 10 && ttl > 5);

        st2 = cache.get(cacheKey, String.class);
        Assert.assertTrue(st2 == null);
    }

    @Test
    public void testCacheEvict() throws Exception {
        // 测试 缓存过期时间
        String cacheName = "cache:name";
        String cacheKey1 = "cache:key2";
        String cacheKey2 = "cache:key3";
        MultiLayeringCache cache1 = (MultiLayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting1);
        cache1.get(cacheKey1, () -> initCache(String.class));
        cache1.get(cacheKey2, () -> initCache(String.class));
        // 测试删除方法
        cache1.evict(cacheKey1);
        Thread.sleep(500);
        String str1 = cache1.get(cacheKey1, String.class);
        String str2 = cache1.get(cacheKey2, String.class);
        Assert.assertNull(str1);
        Assert.assertNotNull(str2);
        // 测试删除方法
        cache1.evict(cacheKey1);
        Thread.sleep(500);
        str1 = cache1.get(cacheKey1, () -> initCache(String.class));
        str2 = cache1.get(cacheKey2, String.class);
        Assert.assertNotNull(str1);
        Assert.assertNotNull(str2);
    }

    private <T> T initCache(Class<T> t) {
        log.debug("加载缓存");
        return (T) "test";
    }

    private <T> T initNullCache() {
        log.debug("加载缓存,空值");
        return null;
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time * 1000);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
