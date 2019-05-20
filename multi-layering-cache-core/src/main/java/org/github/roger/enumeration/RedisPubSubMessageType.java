package org.github.roger.enumeration;

import lombok.Getter;

@Getter
public enum RedisPubSubMessageType {

    /**
     * 删除缓存
     */
    EVICT("删除缓存"),

    /**
     * 清空缓存
     */
    CLEAR("清空缓存");

    private String label;

    RedisPubSubMessageType(String label) {
        this.label = label;
    }
}
