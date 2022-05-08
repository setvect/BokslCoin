package com.setvect.bokslcoin.autotrading.util;

import java.util.ArrayList;

/**
 * 사이즈가 제한된 큐. <br>
 *
 * @param <K> Type
 */
public class LimitedSizeQueue<K> extends ArrayList<K> {
    /**
     * 최대 사이즈
     */
    private final int maxSize;

    /**
     * @param size 최대 저장 사이즈
     */
    public LimitedSizeQueue(final int size) {
        this.maxSize = size;
    }

    /**
     * TODO index와 maxSize 비교
     */
    @Override
    public void add(int index, final K k) {
        super.add(index, k);
        if (size() > maxSize) {
            removeRange(maxSize, size());
        }
    }

    @Override
    public boolean add(final K k) {
        boolean r = super.add(k);
        if (size() > maxSize) {
            removeRange(0, size() - maxSize - 1);
        }
        return r;
    }
}
