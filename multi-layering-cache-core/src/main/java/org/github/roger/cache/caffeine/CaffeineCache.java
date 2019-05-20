package org.github.roger.cache.caffeine;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.github.roger.cache.AbstractValueAdaptingCache;
import org.github.roger.enumeration.ExpireMode;
import org.github.roger.settings.FirstCacheSetting;
import org.github.roger.support.NullValue;

import java.util.concurrent.Callable;

@Slf4j
public class CaffeineCache extends AbstractValueAdaptingCache {

    /** caffeine 缓存对象 */
    private Cache<Object,Object> cache;


    /**
     * 使用name和{@link FirstCacheSetting}创建一个 {@link CaffeineCache} 实例
     *
     * @param name              缓存名称
     * @param firstCacheSetting 一级缓存配置 {@link FirstCacheSetting}
     */
    public CaffeineCache(String name, FirstCacheSetting firstCacheSetting) {
        super( name);
        this.cache = getCache(firstCacheSetting);
    }

    /**
     *  构建一个caffeine缓存对象，用于存储一级缓存
     * @param firstCacheSetting 一级缓存配置
     * @return  一级缓存对象
     */
    private Cache<Object,Object> getCache(FirstCacheSetting firstCacheSetting) {
        //根据一级缓存设置，构建caffeine缓存对象
        Caffeine<Object,Object> cacheBuilder = Caffeine.newBuilder();
        cacheBuilder.initialCapacity(firstCacheSetting.getInitialCapacity());
        cacheBuilder.maximumSize(firstCacheSetting.getMaximumSize());
        if(ExpireMode.WRITE.equals(firstCacheSetting.getExpireMode())){
            cacheBuilder.expireAfterWrite(firstCacheSetting.getExpireTime(),firstCacheSetting.getTimeUnit());
        }
        if(ExpireMode.ACCESS.equals(firstCacheSetting.getExpireMode())){
            cacheBuilder.expireAfterAccess(firstCacheSetting.getExpireTime(),firstCacheSetting.getTimeUnit());
        }
        return cacheBuilder.build();
    }


    public boolean isAllowNullValues() {
        return false;
    }

    public Object getRealCache() {
        return this.cache;
    }

    public Object get(Object key) {
        log.debug("caffeine缓存 key={} 获取缓存", JSON.toJSONString(key));

        if (this.cache instanceof LoadingCache) {
            return ((LoadingCache<Object, Object>) this.cache).get(key);
        }

        return cache.getIfPresent(key);
    }

    public <T> T get(Object key, Callable<T> valueLoader) {
        log.debug("caffeine缓存 key={} 获取缓存， 如果没有命中就走库加载缓存", JSON.toJSONString(key));
        // 获取key对应的缓存值，如果没有，就使用valuLoader 获取值，类似设置一个默认值
        Object result = this.cache.get(key, (k) -> loaderValue(key, valueLoader));
        // 如果不允许存NULL值 直接删除NULL值缓存
        boolean isEvict = !isAllowNullValues() && (result == null || result instanceof NullValue);
        if (isEvict) {
            evict(key);
        }
        return (T) fromStoreValue(result);
    }

    /**
     * 加载数据
     */
    private <T> Object loaderValue(Object key, Callable<T> valueLoader) {
        try {
            T t = valueLoader.call();
            log.debug("caffeine缓存 key={} 从库加载缓存", JSON.toJSONString(key), JSON.toJSONString(t));

            return toStoreValue(t);
        } catch (Exception e) {
            throw new LoaderCacheValueException(key, e);
        }
    }

    public void put(Object key, Object value) {
        // 允许存NULL值
        if (isAllowNullValues()) {
            log.debug("caffeine缓存 key={} put缓存，缓存值：{}", JSON.toJSONString(key), JSON.toJSONString(value));
            this.cache.put(key, toStoreValue(value));
            return;
        }

        // 不允许存NULL值
        if (value != null && !(value instanceof NullValue)) {
            log.debug("caffeine缓存 key={} put缓存，缓存值：{}", JSON.toJSONString(key), JSON.toJSONString(value));
            this.cache.put(key, toStoreValue(value));
        }
        log.debug("缓存值为NULL并且不允许存NULL值，不缓存数据");
    }

    public Object putIfAbsent(Object key, Object value) {
        log.debug("caffeine缓存 key={} putIfAbsent 缓存，缓存值：{}", JSON.toJSONString(key), JSON.toJSONString(value));
        boolean flag = !isAllowNullValues() && (value == null || value instanceof NullValue);
        if (flag) {
            return null;
        }
        Object result = this.cache.get(key, k -> toStoreValue(value));
        return fromStoreValue(result);
    }

    public void evict(Object key) {
        log.debug("caffeine缓存 key={} 清除缓存", JSON.toJSONString(key));
        this.cache.invalidate(key);
    }

    public void clear() {
        log.debug("caffeine缓存 key={} 清空缓存");
        this.cache.invalidateAll();
    }
}
