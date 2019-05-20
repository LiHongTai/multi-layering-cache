package com.github.roger.expression;

import lombok.Getter;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * 描述表达式计算期间使用的根对象。
 */
@Getter
public class CacheExpressionRootObject {
    private final Method method;

    private final Object[] args;

    private final Object target;

    private final Class<?> targetClass;

    public CacheExpressionRootObject(Method method, Object[] args, Object target, Class<?> targetClass) {
        Assert.notNull(method, "Method is required");
        Assert.notNull(targetClass, "targetClass is required");
        this.method = method;
        this.target = target;
        this.targetClass = targetClass;
        this.args = args;
    }

    public String getMethodName() {
        return this.method.getName();
    }
}
