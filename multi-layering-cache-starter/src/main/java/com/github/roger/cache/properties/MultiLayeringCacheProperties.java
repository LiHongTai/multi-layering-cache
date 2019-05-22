package com.github.roger.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties( prefix = "spring.multi-layering-cache")
@Data
public class MultiLayeringCacheProperties {

    /**
     * 命名空间，必须唯一般使用服务名
     */
    private String namespace;
}
