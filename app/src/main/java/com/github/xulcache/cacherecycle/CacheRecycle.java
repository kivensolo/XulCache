package com.github.xulcache.cacherecycle;

import com.github.xulcache.CacheDomain;
import com.github.xulcache.CacheModel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class CacheRecycle {

    // 回收策略枚举
    public static final int STRATEGY_NO_RECYCLE = 0x0001;
    public static final int STRATEGY_EXPIRED = 0x0002;
    public static final int STRATEGY_RECENTLY_UNUSED = 0x0003;

    private CacheDomain _cacheDomain;
    private ConcurrentLinkedQueue<RecycleStrategy> _strategies;

    public CacheRecycle(CacheDomain cacheDomain) {
        _cacheDomain = cacheDomain;
        _strategies = new ConcurrentLinkedQueue<RecycleStrategy>();
    }

    /**
     * 添加回收策略 回收时会按照添加策略的先后顺序依次回收，直到找到下一个回收对象
     */
    public void addRecycleStrategy(int recycleStrategy) {
        RecycleStrategy newStrategy = getRecycleStrategy(recycleStrategy);
        if (newStrategy != null && !_strategies.contains(newStrategy)) {
            _strategies.add(newStrategy);
        }
    }

    private RecycleStrategy getRecycleStrategy(int recycleStrategyFlag) {
        RecycleStrategy strategy = null;
        switch (recycleStrategyFlag) {
            case STRATEGY_EXPIRED:
                strategy = new ExpireStrategy(_cacheDomain);
                break;
            case STRATEGY_RECENTLY_UNUSED:
                strategy = new RecentlyUnusedStrategy();
                break;
            case STRATEGY_NO_RECYCLE:
                strategy = new NoRecycleStrategy();
                break;
            default:
                strategy = null;
                break;
        }
        return strategy;
    }

    public void removeRecycleStrategy(int recycleStrategy) {
        _strategies.remove(getRecycleStrategy(recycleStrategy));
    }

    public void clear() {
        _strategies.clear();
    }

    public boolean containsRecycleStrategy(int recycleStrategy) {
        return _strategies.contains(getRecycleStrategy(recycleStrategy));
    }

    public CacheModel recycle(ConcurrentMap<String, CacheModel> caches) {
        CacheModel cache = null;
        for (RecycleStrategy strategy : _strategies) {
            cache = strategy.findRecycledCache(caches.values());
            if (cache != null) {
                caches.remove(cache.getKey());
                break;
            }
        }

        return cache;
    }
}

