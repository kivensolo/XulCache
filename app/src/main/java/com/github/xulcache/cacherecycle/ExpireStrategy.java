package com.github.xulcache.cacherecycle;

import com.github.xulcache.CacheDomain;
import com.github.xulcache.CacheModel;

import java.util.Collection;

class ExpireStrategy implements RecycleStrategy {

    private final CacheDomain _cacheDomain;

    public ExpireStrategy(CacheDomain domain) {
        _cacheDomain = domain;
    }

    @Override
    public CacheModel findRecycledCache(Collection<CacheModel> caches) {
        CacheModel cacheModel = null;
        for (CacheModel cache : caches) {
            if (cache != null && _cacheDomain.isExpired(cache)) {
                cacheModel = cache;
                break;
            }
        }

        return cacheModel;
    }
}
