package org.github.roger.enumeration;

public enum ExpireMode {

    /**
     * 每写入一次重新计算一次缓存的有效时间
     */
    WRITE("计算到期时间的标准是:距离最后一次写入时间到期时失效"),

    /**
     * 每访问一次重新计算一次缓存的有效时间
     */
    ACCESS("计算到期时间的标准是:距离最后一次访问时间到期时失效");

    private String label;

    ExpireMode(String label) {
        this.label = label;
    }
}
