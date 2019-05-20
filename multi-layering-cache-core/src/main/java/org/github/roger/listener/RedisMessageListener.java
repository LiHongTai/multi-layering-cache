package org.github.roger.listener;

import com.alibaba.fastjson.JSON;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.github.roger.MultiLayeringCache;
import org.github.roger.cache.ICache;
import org.github.roger.manager.AbstractCacheManager;
import org.github.roger.message.RedisPubSubMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.Collection;

@Setter
@Slf4j
public class RedisMessageListener extends MessageListenerAdapter {

    /**
     * 缓存管理器
     */
    private AbstractCacheManager cacheManager;


    @Override
    public void onMessage(Message message, byte[] pattern) {
        super.onMessage(message, pattern);
        // 解析订阅发布的信息，获取缓存的名称和缓存的key
        RedisPubSubMessage redisPubSubMessage = (RedisPubSubMessage) cacheManager.getRedisTemplate()
                .getValueSerializer().deserialize(message.getBody());
        log.debug("redis消息订阅者接收到频道【{}】发布的消息。消息内容：{}", new String(message.getChannel()), JSON.toJSONString(redisPubSubMessage));

        // 根据缓存名称获取多级缓存，可能有多个
        Collection<ICache> caches = cacheManager.getCache(redisPubSubMessage.getCacheName());
        for (ICache cache : caches) {
            // 判断缓存是否是多级缓存
            if (cache != null && cache instanceof MultiLayeringCache) {
                switch (redisPubSubMessage.getMessageType()) {
                    case EVICT:
                        // 获取一级缓存，并删除一级缓存数据
                        ((MultiLayeringCache) cache).getFirstCache().evict(redisPubSubMessage.getKey());
                        log.info("删除一级缓存{}数据,key={}", redisPubSubMessage.getCacheName(), redisPubSubMessage.getKey());
                        break;

                    case CLEAR:
                        // 获取一级缓存，并删除一级缓存数据
                        ((MultiLayeringCache) cache).getFirstCache().clear();
                        log.info("清除一级缓存{}数据", redisPubSubMessage.getCacheName());
                        break;

                    default:
                        log.error("接收到没有定义的订阅消息频道数据");
                        break;
                }

            }
        }
    }
}
