package org.github.roger.settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.github.roger.enumeration.ExpireMode;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FirstCacheSetting implements Serializable {

    /**
     * 缓存初始Size
     */
    private int initialCapacity = 10;

    /**
     * 缓存最大Size
     */
    private int maximumSize = 500;

    /**
     * 缓存有效时间
     */
    private int expireTime = 0;

    /**
     * 缓存时间单位
     */
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    /**
     * 缓存失效模式{@link ExpireMode}
     */
    private ExpireMode expireMode = ExpireMode.WRITE;

    public boolean isAllowNullValues() {
        return false;
    }
}
