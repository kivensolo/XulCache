package com.github.xulcache.cachedomain;

import com.github.xulcache.CacheDomain;
import com.github.xulcache.CacheModel;
import com.github.xulcache.cacheimplement.CacheImpl;
import com.github.xulcache.cacheimplement.FileCache;
import com.github.xulcache.cacheimplement.MemoryCache;

import java.io.File;
import java.util.Collection;

public class WriteThroughCacheDomain extends CacheDomain {

    protected final CacheImpl _memoryCache;
    protected final CacheImpl _fileCache;

    public WriteThroughCacheDomain(long maxSize, int maxCount,
                                   File cacheDir, long maxFileSize, int maxFileCount) {
        // 同时写入内存和文件，取最小的大小限制和数量限制
        long minSize = Math.min(maxSize, maxFileSize);
        int minCount = Math.min(maxCount, maxFileCount);

        _memoryCache = new MemoryCache(minSize, minCount);
        _fileCache = new FileCache(cacheDir, minSize, minCount);

        // 保证文件缓存和内存保存数据同步
        for (CacheModel cacheData : _fileCache.getAllCaches()) {
            _memoryCache.putCache(cacheData);
        }
    }

    @Override
    public void setDomainFlags(int domainFlags) {
        super.setDomainFlags(domainFlags);
        _memoryCache.setDomainFlags(domainFlags);
        _fileCache.setDomainFlags(domainFlags);
    }

    @Override
    protected boolean putCache(CacheModel cacheData) {
        boolean putResult = _memoryCache.putCache(cacheData);
        if (putResult) {
            putResult = _fileCache.putCache(cacheData);
        } else {
            // 内存缓存保存失败
            return false;
        }

        if (putResult) {
            return true;
        } else {
            // 文件缓存保存失败，同时删除内存中的缓存
            _memoryCache.removeCache(cacheData.getKey());
            return false;
        }
    }

    @Override
    protected CacheModel getCache(String key, boolean update) {
        return _memoryCache.getCache(key, update);
    }

    @Override
    protected CacheModel removeCache(String md5Key) {
        CacheModel memoryData = _memoryCache.removeCache(md5Key);
        if (memoryData != null) {
            _fileCache.removeCache(md5Key);
        }

        return memoryData;
    }

    @Override
    public void clear() {
        _memoryCache.clear();
        _fileCache.clear();
    }

    @Override
    public void close() {
        _memoryCache.clear();
    }

    @Override
    protected CacheModel removeNextCache() {
        CacheModel memoryData = _memoryCache.removeNextCache();
        if (memoryData != null) {
            _fileCache.removeCache(memoryData.getKey());
        }

        return memoryData;
    }

    @Override
    public long size() {
        return _memoryCache.size();
    }

    @Override
    public long sizeCapacity() {
        return _memoryCache.sizeCapacity();
    }

    @Override
    public int count() {
        return _memoryCache.count();
    }

    @Override
    public int countCapacity() {
        return _memoryCache.countCapacity();
    }

    @Override
    public Collection<CacheModel> getAllCaches() {
        return _memoryCache.getAllCaches();
    }
}
