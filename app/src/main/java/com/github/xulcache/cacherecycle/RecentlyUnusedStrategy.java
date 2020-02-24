package com.github.xulcache.cacherecycle;


import com.github.xulcache.CacheModel;

import java.util.Collection;

class RecentlyUnusedStrategy implements RecycleStrategy {

    @Override
    public CacheModel findRecycledCache(Collection<CacheModel> caches) {
        CacheModel oldestData = null;
        for (CacheModel cacheData : caches) {
            if (oldestData == null) {
                oldestData = cacheData;
            } else {
                if (cacheData.getLastAccessTime() < oldestData.getLastAccessTime()) {
                    oldestData = cacheData;
                }
            }
        }

        return oldestData;
    }
}
