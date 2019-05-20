package com.github.roger.support;

import lombok.Getter;

public interface CacheOperationInvoker {

    /**
     * 调用此实例定义的缓存操作.
     *      如果缓存中获取不到对应的信息，使用此方法调用具体的获取数据
     * @return
     * @throws ThrowableWrapperException
     */
    Object invoke() throws ThrowableWrapperException;

    class ThrowableWrapperException extends Exception {

        @Getter
        private final Throwable original;

        public ThrowableWrapperException(Throwable original) {
            super(original.getMessage(), original);
            this.original = original;
        }
    }
}
