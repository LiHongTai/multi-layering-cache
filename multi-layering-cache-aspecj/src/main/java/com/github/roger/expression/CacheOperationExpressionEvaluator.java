package com.github.roger.expression;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheOperationExpressionEvaluator extends CachedExpressionEvaluator {

    /**
     * 指示没有结果变量
     */
    public static final Object NO_RESULT = new Object();

    /**
     * 指示结果变量根本不能使用
     */
    public static final Object RESULT_UNAVAILABLE = new Object();

    /**
     *  保存结果对象的变量的名称。
     */
    public static final String RESULT_VARIABLE = "result";

    private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

    private final Map<ExpressionKey, Expression> cacheNameCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

    private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

    private final Map<ExpressionKey, Expression> unlessCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

    private final Map<AnnotatedElementKey, Method> targetMethodCache =
            new ConcurrentHashMap<AnnotatedElementKey, Method>(64);

    public EvaluationContext createEvaluationContext(Method method, Object[] args, Object target, Class<?> targetClass) {

        return createEvaluationContext(method, args, target, targetClass, NO_RESULT);
    }

    public EvaluationContext createEvaluationContext(Method method, Object[] args,
                                                     Object target, Class<?> targetClass, Object result) {

        CacheExpressionRootObject rootObject = new CacheExpressionRootObject(
                method, args, target, targetClass);
        Method targetMethod = getTargetMethod(targetClass, method);
        CacheEvaluationContext evaluationContext = new CacheEvaluationContext(
                rootObject, targetMethod, args, getParameterNameDiscoverer());
        if (result == RESULT_UNAVAILABLE) {
            evaluationContext.addUnavailableVariable(RESULT_VARIABLE);
        } else if (result != NO_RESULT) {
            evaluationContext.setVariable(RESULT_VARIABLE, result);
        }
        return evaluationContext;
    }


    private Method getTargetMethod(Class<?> targetClass, Method method) {
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
        Method targetMethod = this.targetMethodCache.get(methodKey);
        if(targetMethod == null){
            targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
            if (targetMethod == null) {
                targetMethod = method;
            }
            this.targetMethodCache.put(methodKey, targetMethod);
        }
        return targetMethod;
    }


    public Object key(String expression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {

        return getExpression(this.keyCache, methodKey, expression).getValue(evalContext);
    }

    public Object cacheName(String expression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {

        return getExpression(this.cacheNameCache, methodKey, expression).getValue(evalContext);
    }

    public boolean condition(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
        return getExpression(this.conditionCache, methodKey, conditionExpression).getValue(evalContext, boolean.class);
    }

    public boolean unless(String unlessExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
        return getExpression(this.unlessCache, methodKey, unlessExpression).getValue(evalContext, boolean.class);
    }

    /**
     * Clear all caches.
     */
    void clear() {
        this.keyCache.clear();
        this.conditionCache.clear();
        this.unlessCache.clear();
        this.targetMethodCache.clear();
    }
}
