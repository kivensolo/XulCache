package com.github.xulcache.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.WeakHashMap;

public class Md5Utils {
    public static String calMD5(String imageKey) {
        String localCacheKey = calMD5(imageKey.getBytes());
        if (localCacheKey == null) {
            return imageKey;
        }
        return localCacheKey;
    }

    private static char hexCharMap[] = new char[]{
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F',
    };

    private static class MessageDigestCtx {
        MessageDigest digest;
        char[] digestStr = new char[32];

        public MessageDigestCtx(MessageDigest digest) {
            this.digest = digest;
        }

        public void reset() {
            digest.reset();
        }

        public char[] digest(byte[] data) {
            byte[] digestVal = digest.digest(data);
            for (int i = 0; i < 16; ++i) {
                int b = digestVal[i] & 0xFF;
                digestStr[i * 2 + 0] = hexCharMap[b / 16];
                digestStr[i * 2 + 1] = hexCharMap[b % 16];
            }
            return digestStr;
        }
    }
    private static final WeakHashMap<Thread, MessageDigestCtx> _threadHashMap = new WeakHashMap<Thread, MessageDigestCtx>();

    private static MessageDigestCtx getMD5() {
        synchronized (_threadHashMap) {
            Thread thread = Thread.currentThread();
            MessageDigestCtx messageDigest = _threadHashMap.get(thread);
            if (messageDigest == null) {
                try {
                    MessageDigest md5 = MessageDigest.getInstance("md5");
                    MessageDigestCtx digestCtx = new MessageDigestCtx(md5);
                    _threadHashMap.put(thread, digestCtx);
                    return digestCtx;
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            messageDigest.reset();
            return messageDigest;
        }
    }

    public static String calMD5(byte[] data) {
        MessageDigestCtx md5 = getMD5();
        if (md5 != null) {
            return String.valueOf(md5.digest(data));
        } else {
            return null;
        }
    }
}
