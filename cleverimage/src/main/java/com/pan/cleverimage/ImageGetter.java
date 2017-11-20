package com.pan.cleverimage;

import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.pan.cleverimage.task.base.Setting;
import com.pan.cleverimage.task.base.Task;
import com.pan.cleverimage.task.module.BitmapDecoder;
import com.pan.cleverimage.task.module.BitmapLoader;
import com.pan.cleverimage.task.module.BitmapSaver;
import com.pan.cleverimage.task.module.UrlGetter;
import com.pan.cleverimage.util.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by pan on 19/11/2017.
 */

public class ImageGetter {
    private final static String TAG = "ImageGetter";
    private static final boolean DEBUG = true;

    private static ImageGetter instance;
    public static final String DEFAULT_FOLDERNAME = "imagegetter";
    public static final int DEFAULT_MEM_LRUCACHE_SIZE = 20 * 1024 * 1024; //default cache size 20MB
    public static final int DEFAULT_DOWNLOAD_THREAD_POOL_SIZE = 5; //default thread pool size
    protected static String FILE_CACHE_FOLDER;

    protected LruCache<String, Bitmap> bitmapLruCache;
    protected ExecutorService networkRequestExecutorService;
    protected ExecutorService imageDecodeExecutorService;
    protected ExecutorService saveToDiskExecutorService;
    protected ExecutorService loadFromDiskExecutorService;
    public static final Handler handlerMainThread = new Handler();

    private ImageGetter(ExecutorService network, ExecutorService decoder, ExecutorService saveToDisk, ExecutorService loadFromDisk) {
        this.networkRequestExecutorService = network;
        this.imageDecodeExecutorService = decoder;
        this.saveToDiskExecutorService = saveToDisk;
        this.loadFromDiskExecutorService = loadFromDisk;
    }

    public static synchronized ImageGetter getInstance() {
        if (instance == null) {
            return init();
        }
        return instance;
    }

    public static synchronized ImageGetter init() {
        return init(null);
    }

    public static synchronized ImageGetter init(ImageGetter imagegetter) {
        if (instance != null) {
            throw new IllegalStateException("ImageGetter has already been initialized!");
        }
        if (imagegetter == null) {
            return init(DEFAULT_FOLDERNAME, DEFAULT_MEM_LRUCACHE_SIZE, DEFAULT_DOWNLOAD_THREAD_POOL_SIZE);
        }
        instance = imagegetter;
        return instance;
    }

    public static synchronized ImageGetter init(String foldername, Integer bitmapcachesize, Integer threadpoolsize) {
        if (instance != null) {
            throw new IllegalStateException("ImageGetter has already been initialized!");
        }
        instance = new ImageGetter(foldername, bitmapcachesize);
        return instance;
    }

    protected ImageGetter(String foldername, Integer cachesize) {
        initCacheFolder(foldername);
        initLRUCache(cachesize);
        initThreadPool();
        FILE_CACHE_FOLDER = Environment.getExternalStorageDirectory() + File.separator + foldername + File.separator;
        File file = new File(FILE_CACHE_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    protected void initThreadPool() {
        this.networkRequestExecutorService = Executors.newFixedThreadPool(5);
        this.imageDecodeExecutorService = Executors.newFixedThreadPool(5);
        this.saveToDiskExecutorService = Executors.newFixedThreadPool(2);
        this.loadFromDiskExecutorService = Executors.newFixedThreadPool(2);
    }

    protected void initCacheFolder(String foldername) {
        if (TextUtils.isEmpty(foldername)) {
            foldername = DEFAULT_FOLDERNAME;
        }
        FILE_CACHE_FOLDER = Environment.getExternalStorageDirectory() + File.separator + foldername + File.separator;
    }

    protected void initLRUCache(Integer cachesize) {
        if (cachesize == null || cachesize <= 0) {
            cachesize = DEFAULT_MEM_LRUCACHE_SIZE;
        }
        bitmapLruCache = new LruCache<String, Bitmap>(cachesize) {
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    public static Bitmap getImage(String url, Callback callback) {
        return getImage(url, false, callback);
    }

    public static Bitmap getImage(String url, boolean forceupdate, final Callback callback) {
        ImageGetter ins = ImageGetter.getInstance();
        if (forceupdate) {
            if (DEBUG) {
                Log.d(TAG, "Loading Image from internet. url: " + url);
            }
            ins.loadImageFromInternet(url, callback);
            return null;
        } else {
            Setting setting = new Setting(FILE_CACHE_FOLDER);
            setting.putUrl(url);
            String key = setting.buildCacheKey();
            Bitmap bitmap = ins.bitmapLruCache.get(key);
            if (bitmap != null) {
                if (callback != null) {
                    callback.OnImageGot(bitmap);
                }
                if (DEBUG) {
                    Log.d(TAG, "Got Image from bitmapLruCache. url: " + url);
                }
                return bitmap;
            } else {
                if (ins.readImageFromDisk(url, callback)) {
                    if (DEBUG) {
                        Log.d(TAG, "Got Image from disk. url: " + url);
                    }
                    return null;
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "Loading Image from internet. url: " + url);
                    }
                    ins.loadImageFromInternet(url, callback);
                }
                return null;
            }
        }
    }

    protected void saveImageToDisk(String url, Bitmap bitmap) {
        BitmapSaver bitmapSaver = new BitmapSaver(saveToDiskExecutorService, FILE_CACHE_FOLDER);
        Setting setting = new Setting(FILE_CACHE_FOLDER);
        setting.putUrl(url);
        bitmapSaver.setInput(bitmap);
        bitmapSaver.start(setting);
    }

    protected void loadImageFromInternet(final String url, final Callback callback) {
        Setting setting = new Setting(FILE_CACHE_FOLDER);
        setting.putUrl(url);
        UrlGetter urlGetter = new UrlGetter(instance.networkRequestExecutorService, url);
        BitmapDecoder bitmapDecoder = new BitmapDecoder(instance.imageDecodeExecutorService);
        urlGetter.setNextTask(bitmapDecoder);
        bitmapDecoder.setCallback(new Task.Callback<InputStream, Bitmap>() {
            @Override
            public void OnFinish(InputStream input, Bitmap output, Setting setting) {
                if (callback != null) {
                    callback.OnImageGot(output);
                }
                instance.bitmapLruCache.put(setting.buildCacheKey(), output);
                saveImageToDisk(url, output);
            }
        });
        urlGetter.start(setting);
    }

    protected boolean readImageFromDisk(String url, final Callback callback) {
        Setting setting = new Setting(FILE_CACHE_FOLDER);
        setting.putUrl(url);
        if (!setting.isDiskFileValid()) {
            return false;
        }
        BitmapLoader bitmapLoader = new BitmapLoader(loadFromDiskExecutorService);
        bitmapLoader.setSetting(setting);
        bitmapLoader.setCallback(new Task.Callback<String, Bitmap>() {
            @Override
            public void OnFinish(String input, Bitmap output, Setting setting) {
                String file = setting.buildCacheKey();
                instance.bitmapLruCache.put(file, output);
                if(callback != null) {
                    callback.OnImageGot(output);
                }
            }
        });
        bitmapLoader.start(setting.buildDiskFileDir());
        return true;
    }


    public static void clearMemCache() {
        ImageGetter.getInstance().bitmapLruCache.evictAll();
    }

    public static void clearDiskCache() {
        //// TODO: 2017/10/25 if the file under this directory is operating?
        FileUtils.deleteAllInDir(FILE_CACHE_FOLDER);
    }

    public interface Callback {
        void OnImageGot(Bitmap bitmap);
    }
}
