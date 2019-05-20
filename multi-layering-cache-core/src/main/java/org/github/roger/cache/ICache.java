package org.github.roger.cache;

import java.util.concurrent.Callable;

/**
 * 缓存的顶级接口
 */
public interface ICache {

    /**
     * 定义缓存对象的名称，是为了区分不同的缓存对象
     * @return 返回缓存的名称
     */
    String getName();

    /**
     * @return 返回真实的缓存对象
     */
    Object getRealCache();

    /**
     *  根据key获取其对应的缓存对象，如果没有就返回null
     * @param key
     * @return 返回缓存key对应的缓存对象
     */
    Object get(Object key);

    /**
     *  根据key获取其对应的缓存对象，并将返回的缓存对象转换成对应的类型
     *  如果没有就返回null
     * @param key
     * @param type  缓存对象的类型
     * @param <T>
     * @return
     */
    <T> T get(Object key,Class<T> type);

    /**
     *  根据key获取其对应的缓存对象，并将返回的缓存对象转换成对应的类型
     *  如果对应key不存在则调用valueLoader加载数据
     * @param key
     * @param valueLoader 加载缓存的回调方法
     * @param <T>
     * @return
     */
    <T> T get(Object key, Callable<T> valueLoader);

    /**
     * 将对应key-value放到缓存，如果key原来有值就直接覆盖
     *
     * @param key   缓存key
     * @param value 缓存的值
     */
    void put(Object key, Object value);

    /**
     * 如果缓存key没有对应的值就将值put到缓存，如果有就直接返回原有的值
     * 就相当于:
     * Object existingValue = cache.get(key);
     * if (existingValue == null) {
     *     cache.put(key, value);
     *     return null;
     * } else {
     *     return existingValue;
     * }
     * @param key   缓存key
     * @param value 缓存key对应的值
     * @return 因为值本身可能为NULL，或者缓存key本来就没有对应值的时候也为NULL，
     * 所以如果返回NULL就表示已经将key-value键值对放到了缓存中
     */
    Object putIfAbsent(Object key, Object value);

    /**
     * 在缓存中删除对应的key
     *
     * @param key 缓存key
     */
    void evict(Object key);

    /**
     * 清楚缓存
     */
    void clear();

}
