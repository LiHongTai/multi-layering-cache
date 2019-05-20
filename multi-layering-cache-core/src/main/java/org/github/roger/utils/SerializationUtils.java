package org.github.roger.utils;

/**
 * 序列化工具类
 *
 */
public abstract class SerializationUtils {

    public static final byte[] EMPTY_ARRAY = new byte[0];

    public static boolean isEmpty(byte[] data) {
        return (data == null || data.length == 0);
    }
}
