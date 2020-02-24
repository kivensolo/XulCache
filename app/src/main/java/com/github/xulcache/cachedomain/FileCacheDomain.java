package com.github.xulcache.cachedomain;

import com.github.xulcache.cacheimplement.FileCache;

import java.io.File;

public class FileCacheDomain extends FileCache {

    public FileCacheDomain(File cacheDir, long maxSize, int maxCount) {
        super(cacheDir, maxSize, maxCount);
    }
}
