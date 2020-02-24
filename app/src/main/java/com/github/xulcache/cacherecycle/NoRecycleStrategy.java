package com.github.xulcache.cacherecycle;


import com.github.xulcache.CacheModel;

import java.util.Collection;

class NoRecycleStrategy implements RecycleStrategy {

    @Override
    public CacheModel findRecycledCache(Collection<CacheModel> caches) {
        return null;
    }
}
