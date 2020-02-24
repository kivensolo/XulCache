package com.github.xulcache.cacherecycle;

import com.github.xulcache.CacheModel;

import java.util.Collection;

interface RecycleStrategy {

    /**
     * 根据特定的回收算法返回一个可回收的数据对象
     *
     * @param caches 缓存数据集合
     * @return 可回收对象，若未找到，则返回null
     */
    CacheModel findRecycledCache(Collection<CacheModel> caches);
}
