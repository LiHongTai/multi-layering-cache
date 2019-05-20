package com.github.roger.cahce.aspect;

import com.github.roger.cahce.config.ICacheManagerConfig;
import com.github.roger.domain.User;
import com.github.roger.service.impl.UserServiceImpl;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

// SpringJUnit4ClassRunner再Junit环境下提供Spring TestContext Framework的功能。
@RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration用来加载配置ApplicationContext，其中classes用来加载配置类
@ContextConfiguration(classes = {ICacheManagerConfig.class})
@Log4j
public class CacheAspectTest {

    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Test
    public void testGetUserById(){
        long userId = 111;
        // 二级缓存key ： user:info:111 有效时间100秒 强制刷新时间 30秒
        // 根据UserId获取数据，首先从缓存中获取，如果获取不到，去数据库中获取
        // 然后把缓存写入一级，二级缓存
        User user = userService.getUserById(userId);
        sleep(10);
        // 已经写入二级redis缓存，因此可以从缓存中获取
        Object result = redisTemplate.opsForValue().get("user:info:111");
        Assert.assertNotNull(result);

        sleep(65);
        long ttl = redisTemplate.getExpire("user:info:111");
        log.debug("进入强制刷新缓存时间段 ttl = " + ttl);
        Assert.assertTrue(ttl > 0 && ttl < 30);
        userService.getUserById(userId);
        // 因为是开启线程去刷新二级缓存，因此这里在获取有效时间的时候，需要延后几秒，才能获取到新的有效时间
        sleep(2);
        long ttl2 = redisTemplate.getExpire("user:info:111");
        log.debug("强制刷新缓存后，有效时间发生变化 ttl2 = " + ttl2);
        Assert.assertTrue(ttl2 > 50);

    }

    @Test
    public void testSaveUser(){
        User user = new User();
        // 二级缓存key ： user:info:32有效时间100秒 强制刷新时间 30秒
        // @Cacheable 注解的value值 + 传递参数对象的 user.getUserId() 组成二级缓存的缓存key
        userService.saveUser(user);
        log.debug("保存User对象 " + user);
    }

    @Test
    public void testGetUserNoKey(){
        userService.getUserNoKey(123,new String[]{"w","y","z"});
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time * 1000);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
