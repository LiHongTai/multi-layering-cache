package com.github.roger.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
    二级缓存配置项
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface SecondaryCache {

    /**
     * 缓存有效时间
     */
    long expireTime() default 0;

    /**
     * 缓存主动在失效前强制刷新缓存的时间
     * 建议是： preloadTime default expireTime * 0.2
     *
     * @return long
     */
    long preloadTime() default 0;

    /**
     * 时间单位 {@link TimeUnit}
     */
    TimeUnit timeUnit() default TimeUnit.HOURS;

    /**
     * 是否强制刷新（走数据库），默认是false
     */
    boolean forceRefresh() default false;


    boolean isUsePrefix() default true;

    /**
     * 是否允许存NULL值
     */
    boolean isAllowNullValue() default false;

    /**
     * 非空值和null值之间的时间倍率，默认是1。allowNullValuedefaulttrue才有效
     *
     * 如配置缓存的有效时间是200秒，倍率这设置成10，
     * 那么当缓存value为null时，缓存的有效时间将是20秒，非空时为200秒
     */
    int magnification() default 1;
}

