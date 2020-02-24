package com.github.xulcache.cachedomain;

import com.github.xulcache.CacheModel;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PropertyCacheDomain extends WriteBackCacheDomain {

    private final AtomicBoolean _isUpdated = new AtomicBoolean(false);
    private final ThreadPoolExecutor _threadPoolExecutor;
    private boolean isRuning = false;

    private Runnable saveDataRunnable = new Runnable(){
        @Override
        public void run() {
            if(isRuning){
                if (_isUpdated.getAndSet(false)) {
                    // 更新过数据，需要写入文件缓存
                    for (CacheModel cache : _memoryCache.getAllCaches()) {
                        if (null ==_fileCache.getCache(cache.getKey(), false)) {
                            _fileCache.putCache(cache);
                        }
                    }
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public PropertyCacheDomain(long maxSize, int maxCount,
                               File cacheDir, long maxFileSize, int maxFileCount) {
        super(maxSize, maxCount, cacheDir, maxFileSize, maxFileCount);

        isRuning = true;
        _threadPoolExecutor = new ThreadPoolExecutor(1,2,30 ,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        _threadPoolExecutor.execute(saveDataRunnable);
    }


    @Override
    public void close() {
        super.close();
        isRuning = false;
        _threadPoolExecutor.remove(saveDataRunnable);
    }

    @Override
    protected boolean putCache(CacheModel cacheData) {
        boolean isUpdated = super.putCache(cacheData);
        if (isUpdated) {
            _isUpdated.set(true);
        }
        return isUpdated;
    }

    @Override
    protected CacheModel removeCache(String md5Key) {
        CacheModel cache = super.removeCache(md5Key);
        if (cache != null) {
            _isUpdated.set(true);
        }
        return cache;
    }

    @Override
    protected CacheModel removeNextCache() {
        CacheModel cache = super.removeNextCache();
        if (cache != null) {
            _isUpdated.set(true);
        }
        return cache;
    }

    public boolean getIsUpdated() {
        return _isUpdated.get();
    }
}
