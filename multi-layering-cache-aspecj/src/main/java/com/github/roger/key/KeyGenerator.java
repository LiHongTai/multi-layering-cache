package com.github.roger.key;

import java.lang.reflect.Method;

public interface KeyGenerator {

    Object generate(Object target, Method method, Object... params);
}
