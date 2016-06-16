package com.vivavideo.imkit.userInfoCache;

import java.util.LinkedHashMap;
import java.util.Map;

class RongUserCache<K, V> {
    private final LinkedHashMap<K, V> map;
    private int size;
    private int maxSize;

    RongUserCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
    }

    V get(K key) {
        if (key == null) {
            return null;
        }

        synchronized (this) {
            return map.get(key);
        }
    }

    V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        V previous;
        synchronized (this) {
            size++;
            previous = map.put(key, value);
            if (previous != null) {
                size--;
            }
            if (size > maxSize) {
                Map.Entry<K, V> toEvict = map.entrySet().iterator().next();
                if (toEvict == null) {
                    return previous;
                }
                map.remove(toEvict.getKey());
                size--;
            }
        }
        return previous;
    }
}
