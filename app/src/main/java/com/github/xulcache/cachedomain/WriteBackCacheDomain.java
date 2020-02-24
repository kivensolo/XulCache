package com.github.xulcache.cachedomain;

import com.github.xulcache.CacheDomain;
import com.github.xulcache.CacheModel;
import com.github.xulcache.cacheimplement.CacheImpl;
import com.github.xulcache.cacheimplement.FileCache;
import com.github.xulcache.cacheimplement.MemoryCache;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

public class WriteBackCacheDomain extends CacheDomain {

    protected final CacheImpl _memoryCache;
    protected final CacheImpl _fileCache;

    public WriteBackCacheDomain(long maxSize, int maxCount,
                                File cacheDir, long maxFileSize, int maxFileCount) {
        _memoryCache = new MemoryCache(maxSize, maxCount);
        _fileCache = new FileCache(cacheDir, maxFileSize, maxFileCount);
    }

    @Override
    public void setDomainFlags(int domainFlags) {
        super.setDomainFlags(domainFlags);
        _memoryCache.setDomainFlags(domainFlags);
        _fileCache.setDomainFlags(domainFlags);
    }

    @Override
    protected boolean putCache(CacheModel cacheData) {
        _fileCache.removeCache(cacheData.getKey());//当有更新时，删除filecache的内容，使文件能更新为最新
        return _memoryCache.putCache(cacheData);
    }

    @Override
    protected CacheModel getCache(String key, boolean update) {
        CacheModel data = _memoryCache.getCache(key, update);
        if (data == null) {
            data = _fileCache.getCache(key, update);
        }
        return data;
    }

    @Override
    protected CacheModel removeCache(String md5Key) {
        CacheModel cacheModel = _memoryCache.removeCache(md5Key);
        CacheModel fileCacheModel = _fileCache.removeCache(md5Key);
        return cacheModel == null ? fileCacheModel : cacheModel;
    }

    @Override
    public void clear() {
        // 同时清除一级缓存和二级缓存
        _memoryCache.clear();
        _fileCache.clear();
    }

    @Override
    protected CacheModel removeNextCache() {
        CacheModel cacheModel = _memoryCache.removeNextCache();
        if (cacheModel == null) {
            // 一级缓存未命中，从二级缓存删除
            cacheModel = _fileCache.removeNextCache();
        } else {
            // 一级缓存命中，存入二级缓存
            _fileCache.putCache(cacheModel);
        }

        return cacheModel;
    }

    @Override
    public void close() {
        for (CacheModel cache : _memoryCache.getAllCaches()) {
            _fileCache.putCache(cache);
        }
        _memoryCache.clear();
    }

    @Override
    public long size() {
        return _memoryCache.size() + _fileCache.size();
    }

    @Override
    public long sizeCapacity() {
        return _memoryCache.sizeCapacity() + _fileCache.sizeCapacity();
    }

    @Override
    public int count() {
        return _memoryCache.count() + _fileCache.count();
    }

    @Override
    public int countCapacity() {
        int count = _memoryCache.countCapacity() + _fileCache.countCapacity();
        return count > 0 ? count : Integer.MAX_VALUE;
    }

    @Override
    public Collection<CacheModel> getAllCaches() {
        HashSet<CacheModel> caches = new HashSet<CacheModel>(count());
        caches.addAll(_memoryCache.getAllCaches());
        caches.addAll(_fileCache.getAllCaches());
        return caches;
    }
}
