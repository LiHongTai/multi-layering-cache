package com.github.roger.service.impl;

import com.github.roger.annotation.Cacheable;
import com.github.roger.annotation.FirstCache;
import com.github.roger.annotation.SecondaryCache;
import com.github.roger.domain.User;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class UserServiceImpl {


    @Cacheable(value = "user:info", key = "#userId", ignoreException = false,
            firstCache = @FirstCache(expireTime = 4, timeUnit = TimeUnit.SECONDS),
            secondaryCache = @SecondaryCache(expireTime = 100, preloadTime = 30,
                    forceRefresh = true, timeUnit = TimeUnit.SECONDS, isAllowNullValue = true))
    public User getUserById(long userId) {
        log.debug("测试正常配置的缓存方法，参数是基本类型");
        User user = new User();
        user.setUserId(userId);
        user.setAge(31);
        user.setLastName(new String[]{"w", "y", "h"});
        return user;
    }


    @Cacheable(value = "user:info", key = "#user.userId", ignoreException = false,
            firstCache = @FirstCache(expireTime = 4, timeUnit = TimeUnit.SECONDS),
            secondaryCache = @SecondaryCache(expireTime = 100, preloadTime = 30,
                    forceRefresh = true, timeUnit = TimeUnit.SECONDS, isAllowNullValue = true))
    public User saveUser(User user) {
        log.debug("测试正常配置的缓存方法，参数是基本类型");
        user.setAge(32);
        user.setLastName(new String[]{"w", "y", "h"});
        return user;
    }

    @Cacheable(value = "user:info", ignoreException = false,
            firstCache = @FirstCache(expireTime = 4, timeUnit = TimeUnit.SECONDS),
            secondaryCache = @SecondaryCache(expireTime = 100, preloadTime = 30, forceRefresh = true, timeUnit = TimeUnit.SECONDS))
    public User getUserNoKey(long userId, String[] lastName) {
        log.debug("测试没有配置key的缓存方法，参数是基本类型和数组的缓存缓存方法");
        User user = new User();
        user.setUserId(userId);
        user.setAge(31);
        user.setLastName(lastName);
        return user;
    }

}
