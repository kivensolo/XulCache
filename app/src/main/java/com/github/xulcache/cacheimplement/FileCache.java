package com.github.xulcache.cacheimplement;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.github.xulcache.CacheModel;
import com.github.xulcache.stream.XulMemoryOutputStream;
import com.github.xulcache.utils.SystemUtil;
import com.github.xulcache.utils.XulBitmapUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

public class FileCache extends CacheImpl {

	private static final String TEMP_FILE_PREFIX = "temp_";

	protected File _cacheDir;

	public FileCache(File cacheDir, long maxSize, int maxCount) {
		super(maxSize, maxCount);
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			Log.e(TAG,"Can't make dirs in " + cacheDir.getAbsolutePath());
			return;
		}
		_cacheDir = cacheDir;
		calculateCacheSizeAndCacheCount();
	}

	/**
	 * 计算缓存 cacheSize和cacheCount
	 */
	private void calculateCacheSizeAndCacheCount() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
		long size = 0;
		int count = 0;
		File[] cachedFiles = _cacheDir.listFiles();
		if (cachedFiles != null) {
			for (File cachedFile : cachedFiles) {
				CacheModel cacheModel = new CacheModel();

				String fileName = cachedFile.getName();
				if (fileName.startsWith(TEMP_FILE_PREFIX)) {
					// 临时文件，脏数据，删除
					cachedFile.delete();
					continue;
				}

				cacheModel.setKey(fileName);
				cacheModel.setData(cachedFile);
				cacheModel.setLastAccessTime(cachedFile.lastModified());
				if (isExpired(cacheModel)) {
					cachedFile.delete();
					continue;
				}

				_caches.put(cacheModel.getKey(), cacheModel);
				cacheModel.setOwner(this);
				size += cachedFile.length();
				count++;
			}
			_cacheSize.set(size);
			_cacheCount.set(count);
		}
//            }
//        }).start();
	}

	@Override
	public String getAsString(CacheModel cacheModel) {
		File file = (File) cacheModel.getData();
		if (file == null) {
			return null;
		}

		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String readString = "";
			String currentLine;
			while ((currentLine = in.readLine()) != null) {
				readString += currentLine;
			}
			return readString;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public CacheModel getCache(String key, boolean update) {
		CacheModel cacheModel = _caches.get(key);
		if (cacheModel == null || isExpired(cacheModel)) {
			if (cacheModel != null) {
				removeCache(key);
				((File) cacheModel.getData()).delete();
			}
			return null;
		}

		File file = (File) cacheModel.getData();
		if (!file.exists()) {
			return null;
		}

		if (update) {
			cacheModel.updateLastAccessTime();
			file.setLastModified(cacheModel.getLastAccessTime());
		}
		return cacheModel;
	}

	@Override
	public boolean putCache(CacheModel data) {
		if (_cacheDir == null || !_cacheDir.exists() && !_cacheDir.mkdirs()) {
			Log.e(TAG, "Cache directory is null and cannot create.");
			return false;
		}

		CacheModel newModel = new CacheModel(data);
		if (!CacheModel.isValid(newModel)) {
			return false;
		}

		// 准备临时写入文件
		long putTime = System.currentTimeMillis();
		File tempFile = new File(
			_cacheDir, TEMP_FILE_PREFIX + putTime + Thread.currentThread().getId());

		// 写入缓存数据
		boolean saveSuccessful = false;
		Object cacheData = newModel.getData();
		if (cacheData instanceof String) {
			saveSuccessful = writeToFile(tempFile, (String) cacheData);
		} else if (cacheData instanceof byte[]) {
			saveSuccessful = writeToFile(tempFile, (byte[]) cacheData);
		} else if (cacheData instanceof InputStream) {
			saveSuccessful = writeToFile(tempFile, (InputStream) cacheData);
		} else {
			tempFile.deleteOnExit();
			return dispatched(newModel);
		}

		if (saveSuccessful) {
			// 文件写入成功，保存缓存文件
			saveFileToCache(tempFile, putTime, newModel);
			saveSuccessful = super.putCache(newModel);
		}
		return saveSuccessful;
	}

	private boolean dispatched(CacheModel cache) {
		boolean isDispatched = false;
		Object data = cache.getData();
		if (data instanceof Bitmap) {
			_put(cache.getKey(), (Bitmap) data);
			isDispatched = true;
		} else if (data instanceof Drawable) {
			_put(cache.getKey(), (Drawable) data);
			isDispatched = true;
		} else if (data instanceof JSONObject) {
			_put(cache.getKey(), (JSONObject) data);
			isDispatched = true;
		} else if (data instanceof JSONArray) {
			_put(cache.getKey(), (JSONArray) data);
			isDispatched = true;
		} else if ((data instanceof Serializable)) {
			_put(cache.getKey(), (Serializable) data);
			isDispatched = true;
		}

		return isDispatched;
	}

	private void saveFileToCache(File tempFile, long putTime, CacheModel data) {
		File file = new File(_cacheDir, data.getKey());
		tempFile.renameTo(file);
		file.setLastModified(putTime);
		data.setLastAccessTime(putTime);
		data.setData(file);
	}

	private boolean writeToFile(File tempFile, InputStream cacheData) {
		boolean saveSuccessful = false;
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(tempFile);
			byte[] tmpBuf = new byte[4096];
			int len;
			while ((len = cacheData.read(tmpBuf)) > 0) {
				out.write(tmpBuf, 0, len);
			}
			saveSuccessful = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return saveSuccessful;
	}

	private boolean writeToFile(File tempFile, byte[] cacheData) {
		boolean saveSuccessful = false;
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(tempFile);
			out.write(cacheData);
			saveSuccessful = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return saveSuccessful;
	}

	private boolean writeToFile(File tempFile, String cacheData) {
		boolean saveSuccessful = false;
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(tempFile), 1024);
			out.write(cacheData);
			saveSuccessful = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return saveSuccessful;
	}

	@Override
	public byte[] getAsBinary(CacheModel cacheModel) {
		File file = (File) cacheModel.getData();
		if (file == null) {
			return null;
		}

		RandomAccessFile RAFile = null;
		try {
			RAFile = new RandomAccessFile(file, "r");
			byte[] byteArray = new byte[(int) RAFile.length()];
			RAFile.read(byteArray);
			return byteArray;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (RAFile != null) {
				try {
					RAFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public InputStream getAsStream(CacheModel cacheModel) {
		File file = (File) cacheModel.getData();
		if (file == null) {
			return null;
		}

		RandomAccessFile RAFile = null;
		try {
			RAFile = new RandomAccessFile(file, "r");

			final int length = (int) RAFile.length();
			XulMemoryOutputStream memoryOps = new XulMemoryOutputStream();
			memoryOps.reset(length);
			byte[] dataBuffer = memoryOps.getDataBuffer();
			RAFile.read(dataBuffer, 0, length);
			memoryOps.setDataSize(length);
			return memoryOps.toInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (RAFile != null) {
				try {
					RAFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void _put(String key, JSONObject value) {
		CacheModel cacheModel = new CacheModel();
		cacheModel.setKey(key);
		cacheModel.setData(value.toString());
		putCache(cacheModel);
	}

	@Override
	public JSONObject getAsJSONObject(CacheModel cacheModel) {
		String JSONString = getAsString(cacheModel);
		try {
			JSONObject obj = new JSONObject(JSONString);
			return obj;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void _put(String key, JSONArray value) {
		CacheModel cacheModel = new CacheModel();
		cacheModel.setKey(key);
		cacheModel.setData(value.toString());
		putCache(cacheModel);
	}

	@Override
	public JSONArray getAsJSONArray(CacheModel cacheModel) {
		String JSONString = getAsString(cacheModel);
		try {
			JSONArray obj = new JSONArray(JSONString);
			return obj;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void _put(String key, Serializable value) {
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(value);
			byte[] data = baos.toByteArray();

			CacheModel cacheModel = new CacheModel();
			cacheModel.setKey(key);
			cacheModel.setData(data);
			putCache(cacheModel);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				oos.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public Object getAsObject(CacheModel cacheModel) {
		byte[] data = getAsBinary(cacheModel);
		if (data != null) {
			ByteArrayInputStream bais = null;
			ObjectInputStream ois = null;
			try {
				bais = new ByteArrayInputStream(data);
				ois = new ObjectInputStream(bais);
				Object reObject = ois.readObject();
				return reObject;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} finally {
				try {
					if (bais != null) {
						bais.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					if (ois != null) {
						ois.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;

	}

	private void _put(String key, Bitmap value) {
		CacheModel cacheModel = new CacheModel();
		cacheModel.setKey(key);
		cacheModel.setData(XulBitmapUtil.bitmap2Bytes(value));
		putCache(cacheModel);
	}

	@Override
	public Bitmap getAsBitmap(CacheModel cacheModel) {
		if (getAsBinary(cacheModel) == null) {
			return null;
		}
		return XulBitmapUtil.bytes2Bitmap(getAsBinary(cacheModel));
	}

	private void _put(String key, Drawable value) {
		put(key, XulBitmapUtil.drawable2Bitmap(value));
	}

	@Override
	public Drawable getAsDrawable(CacheModel cacheModel) {
		if (getAsBinary(cacheModel) == null) {
			return null;
		}
		return XulBitmapUtil.bitmap2Drawable(XulBitmapUtil.bytes2Bitmap(getAsBinary(cacheModel)));
	}

	@Override
	public CacheModel removeCache(String md5Key) {
		CacheModel cacheModel = super.removeCache(md5Key);
		removeCacheFile(cacheModel);
		return cacheModel;
	}

	@Override
	public CacheModel removeNextCache() {
		CacheModel cacheModel = super.removeNextCache();
		removeCacheFile(cacheModel);
		return cacheModel;
	}

	private void removeCacheFile(CacheModel cacheModel) {
		if (cacheModel == null) {
			return;
		}

		File file = (File) cacheModel.getData();
		if (file.exists()) {
			file.delete();
		}
	}

	@Override
	public void clear() {
		super.clear();
		if (_cacheDir != null) {
			SystemUtil.deleteDir(_cacheDir);
			_cacheDir.mkdirs();
		}
	}

}
