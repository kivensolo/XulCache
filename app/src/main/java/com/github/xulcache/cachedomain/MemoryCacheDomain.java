package com.github.xulcache.cachedomain;


import com.github.xulcache.cacheimplement.MemoryCache;

public class MemoryCacheDomain extends MemoryCache {

    public MemoryCacheDomain(long maxSize, int maxCount) {
        super(maxSize, maxCount);
    }
}
