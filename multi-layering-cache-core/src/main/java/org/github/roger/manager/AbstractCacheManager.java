package org.github.roger.manager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.github.roger.cache.ICache;
import org.github.roger.listener.RedisMessageListener;
import org.github.roger.settings.MultiLayeringCacheSetting;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class AbstractCacheManager implements ICacheManager,InitializingBean,DisposableBean,SmartLifecycle {

    /**
     * redis pub/sub 容器
     */
    private final RedisMessageListenerContainer container = new RedisMessageListenerContainer();

    /**
     * redis pub/sub 监听器
     */
    private final RedisMessageListener messageListener = new RedisMessageListener();


    /**
     * 缓存容器
     * 外层key是cache_name
     * 里层key是[一级缓存有效时间-二级缓存有效时间-二级缓存自动刷新时间]
     */
    private final ConcurrentMap<String, ConcurrentMap<String, ICache>> cacheContainer = new ConcurrentHashMap<>(16);

    /**
     * 缓存名称容器
     */
    private volatile Set<String> cacheNames = new LinkedHashSet<>();

    /**
     * CacheManager 容器
     */
    protected static Set<AbstractCacheManager> cacheManagers = new LinkedHashSet<>();

    /**
     * redis 客户端
     */
    @Getter
    protected RedisTemplate<String, Object> redisTemplate;

    public static Set<AbstractCacheManager> getCacheManager() {
        return cacheManagers;
    }

    @Override
    public Collection<ICache> getCache(String name) {
        ConcurrentMap<String, ICache> cacheMap = this.cacheContainer.get(name);
        if (CollectionUtils.isEmpty(cacheMap)) {
            return Collections.emptyList();
        }
        return cacheMap.values();
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheNames;
    }

    // Lazy cache initialization on access
    @Override
    public ICache getCache(String name, MultiLayeringCacheSetting multiLayeringCacheSetting) {
        // 第一次获取缓存Cache，如果有直接返回,如果没有加锁往容器里里面放Cache
        ConcurrentMap<String, ICache> cacheMap = this.cacheContainer.get(name);
        if (!CollectionUtils.isEmpty(cacheMap)) {
            if (cacheMap.size() > 1) {
                log.warn("缓存名称为 {} 的缓存,存在两个不同的过期时间配置，请一定注意保证缓存的key唯一性，否则会出现缓存过期时间错乱的情况", name);
            }
            ICache iCache = cacheMap.get(multiLayeringCacheSetting.getInternalKey());
            if (iCache != null) {
                return iCache;
            }
        }

        // 第二次获取缓存Cache，加锁往容器里里面放Cache
        synchronized (this.cacheContainer) {
            cacheMap = this.cacheContainer.get(name);
            if (!CollectionUtils.isEmpty(cacheMap)) {
                // 从容器中获取缓存
                ICache iCache = cacheMap.get(multiLayeringCacheSetting.getInternalKey());
                if (iCache != null) {
                    return iCache;
                }
            } else {
                cacheMap = new ConcurrentHashMap<>(16);
                cacheContainer.put(name, cacheMap);
                // 更新缓存名称
                updateCacheNames(name);
                // 创建redis监听
                addMessageListener(name);
            }

            // 新建一个Cache对象
            ICache iCache = getMissingCache(name, multiLayeringCacheSetting);
            if (iCache != null) {
                // 装饰Cache对象
                iCache = decorateCache(iCache);
                // 将新的Cache对象放到容器
                cacheMap.put(multiLayeringCacheSetting.getInternalKey(), iCache);
                //同一缓存名称，缓存的过期时间设置要唯一
                if (cacheMap.size() > 1) {
                    log.warn("缓存名称为 {} 的缓存,存在两个不同的过期时间配置，请一定注意保证缓存的key唯一性，否则会出现缓存过期时间错乱的情况", name);
                }
            }

            return iCache;
        }
    }

    /**
     * 根据缓存名称在CacheManager中没有找到对应Cache时，通过该方法新建一个对应的Cache实例
     *
     * @param name                 缓存名称
     * @param multiLayeringCacheSetting 缓存配置
     * @return {@link ICache}
     */
    protected abstract ICache getMissingCache(String name, MultiLayeringCacheSetting multiLayeringCacheSetting);

    /**
     * 更新缓存名称容器
     *
     * @param name 需要添加的缓存名称
     */
    private void updateCacheNames(String name) {
        cacheNames.add(name);
    }

    /**
     * 获取Cache对象的装饰示例
     *
     * @param iCache 需要添加到CacheManager的Cache实例
     * @return 装饰过后的Cache实例
     */
    protected ICache decorateCache(ICache iCache) {
        return iCache;
    }

    /**
     * 添加消息监听
     *
     * @param name 缓存名称
     */
    protected void addMessageListener(String name) {
        container.addMessageListener(messageListener, new ChannelTopic(name));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        messageListener.setCacheManager(this);
        container.setConnectionFactory(getRedisTemplate().getConnectionFactory());
        container.afterPropertiesSet();
        messageListener.afterPropertiesSet();
    }

    @Override
    public void destroy() throws Exception {
        container.destroy();
    }

    @Override
    public boolean isAutoStartup() {
        return container.isAutoStartup();
    }

    @Override
    public void stop(Runnable callback) {
        container.stop(callback);
    }

    @Override
    public void start() {
        container.start();
    }

    @Override
    public void stop() {
        container.stop();
    }

    @Override
    public boolean isRunning() {
        return container.isRunning();
    }

    @Override
    public int getPhase() {
        return container.getPhase();
    }
}
