package com.github.xulcache;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.github.xulcache.cachedomain.FileCacheDomain;
import com.github.xulcache.cachedomain.MemoryCacheDomain;
import com.github.xulcache.cachedomain.PropertyCacheDomain;
import com.github.xulcache.cachedomain.WriteBackCacheDomain;
import com.github.xulcache.cachedomain.WriteThroughCacheDomain;
import com.github.xulcache.utils.SystemUtil;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CacheCenter {

    private static final String TAG = CacheCenter.class.getSimpleName();

    // =======================================
    // ============ Cache Flags ==============
    // =======================================
    /**
     * Cache is valid until revision(versionCode) changed
     */
    public static final int CACHE_FLAG_REVISION_LOCAL = 0x00000;
    /**
     * Cache is valid until app's version type changed
     */
    public static final int CACHE_FLAG_VERSION_LOCAL = 0x00001;
    /**
     * Cache will always valid
     */
    public static final int CACHE_FLAG_GLOBAL = 0x00002;

    /**
     * Cache will never be recycled automatically
     */
    public static final int CACHE_FLAG_PERSISTENT = 0x00010;
    /**
     * Cache domain optimized for property storing
     */
    public static final int CACHE_FLAG_PROPERTY = 0x10000;

    /**
     * Cache in memory only
     */
    public static final int CACHE_FLAG_MEMORY = 0x20000;
    /**
     * Cache in file only
     */
    public static final int CACHE_FLAG_FILE = 0x40000;
    /**
     * Cache in memory and file
     */
    public static final int CACHE_FLAG_WRITE_BACK = CACHE_FLAG_MEMORY | CACHE_FLAG_FILE;
    /**
     * Cache in memory and file at the same time
     */
    public static final int CACHE_FLAG_WRITE_THROUGH = 0x80000 | CACHE_FLAG_WRITE_BACK;

    /**
     * 默认缓存大小
     */
    public static final long DEFAULT_MAX_MEMORY_SIZE = 1024 * 1024 * 32; // 32 mb
    public static final int DEFAULT_MAX_MEMORY_COUNT = Integer.MAX_VALUE; // 默认不使用
    public static final long DEFAULT_MAX_FILE_SIZE = 1024 * 1024 * 128; // 128 mb
    public static final int DEFAULT_MAX_FILE_COUNT = Integer.MAX_VALUE; // 默认不使用

    private static final ConcurrentMap<Integer, CacheDomain> _cacheDomains =
            new ConcurrentHashMap<Integer, CacheDomain>();

    private static int _revision = 0;
    private static String _version = "all";

    /**
     * 获取指定的cache domain
     *
     * @param domainId cache domain的标识id
     * @return 若存在，返回对应的cache domain，否则返回null
     */
    public static CacheDomain getCacheDomain(int domainId) {
        return _cacheDomains.get(domainId);
    }

    /**
     * 关闭cache center，一般在程序结束时调用。
     */
    public static void close() {
        for (CacheDomain domain : _cacheDomains.values()) {
            domain.close();
        }
    }

    /**
     * 清理缓存中心的所有cache数据，具有PERSISTENT标志的缓存域除外。
     */
    public static void clear() {
        if (_cacheDomains.values() != null && _cacheDomains.size() != 0) {
            for (CacheDomain domain : _cacheDomains.values()) {
                // 具有PERSISTENT标志的缓存域不需要清空
                if ((domain.getDomainFlags() & 0xF0) != CacheCenter.CACHE_FLAG_PERSISTENT) {
                    domain.clear();
                }
            }
        }
    }

    /**
     * 创建cache domain
     *
     * @param domainId cache domain标识id
     * @return 若创建成功或已存在相同domain则返回domain， 否则返回空（比如id相同但flag不同或无法创建缓存目录等原因造成）
     */
    public static CacheDomainBuilder buildCacheDomain(int domainId,Context context) {
        return CacheDomainBuilder.obtainBuilder(context, domainId);
    }

    public static int getRevision() {
        return _revision;
    }

    public static void setRevision(int revision) {
        _revision = revision;
    }

    public static String getVersion() {
        return _version;
    }

    public static void setVersion(String version) {
        _version = version;
    }

    public static final class CacheDomainBuilder {

        public static final int DEFAULT_DOMAIN_LIFE_TIME = 0;   // 默认为长期有效
        public static final int DEFAULT_DOMAIN_FLAGS = CACHE_FLAG_WRITE_BACK;

        public static final String PREFIX_REVISION = "revision-";
        public static final String PREFIX_VERSION = "version-";
        public static final String PREFIX_GLOBAL = "global";

        /**
         * DomainId-DomainFlags-DomainLifeTime
         */
        private static final String CACHE_DOMAIN_DIR_FORMAT = "%d-%d-%d";

        private int _domainId;
        private int _domainFlags;
        private Context _context;
        private long _maxMemorySize;
        private int _maxMemoryCount;
        private long _maxFileSize;
        private int _maxFileCount;
        private long _lifeTime;

        private CacheDomainBuilder(Context context, int domainId) {
            _context = context;
            initBuilder(domainId);
        }

        public static CacheDomainBuilder obtainBuilder(Context context, int domainId) {
            return new CacheDomainBuilder(context, domainId);
        }

        public CacheDomainBuilder initBuilder(int domainId) {
            _domainId = domainId;
            _domainFlags = DEFAULT_DOMAIN_FLAGS;
            _lifeTime = DEFAULT_DOMAIN_LIFE_TIME;
            _maxMemorySize = DEFAULT_MAX_MEMORY_SIZE;
            _maxMemoryCount = DEFAULT_MAX_MEMORY_COUNT;
            _maxFileSize = DEFAULT_MAX_FILE_SIZE;
            _maxFileCount = DEFAULT_MAX_FILE_COUNT;
            return this;
        }

        public CacheDomainBuilder setDomainFlags(int domainFlag) {
            _domainFlags = domainFlag;
            return this;
        }

        public CacheDomainBuilder setLifeTime(long ms) {
            _lifeTime = ms;
            return this;
        }

        public CacheDomainBuilder setMaxMemorySize(long maxSize) {
            _maxMemorySize = maxSize;
            return this;
        }

        public CacheDomainBuilder setMaxMemoryCount(int maxCount) {
            _maxMemoryCount = maxCount;
            return this;
        }

        public CacheDomainBuilder setMaxFileSize(long maxFileSize) {
            _maxFileSize = maxFileSize;
            return this;
        }

        public CacheDomainBuilder setMaxFileCount(int maxFileCount) {
            _maxFileCount = maxFileCount;
            return this;
        }

        public synchronized CacheDomain build() {
            CacheDomain cachedDomain = _cacheDomains.get(_domainId);
            if (cachedDomain != null) {
                if (_domainFlags == cachedDomain.getDomainFlags()
                    && _lifeTime == cachedDomain.getLifeTime()) {
                    // 同一个domain，直接返回已存在的实例
                    return cachedDomain;
                } else {
                    // 不允许创建id相同但flag或者lifetime不同的cache domain
                    return null;
                }
            }

            File cacheDir = getCacheDir();
            if (cacheDir == null) {
                Log.e(TAG, "Cannot get cache directory.");
                return null;
            }

            CacheDomain domain;
            switch (_domainFlags & ~0xFF) {
                case CACHE_FLAG_PROPERTY:
                    domain = new PropertyCacheDomain(
                            _maxMemorySize, _maxMemoryCount, cacheDir, _maxFileSize, _maxFileCount);
                    break;
                case CACHE_FLAG_MEMORY:
                    domain = new MemoryCacheDomain(_maxMemorySize, _maxMemoryCount);
                    break;
                case CACHE_FLAG_FILE:
                    domain = new FileCacheDomain(cacheDir, _maxFileSize, _maxFileCount);
                    break;
                case CACHE_FLAG_WRITE_BACK:
                    domain = new WriteBackCacheDomain(
                            _maxMemorySize, _maxMemoryCount, cacheDir, _maxFileSize, _maxFileCount);
                    break;
                case CACHE_FLAG_WRITE_THROUGH:
                    domain = new WriteThroughCacheDomain(
                            _maxMemorySize, _maxMemoryCount, cacheDir, _maxFileSize, _maxFileCount);
                    break;
                default:
                    domain = new MemoryCacheDomain(_maxMemorySize, _maxMemoryCount);
                    break;
            }

            domain.setDomainId(_domainId);
            domain.setDomainFlags(_domainFlags);
            domain.setLifeTime(_lifeTime);
            _cacheDomains.put(_domainId, domain);
            return domain;
        }

        private File getCacheDir() {
            String rootCacheDir = SystemUtil.getDiskCacheDir(_context);
            if (TextUtils.isEmpty(rootCacheDir)) {
                Log.e(TAG, "Cannot get root cache directory.");
                return null;
            }

            File cacheDir;
            String cacheDirName;
            switch (_domainFlags & 0xF) {
                case CACHE_FLAG_REVISION_LOCAL:
                    cacheDirName = PREFIX_REVISION + getRevision();
                    cacheDir = new File(rootCacheDir, cacheDirName);
                    if (!cacheDir.exists()) {
                        // 目录不存在，可能存在旧的缓存，清除无效缓存
                        clearInvalidCache(rootCacheDir, PREFIX_REVISION, cacheDirName);
                    }
                    break;
                case CACHE_FLAG_VERSION_LOCAL:
                    cacheDirName = PREFIX_VERSION + getVersion();
                    cacheDir = new File(rootCacheDir, cacheDirName);
                    if (!cacheDir.exists()) {
                        clearInvalidCache(rootCacheDir, PREFIX_VERSION, cacheDirName);
                    }
                    break;
                case CACHE_FLAG_GLOBAL:
                    cacheDir = new File(rootCacheDir, PREFIX_GLOBAL);
                    break;
                default:
                    cacheDir = new File(rootCacheDir, PREFIX_GLOBAL);
                    break;
            }

            String domainName = String.format(CACHE_DOMAIN_DIR_FORMAT,
                                              _domainId, _domainFlags, _lifeTime);

            if (cacheDir.exists()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        if (fileName.startsWith(_domainId + "-") && !fileName.equals(domainName)) {
                            // 不允许创建id相同但flag或者lifetime不同的cache domain
                            return null;
                        }
                    }
                }
            }

            cacheDir = new File(cacheDir, domainName);
            return cacheDir;
        }

        private void clearInvalidCache(final String parent, final String prefix, final  String curFile) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File root = new File(parent);
                    File[] files;
                    if (root == null || (files = root.listFiles()) == null) {
                        return;
                    }
                    for (File file : files) {
                        if (file.getName().equals(curFile)) { //不删除当前缓存文件
                            continue;
                        }
                        if (file.getName().startsWith(prefix)) {
                            SystemUtil.deleteDir(file);
                        }
                    }
                }
            }).start();
        }
    }
}
