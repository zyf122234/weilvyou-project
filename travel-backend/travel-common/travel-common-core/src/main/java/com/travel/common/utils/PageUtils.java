package com.travel.common.utils;


public final class PageUtils {

    private static final long DEFAULT_CURRENT = 1L;
    private static final long DEFAULT_SIZE = 10L;

    private PageUtils() {
    }

    public static long safeCurrent(Long current) {
        return current == null || current < 1 ? DEFAULT_CURRENT : current;
    }

    public static long safeSize(Long size) {
        return safeSizeOrDefault(size, DEFAULT_SIZE);
    }

    public static long safeSizeOrDefault(Long size, long defaultSize) {
        long normalizedDefaultSize = defaultSize < 1 ? DEFAULT_SIZE : defaultSize;
        if (size == null || size < 1) {
            return normalizedDefaultSize;
        }
        return size;
    }
}
