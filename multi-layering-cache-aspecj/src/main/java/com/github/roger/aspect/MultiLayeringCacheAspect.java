package com.github.roger.aspect;

import com.github.roger.annotation.CacheEvict;
import com.github.roger.annotation.CachePut;
import com.github.roger.annotation.Cacheable;
import com.github.roger.expression.CacheOperationExpressionEvaluator;
import com.github.roger.key.KeyGenerator;
import com.github.roger.key.impl.DefaultKeyGenerator;
import com.github.roger.support.CacheOperationInvoker;
import com.github.roger.utils.CacheAspectUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.github.roger.cache.ICache;
import org.github.roger.exception.SerializationException;
import org.github.roger.manager.ICacheManager;
import org.github.roger.settings.FirstCacheSetting;
import org.github.roger.settings.MultiLayeringCacheSetting;
import org.github.roger.settings.SecondaryCacheSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Collection;

@Aspect
@Slf4j
public class MultiLayeringCacheAspect {

    private static final String CACHE_KEY_ERROR_MESSAGE = "缓存Key %s 不能为NULL";
    private static final String CACHE_NAME_ERROR_MESSAGE = "缓存名称不能为NULL";

    @Autowired(required = false)//如果自定义了使用自定义的，否则使用默认的
    private KeyGenerator keyGenerator = new DefaultKeyGenerator();

    @Autowired
    private ICacheManager iCacheManager;

    @Pointcut("@annotation(com.github.roger.annotation.Cacheable)")
    public void cacheablePointCut(){
    }

    @Pointcut("@annotation(com.github.roger.annotation.CachePut)")
    public void cachePutPointCut(){
    }

    @Pointcut("@annotation(com.github.roger.annotation.CacheEvict)")
    public void cacheEvictPointCut(){
    }

    @Around("cacheablePointCut()")
    public Object cacheableAroundAdvice(ProceedingJoinPoint pJoinPoint) throws Throwable{
        //通过非缓存的方式获取数据的操作类接口
        CacheOperationInvoker aopInvoker = CacheAspectUtil.getCacheOpreationInvoker(pJoinPoint);

        //获取正在执行的目标方法
        Method method = CacheAspectUtil.getSpecificMethod(pJoinPoint);

        //获取方法上的Cacheable注解
        Cacheable cacheable = AnnotationUtils.findAnnotation(method,Cacheable.class);
        try {
            //执行查询缓存的方法
            return executeCachealbe(aopInvoker, method, cacheable, pJoinPoint);
        }catch (SerializationException sex){
            // 如果是序列化异常需要先删除原有缓存
            String[] cacheNames = cacheable.cacheNames();
            // 删除缓存
            delete(cacheNames, cacheable.key(), method, pJoinPoint);

            // 忽略操作缓存过程中遇到的异常
            if (cacheable.ignoreException()) {
                log.warn(sex.getMessage(), sex);
                return aopInvoker.invoke();
            }
            throw sex;
        }catch (Exception ex){
            // 忽略操作缓存过程中遇到的异常
            if (cacheable.ignoreException()) {
                log.warn(ex.getMessage(), ex);
                return aopInvoker.invoke();
            }
            throw ex;
        }
    }

    private Object executeCachealbe(CacheOperationInvoker aopInvoker, Method method, Cacheable cacheable, ProceedingJoinPoint pJoinPoint) {
        // 解析SpEL表达式获取cacheName和key
        String[] cacheNames = cacheable.cacheNames();
        Assert.notEmpty(cacheable.cacheNames(), CACHE_NAME_ERROR_MESSAGE);
        String cacheName = cacheNames[0];

        Object key = CacheAspectUtil.generateKey(keyGenerator,cacheable.key(), method, pJoinPoint);
        Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, cacheable.key()));

        // 构造多级缓存配置信息
        MultiLayeringCacheSetting layeringCacheSetting = CacheAspectUtil.generateMultiLayeringCacheSetting(cacheable.firstCache(),cacheable.secondaryCache());

        // 通过cacheName和缓存配置获取Cache
        ICache iCache = iCacheManager.getCache(cacheName, layeringCacheSetting);

        // 通Cache获取值
        return iCache.get(key, () -> aopInvoker.invoke());

    }

    @Around("cacheEvictPointCut()")
    public Object cacheEvicArountAdvice(ProceedingJoinPoint pJoinPoint) throws Throwable{
        //通过非缓存的方式获取数据的操作类接口
        CacheOperationInvoker aopInvoker = CacheAspectUtil.getCacheOpreationInvoker(pJoinPoint);

        //获取正在执行的目标方法
        Method method = CacheAspectUtil.getSpecificMethod(pJoinPoint);

        //获取方法上的Cacheable注解
        CacheEvict cacheEvict = AnnotationUtils.findAnnotation(method,CacheEvict.class);
        try{
            return executeCacheEvict(aopInvoker,method,cacheEvict,pJoinPoint);
        }catch (Exception ex){
            // 忽略操作缓存过程中遇到的异常
            if (cacheEvict.ignoreException()) {
                log.warn(ex.getMessage(), ex);
                return aopInvoker.invoke();
            }
            throw ex;
        }
    }

    private Object executeCacheEvict(CacheOperationInvoker aopInvoker, Method method, CacheEvict cacheEvict, ProceedingJoinPoint pJoinPoint) throws Throwable {
        // 解析SpEL表达式获取cacheName和key
        String[] cacheNames = cacheEvict.cacheNames();
        Assert.notEmpty(cacheEvict.cacheNames(), CACHE_NAME_ERROR_MESSAGE);
        // 判断是否删除所有缓存数据
        if(cacheEvict.allEntries()){
            // 删除所有缓存数据（清空）
            for (String cacheName : cacheNames) {
                Collection<ICache> iCaches = iCacheManager.getCache(cacheName);
                if (CollectionUtils.isEmpty(iCaches)) {
                    // 如果没有找到Cache就新建一个默认的
                    ICache iCache = iCacheManager.getCache(cacheName,
                            new MultiLayeringCacheSetting(new FirstCacheSetting(), new SecondaryCacheSetting()));
                    iCache.clear();
                } else {
                    for (ICache iCache : iCaches) {
                        iCache.clear();
                    }
                }
            }
        }else{
            delete(cacheNames,cacheEvict.key(),method,pJoinPoint);
        }
        return aopInvoker.invoke();
    }


    /**
     * 删除执行缓存名称上的指定key
     * */
    private void delete(String[] cacheNames, String keySpEL, Method method, ProceedingJoinPoint pJoinPoint) {
        Object key = CacheAspectUtil.generateKey(keyGenerator,keySpEL, method, pJoinPoint);
        Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, keySpEL));
        for (String cacheName : cacheNames) {
            Collection<ICache> iCaches = iCacheManager.getCache(cacheName);
            if (CollectionUtils.isEmpty(iCaches)) {
                // 如果没有找到Cache就新建一个默认的
                ICache iCache = iCacheManager.getCache(cacheName,
                        new MultiLayeringCacheSetting(new FirstCacheSetting(), new SecondaryCacheSetting()));
                iCache.evict(key);
            } else {
                for (ICache iCache : iCaches) {
                    iCache.evict(key);
                }
            }
        }
    }

    @Around("cachePutPointCut()")
    public Object cachePutAroundAdvice(ProceedingJoinPoint pJoinPoint) throws Throwable{

        //通过非缓存的方式获取数据的操作类接口
        CacheOperationInvoker aopInvoker = CacheAspectUtil.getCacheOpreationInvoker(pJoinPoint);

        //获取正在执行的目标方法
        Method method = CacheAspectUtil.getSpecificMethod(pJoinPoint);

        //获取方法上的CachePut注解
        CachePut cachePut = AnnotationUtils.findAnnotation(method,CachePut.class);

        try {
            // 执行查询缓存方法
            return executeCachePut(aopInvoker, method, cachePut, pJoinPoint);
        } catch (Exception e) {
            // 忽略操作缓存过程中遇到的异常
            if (cachePut.ignoreException()) {
                log.warn(e.getMessage(), e);
                return aopInvoker.invoke();
            }
            throw e;
        }

    }

    private Object executeCachePut(CacheOperationInvoker aopInvoker, Method method, CachePut cachePut, ProceedingJoinPoint pJoinPoint) throws Throwable{

        // 解析SpEL表达式获取cacheName和key
        String[] cacheNames = cachePut.cacheNames();
        Assert.notEmpty(cachePut.cacheNames(), CACHE_NAME_ERROR_MESSAGE);

        Object key = CacheAspectUtil.generateKey(keyGenerator,cachePut.key(), method, pJoinPoint);
        Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, cachePut.key()));

        // 构造多级缓存配置信息
        MultiLayeringCacheSetting layeringCacheSetting = CacheAspectUtil.generateMultiLayeringCacheSetting(cachePut.firstCache(),cachePut.secondaryCache());

        // 指定调用方法获取缓存值
        Object result = aopInvoker.invoke();
        for (String cacheName : cacheNames) {
            // 通过cacheName和缓存配置获取Cache
            ICache iCache = iCacheManager.getCache(cacheName, layeringCacheSetting);
            iCache.put(key, result);
        }
        return result;
    }
}
