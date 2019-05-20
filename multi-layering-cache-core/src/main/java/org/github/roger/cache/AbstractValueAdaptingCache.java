package org.github.roger.cache;

import com.alibaba.fastjson.JSON;
import org.github.roger.support.NullValue;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;

public abstract class AbstractValueAdaptingCache implements ICache{

    //缓存名称
    private String name;

    public AbstractValueAdaptingCache(String name) {
        Assert.notNull(name,"缓存名称不能为空");
        this.name = name;
    }

    /**
     * 缓存对象是否允许为空
     * @return true 允许 false 不允许
     */
    public abstract boolean isAllowNullValues();

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return (T) fromStoreValue(get(key));
    }

    protected Object fromStoreValue(Object storeValue) {
        if(isAllowNullValues() && storeValue instanceof NullValue){
            return null;
        }
        return storeValue;
    }

    protected Object toStoreValue(Object userValue){
        if(isAllowNullValues() && userValue == null){
            return NullValue.INSTANCE;
        }
        return userValue;
    }


    /**
     * {@link #get(Object, Callable)} 方法加载缓存值的包装异常
     */
    public class LoaderCacheValueException extends RuntimeException {

        private final Object key;

        public LoaderCacheValueException(Object key, Throwable ex) {
            super(String.format("加载key为 %s 的缓存数据,执行被缓存方法异常", JSON.toJSONString(key)), ex);
            this.key = key;
        }

        public Object getKey() {
            return this.key;
        }
    }
}
