package com.pan.testapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.LruCache;

import com.pan.testapp.util.FileUtils;
import com.pan.testapp.util.ImageUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Random;

/**
 *
 */

public class ImageGetter {
	private final static String TAG = "ImageGetter";
	private static final boolean DEBUG = true;

	public static final String FOLDER = "imagegetter";
	public static String FILE_FOLDER;

	protected static int BITMAPLRUCACHE_SIZE = 20 * 1024 * 1024; //default cache size 20MB
	protected LruCache<String, Bitmap> bitmapLruCache;
	protected static ImageGetter imageGetterInstance;
	protected Handler handlerMainThread = new Handler();

	protected HandlerThread handlerThreadDiskReader = new HandlerThread("image-disk-reader");
	protected Handler handlerDiskReader;

	protected HandlerThread handlerThreadDiskWriter = new HandlerThread("image-disk_writer");
	protected Handler handlerDiskWriter;

	public static synchronized ImageGetter getInstance() {
		if (imageGetterInstance == null) {
			initInstance(FOLDER, BITMAPLRUCACHE_SIZE);
		}
		return imageGetterInstance;
	}

	public static synchronized ImageGetter initInstance() {
		return initInstance(FOLDER, BITMAPLRUCACHE_SIZE);
	}

	public static synchronized ImageGetter initInstance(String foldername, Integer bitmapcachesize) {
		if (imageGetterInstance != null) {
			throw new IllegalStateException("ImageGetter has already been initialized!");
		}
		if (foldername == null || foldername.length() == 0 || bitmapcachesize == null || bitmapcachesize <= 0) {
			throw new IllegalArgumentException("Invalid argument! foldername: "
					+ foldername + " bitmapcachesize: " + bitmapcachesize);
		}
		FILE_FOLDER = Environment.getExternalStorageDirectory() + File.separator + foldername + File.separator;
		imageGetterInstance = new ImageGetter();
		imageGetterInstance.initLRUCache(bitmapcachesize);
		return imageGetterInstance;
	}

	protected ImageGetter() {
		handlerThreadDiskReader.start();
		handlerThreadDiskWriter.start();
		handlerDiskReader = new Handler(handlerThreadDiskReader.getLooper());
		handlerDiskWriter = new Handler(handlerThreadDiskWriter.getLooper());
	}

	protected void initLRUCache(Integer cachesize) {
		if (cachesize == null || cachesize <= 0) {
			cachesize = BITMAPLRUCACHE_SIZE;
		}
		bitmapLruCache = new LruCache<String, Bitmap>(cachesize) {
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount();
			}
		};
	}

	public static Bitmap getPic(String url, ImageGotListener listener) {
		return getPic(url, false, listener);
	}

	public static Bitmap getPic(String url, boolean forceupdate, ImageGotListener listener) {
		if (forceupdate) {
			imageGetterInstance.loadImageFromInternet(url, listener);
			return null;
		} else {
			Bitmap bitmap = imageGetterInstance.bitmapLruCache.get(imageGetterInstance.getUrlKey(url));
			if (bitmap != null) {
				if (listener != null) {
					listener.OnImageGot(bitmap);
				}
				if (DEBUG) {
					Log.d(TAG, "Got Image from bitmapLruCache.");
				}
				return bitmap;
			} else {
				if (imageGetterInstance.readImageFromDisk(url, listener)) {
					if (DEBUG) {
						Log.d(TAG, "Loading Image from disk.");
					}
					return null;
				} else {
					if (DEBUG) {
						Log.d(TAG, "Loading Image from internet.");
					}
					imageGetterInstance.loadImageFromInternet(url, listener);
				}
				return null;
			}
		}
	}

	protected String buildCacheKey(String url) {
		return getUrlKey(url);
	}

	public static Bitmap loadFromDisk(String photopath) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		return BitmapFactory.decodeFile(photopath, options);
	}

	protected boolean isDiskFileValid(String url) {
		String filepath = FILE_FOLDER + buildDiskFileName(url);
		File file = new File(filepath);
		if (file.exists()) {
			return true;
		} else {
			return false;
		}
	}

	protected boolean readImageFromDisk(String url, ImageGotListener listener) {
		boolean value = imageGetterInstance.isDiskFileValid(url);
		if (!value) {
			return false;
		}
		WeakReference<ImageGotListener> wrlistener = new WeakReference<ImageGotListener>(listener);
		handlerDiskReader.post(new Runnable() {
			@Override
			public void run() {
				Bitmap bitmap = null;
				String filepath = FILE_FOLDER + buildDiskFileName(url);
				bitmap = loadFromDisk(filepath);
				if (bitmap == null) {
					FileUtils.deleteFile(filepath);
					ImageGotListener reallistener = wrlistener.get();
					if (listener != null) {
						final Bitmap bitmapcallback = null;
						handlerMainThread.post(new Runnable() {
							@Override
							public void run() {
								listener.OnImageGot(bitmapcallback);
							}
						});
					}
					return;
				}
				bitmapLruCache.put(buildCacheKey(url), bitmap);
				ImageGotListener reallistener = wrlistener.get();
				if (listener != null) {
					final Bitmap bitmapcallback = bitmap;
					handlerMainThread.post(new Runnable() {
						@Override
						public void run() {
							listener.OnImageGot(bitmapcallback);
						}
					});
				}
			}
		});
		return true;
	}

	protected String buildDiskFileName(String url) {
		return getUrlKey(url);
	}

	private Random randomInCreator = new Random();

	protected void writeImageToDisk(String url, final Bitmap bitmap) {
		handlerDiskWriter.post(new Runnable() {
			@Override
			public void run() {
				String finalfilename = buildDiskFileName(url);
				FileUtils.deleteFile(finalfilename);
				String tempfilename = finalfilename + System.currentTimeMillis() + randomInCreator.nextInt();
				String tempfilepath = FILE_FOLDER + finalfilename;
				//in case the saving procedure interrupted by exception.
				ImageUtils.save(bitmap, tempfilepath, Bitmap.CompressFormat.PNG);
				FileUtils.rename(tempfilepath, finalfilename);
			}
		});
	}

	protected void loadImageFromInternet(String url, ImageGotListener listener) {
		WeakReference<ImageGotListener> wrlistener = new WeakReference<ImageGotListener>(listener);
		new Thread() {
			@Override
			public void run() {
				super.run();
				Bitmap bitmap = null;
				try {
					InputStream in = new java.net.URL(url).openStream();
					bitmap = BitmapFactory.decodeStream(in);
				} catch (Exception e) {
					e.printStackTrace();
				}
				bitmapLruCache.put(buildCacheKey(url), bitmap);
				writeImageToDisk(url, bitmap);
				ImageGotListener reallistener = wrlistener.get();
				if (listener != null) {
					final Bitmap bitmapcallback = bitmap;
					handlerMainThread.post(new Runnable() {
						@Override
						public void run() {
							listener.OnImageGot(bitmapcallback);
						}
					});
				}
				writeImageToDisk(url, bitmap);
			}
		}.start();
	}

	protected String getUrlKey(String url) {
		if (url == null) {
			throw new RuntimeException("Null url passed in");
		} else {
			return url.replaceAll("[.:/,%?&=]", "_").replaceAll("[_]+", "_");
		}
	}

	public void clearMemCache() {
		bitmapLruCache.evictAll();
	}

	public void clearDiskCache() {
		//// TODO: 2017/10/25 if the file under this directory is operating?
		FileUtils.deleteAllInDir(FILE_FOLDER);
	}

	interface ImageGotListener {
		void OnImageGot(Bitmap bitmap);
	}
}
