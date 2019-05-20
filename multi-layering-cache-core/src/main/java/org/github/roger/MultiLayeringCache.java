package org.github.roger;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.github.roger.cache.AbstractValueAdaptingCache;
import org.github.roger.cache.ICache;
import org.github.roger.cache.caffeine.CaffeineCache;
import org.github.roger.cache.redis.RedisCache;
import org.github.roger.enumeration.RedisPubSubMessageType;
import org.github.roger.listener.RedisPublisher;
import org.github.roger.message.RedisPubSubMessage;
import org.github.roger.settings.MultiLayeringCacheSetting;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.concurrent.Callable;

@Slf4j
public class MultiLayeringCache extends AbstractValueAdaptingCache {

    /**
     * redis 客户端
     */
    private RedisTemplate<String, Object> redisTemplate;

    private AbstractValueAdaptingCache firstCache;

    private AbstractValueAdaptingCache secondCache;

    private boolean useFirstCache = true;
    private MultiLayeringCacheSetting multilayeringCacheSetting;

    public MultiLayeringCache(RedisTemplate<String,Object> redisTemplate, AbstractValueAdaptingCache firstCache, AbstractValueAdaptingCache secondCache, MultiLayeringCacheSetting multilayeringCacheSetting) {
        this(secondCache.getName(),redisTemplate,firstCache,secondCache,true,multilayeringCacheSetting);
    }

    public MultiLayeringCache(String name,RedisTemplate<String, Object> redisTemplate,AbstractValueAdaptingCache firstCache, AbstractValueAdaptingCache secondCache, boolean useFirstCache,MultiLayeringCacheSetting multilayeringCacheSetting) {
        super(name);
        this.redisTemplate = redisTemplate;
        this.firstCache = firstCache;
        this.secondCache = secondCache;
        this.useFirstCache = useFirstCache;
        this.multilayeringCacheSetting = multilayeringCacheSetting;
    }

    public Object getRealCache() {
        return this;
    }

    public Object get(Object key) {
        Object storeValue = null;
        if(useFirstCache){
            storeValue = firstCache.get(key);
            log.debug("查询一级缓存。 key={},返回值是:{}", key, JSON.toJSONString(storeValue));
        }
        if(storeValue == null){
            storeValue = secondCache.get(key);
            firstCache.putIfAbsent(key, storeValue);
            log.debug("查询二级缓存,并将数据放到一级缓存。 key={},返回值是:{}", key, JSON.toJSONString(storeValue));
        }
        return fromStoreValue(storeValue);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        if (useFirstCache) {
            Object result = firstCache.get(key, type);
            log.debug("查询一级缓存。 key={},返回值是:{}", key, JSON.toJSONString(result));
            if (result != null) {
                return (T) fromStoreValue(result);
            }
        }

        T result = secondCache.get(key, type);
        firstCache.putIfAbsent(key, result);
        log.debug("查询二级缓存,并将数据放到一级缓存。 key={},返回值是:{}", key, JSON.toJSONString(result));
        return result;
    }

    public <T> T get(Object key, Callable<T> valueLoader) {
        if (useFirstCache) {
            Object result = firstCache.get(key);
            log.debug("查询一级缓存。 key={},返回值是:{}", key, JSON.toJSONString(result));
            if (result != null) {
                return (T) fromStoreValue(result);
            }
        }
        T result = secondCache.get(key, valueLoader);
        firstCache.putIfAbsent(key, result);
        log.debug("查询二级缓存,并将数据放到一级缓存。 key={},返回值是:{}", key, JSON.toJSONString(result));
        return result;
    }

    public void put(Object key, Object value) {
        secondCache.put(key, value);
        // 删除一级缓存
        if (useFirstCache) {
            deleteFirstCache(key);
        }
    }

    public Object putIfAbsent(Object key, Object value) {
        Object result = secondCache.putIfAbsent(key, value);
        // 删除一级缓存
        if (useFirstCache) {
           deleteFirstCache(key);
        }
        return result;
    }

    public void evict(Object key) {
        // 删除的时候要先删除二级缓存再删除一级缓存，否则有并发问题
        secondCache.evict(key);
        // 删除一级缓存
        if (useFirstCache) {
            deleteFirstCache(key);
        }
    }

    private void deleteFirstCache(Object key) {
        // 删除一级缓存需要用到redis的Pub/Sub（订阅/发布）模式，否则集群中其他服服务器节点的一级缓存数据无法删除
        RedisPubSubMessage message = new RedisPubSubMessage();
        message.setCacheName(getName());
        message.setKey(key);
        message.setMessageType(RedisPubSubMessageType.EVICT);
        // 发布消息
        RedisPublisher.publisher(redisTemplate, new ChannelTopic(getName()), message);
    }

    public void clear() {
        // 删除的时候要先删除二级缓存再删除一级缓存，否则有并发问题
        secondCache.clear();
        if (useFirstCache) {
            // 清除一级缓存需要用到redis的订阅/发布模式，否则集群中其他服服务器节点的一级缓存数据无法删除
            RedisPubSubMessage message = new RedisPubSubMessage();
            message.setCacheName(getName());
            message.setMessageType(RedisPubSubMessageType.CLEAR);
            // 发布消息
            RedisPublisher.publisher(redisTemplate, new ChannelTopic(getName()), message);
        }
    }

    public ICache getFirstCache() {
        return firstCache;
    }

    public ICache getSecondCache() {
        return secondCache;
    }

    public boolean isAllowNullValues() {
        return secondCache.isAllowNullValues();
    }
}
