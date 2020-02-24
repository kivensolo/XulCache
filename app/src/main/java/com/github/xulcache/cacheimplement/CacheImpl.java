package com.github.xulcache.cacheimplement;

import com.github.xulcache.CacheDomain;
import com.github.xulcache.CacheModel;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public abstract class CacheImpl extends CacheDomain {

    /**
     * 缓存数据map集合
     */
    protected final ConcurrentMap<String, CacheModel> _caches =
            new ConcurrentHashMap<String, CacheModel>();

    protected final AtomicLong _cacheSize = new AtomicLong(0);
    protected final AtomicInteger _cacheCount = new AtomicInteger(0);
    protected final long _sizeLimit;
    protected final int _countLimit;

    public CacheImpl(long maxSize, int maxCount) {
        _sizeLimit = maxSize;
        _countLimit = maxCount;
    }

    @Override
    public boolean putCache(CacheModel cacheData) {
        if (!CacheModel.isValid(cacheData)) {
            return false;
        }

        long valueSize = cacheData.size();
        int valueCount = 1;
        CacheModel oldCache = _caches.get(cacheData.getKey());
        if (oldCache != null) {
            // 已经存在同样的key
            valueSize -= oldCache.size();
            valueCount = 0;
        }

        if (valueSize > _sizeLimit) {
            throw new RuntimeException("Data is too large to put in cache.");
        }

        while (_cacheSize.get() + valueSize > _sizeLimit) {
            if (removeNextCache() == null) {
                // 无法移除，存储失败
                return false;
            }
        }
        _cacheSize.addAndGet(valueSize);

        while (_cacheCount.get() + valueCount > _countLimit) {
            if (removeNextCache() == null) {
                return false;
            }
        }
        _cacheCount.addAndGet(valueCount);

        cacheData.updateLastAccessTime();
        _caches.put(cacheData.getKey(), cacheData);

        if (cacheData.getOwner() == null){
            cacheData.setOwner(this);
        }
        return true;
    }

    @Override
    public CacheModel getCache(String key, boolean update) {
        CacheModel data = _caches.get(key);
        if (data == null || isExpired(data)) {
            if (data != null) {
                removeCache(key);
            }
            return null;
        }

        if (update) {
            data.updateLastAccessTime();
        }
        return data;
    }

    @Override
    public CacheModel removeCache(String md5Key) {
        CacheModel data = _caches.remove(md5Key);
        if (data != null) {
            _cacheSize.addAndGet(-data.size());
            _cacheCount.addAndGet(-1);
        }

        return data;
    }

    @Override
    public void clear() {
        _caches.clear();
        _cacheSize.set(0);
        _cacheCount.set(0);
    }

    @Override
    public CacheModel removeNextCache() {
        if (_caches.isEmpty()) {
            return null;
        }

        CacheModel cache = _recycler.recycle(_caches);
        if (cache != null) {
            _cacheSize.addAndGet(-cache.size());
            _cacheCount.addAndGet(-1);
        }
        return cache;
    }

    @Override
    public Collection<CacheModel> getAllCaches() {
        return _caches.values();
    }

    public void setRecycleStrategy(int strategyFlags) {
        _recycler.clear();
        _recycler.addRecycleStrategy(strategyFlags);
    }

    @Override
    public long size() {
        return _cacheSize.get();
    }

    @Override
    public long sizeCapacity() {
        return _sizeLimit;
    }

    @Override
    public int count() {
        return _cacheCount.get();
    }

    @Override
    public int countCapacity() {
        return _countLimit;
    }
}
