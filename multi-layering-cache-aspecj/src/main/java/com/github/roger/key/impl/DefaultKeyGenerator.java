package com.github.roger.key.impl;

import com.github.roger.key.DefaultKey;
import com.github.roger.key.KeyGenerator;

import java.lang.reflect.Method;

public class DefaultKeyGenerator implements KeyGenerator {

    public Object generate(Object target, Method method, Object... params) {
        return generateKey(params);
    }

    private Object generateKey(Object[] params) {
        if (params == null || params.length == 0) {
            return DefaultKey.EMPTY;
        }
        if (params.length == 1) {
            Object param = params[0];
            if (param != null && !param.getClass().isArray()) {
                return param;
            }
        }
        return new DefaultKey(params);
    }
}
