package org.github.roger.message;

import lombok.Data;
import org.github.roger.enumeration.RedisPubSubMessageType;

import java.io.Serializable;

@Data
public class RedisPubSubMessage implements Serializable {

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存key
     */
    private Object key;

    /**
     * 消息类型
     */
    private RedisPubSubMessageType messageType;

}
