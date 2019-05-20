package com.github.roger.utils;

import com.github.roger.annotation.FirstCache;
import com.github.roger.annotation.SecondaryCache;
import com.github.roger.expression.CacheOperationExpressionEvaluator;
import com.github.roger.key.KeyGenerator;
import com.github.roger.support.CacheOperationInvoker;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.github.roger.settings.FirstCacheSetting;
import org.github.roger.settings.MultiLayeringCacheSetting;
import org.github.roger.settings.SecondaryCacheSetting;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Objects;

public class CacheAspectUtil {

    /**
     * SpEL表达式计算器
     */
    private static final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator();

    public static CacheOperationInvoker getCacheOpreationInvoker(ProceedingJoinPoint pJoinPoint) {
        //就是返回一个CacheOperationInvoker接口实现类，也即实现invoker方法
        return () -> {
            try {
                return pJoinPoint.proceed();
            } catch (Throwable ex) {
                throw new CacheOperationInvoker.ThrowableWrapperException(ex);
            }
        };

    }

    public static Method getSpecificMethod(ProceedingJoinPoint pJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) pJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        //方法可能在接口上，但我们需要来自目标类的属性。
        //如果目标类为空，则方法将保持不变。
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(pJoinPoint.getTarget());
        if (targetClass == null && pJoinPoint.getTarget() != null) {
            targetClass = pJoinPoint.getTarget().getClass();
        }
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
        //如果我们要处理带有泛型参数的方法，请找到原始方法
        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        return specificMethod;
    }

    public static Object generateKey(KeyGenerator keyGenerator, String keySpEl, Method method, ProceedingJoinPoint pJoinPoint) {

        if(StringUtils.hasText(keySpEl)){
            // 获取注解上的key属性值
            Class<?> targetClass = getTargetClass(pJoinPoint.getTarget());
            EvaluationContext evaluationContext = evaluator.createEvaluationContext(
                    method,pJoinPoint.getArgs(),pJoinPoint.getTarget(),
                    targetClass,CacheOperationExpressionEvaluator.NO_RESULT);
            AnnotatedElementKey methodCacheKey = new AnnotatedElementKey(method, targetClass);

            Object keyValue = evaluator.key(keySpEl,methodCacheKey,evaluationContext);

            return Objects.isNull(keyValue) ? "null" : keyValue;
        }

        return keyGenerator.generate(pJoinPoint.getTarget(), method, pJoinPoint.getArgs());
    }

    /**
     * 获取类信息
     *
     * @param target Object
     * @return targetClass
     */
    public static Class<?> getTargetClass(Object target) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        if (targetClass == null) {
            targetClass = target.getClass();
        }
        return targetClass;
    }

    public static MultiLayeringCacheSetting generateMultiLayeringCacheSetting(FirstCache firstCache, SecondaryCache secondaryCache) {

        FirstCacheSetting firstCacheSetting = new FirstCacheSetting(firstCache.initialCapacity(), firstCache.maximumSize(),
                firstCache.expireTime(), firstCache.timeUnit(), firstCache.expireMode());

        SecondaryCacheSetting secondaryCacheSetting = new SecondaryCacheSetting(secondaryCache.expireTime(),
                secondaryCache.preloadTime(), secondaryCache.timeUnit(), secondaryCache.forceRefresh(),
                secondaryCache.isUsePrefix(),secondaryCache.isAllowNullValue(), secondaryCache.magnification());

        return new MultiLayeringCacheSetting(firstCacheSetting,secondaryCacheSetting);
    }
}
