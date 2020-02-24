package com.github.xulcache.cacheimplement;


import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.github.xulcache.CacheModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class MemoryCache extends CacheImpl {

	public MemoryCache(long maxSize, int maxCount) {
		super(maxSize, maxCount);
	}

	@Override
	public InputStream getAsStream(CacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof InputStream) {
			return (InputStream) data;
		}
		if (data instanceof byte[]) {
			return new ByteArrayInputStream(getAsBinary(cacheModel));
		}
		return null;
	}

	@Override
	public String getAsString(CacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof String) {
			return (String) data;
		}
		return String.valueOf(data);
	}

	@Override
	public JSONObject getAsJSONObject(CacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof JSONObject) {
			return (JSONObject) data;
		}
		return null;
	}

	@Override
	public JSONArray getAsJSONArray(CacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof JSONArray) {
			return (JSONArray) data;
		}
		return null;
	}

	@Override
	public byte[] getAsBinary(CacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof byte[]) {
			return (byte[]) data;
		}
		return null;
	}

	@Override
	public Object getAsObject(CacheModel cacheModel) {
		return cacheModel.getData();
	}

	@Override
	public Bitmap getAsBitmap(CacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof Bitmap) {
			return (Bitmap) data;
		}
		return null;
	}

	@Override
	public Drawable getAsDrawable(CacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof Drawable) {
			return (Drawable) data;
		}
		return null;
	}

	@Override
	public void close() {
		clear();
	}

	@Override
	public boolean putCache(CacheModel cacheData) {
		return false;
	}
}
