package com.pan.cleverimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.pan.cleverimage.util.FileUtils;
import com.pan.cleverimage.util.ImageUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */

public class ImageGetter {
    private final static String TAG = "ImageGetter";
    private static final boolean DEBUG = true;

    public static final String DEFAULT_FOLDERNAME = "imagegetter";
    public static final int DEFAULT_MEM_LRUCACHE_SIZE = 50 * 1024 * 1024; //default memory cache size 20MB
    public static final int DEFAULT_DOWNLOAD_THREAD_POOL_SIZE = 5; //default thread pool size
    private static final long DEFAULT_MAX_IMAGE_COMPRESS_SIZE = 2 * 1024 * 1024;
    protected static String FILE_FOLDER;
    protected static ImageGetter instance;

    protected LruCache<String, Bitmap> bitmapLruCache;
    protected Handler handlerMainThread = new Handler();
    private Random randomValueCreator = new Random();

    protected HandlerThread handlerThreadDiskReader = new HandlerThread("image-disk-reader");
    protected Handler handlerDiskReader;
    protected HandlerThread handlerThreadDiskWriter = new HandlerThread("image-disk-writer");
    protected Handler handlerDiskWriter;
    protected ExecutorService fixedThreadPool;

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
        instance = new ImageGetter(foldername, bitmapcachesize, threadpoolsize);
        return instance;
    }

    protected ImageGetter() {
        this(null, null, null);
    }

    protected ImageGetter(String foldername, Integer cachesize, Integer poolsize) {
        initCacheFolder(foldername);
        initLRUCache(cachesize);
        initThreadPool(poolsize);
        handlerThreadDiskReader.start();
        handlerThreadDiskWriter.start();
        handlerDiskReader = new Handler(handlerThreadDiskReader.getLooper());
        handlerDiskWriter = new Handler(handlerThreadDiskWriter.getLooper());
    }

    protected void initCacheFolder(String foldername) {
        if (TextUtils.isEmpty(foldername)) {
            foldername = DEFAULT_FOLDERNAME;
        }
        FILE_FOLDER = Environment.getExternalStorageDirectory() + File.separator + foldername + File.separator;
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

    protected void initThreadPool(Integer poolsize) {
        if (poolsize == null || poolsize <= 0) {
            poolsize = DEFAULT_DOWNLOAD_THREAD_POOL_SIZE;
        }
        fixedThreadPool = Executors.newFixedThreadPool(poolsize);
    }

    public static Bitmap getPic(String url, ImageGotListener listener) {
        return getPic(url, false, listener);
    }

    public static Bitmap getPic(String url, boolean forceupdate, ImageGotListener listener) {
        ImageGetter ins = ImageGetter.getInstance();
        if (forceupdate) {
            if (DEBUG) {
                Log.d(TAG, "Loading Image from internet. url: " + url);
            }
            ins.loadImageFromInternet(url, listener);
            return null;
        } else {
            Bitmap bitmap = ins.bitmapLruCache.get(ins.getUrlKey(url));
            if (bitmap != null) {
                if (listener != null) {
                    listener.OnImageGot(bitmap);
                }
                if (DEBUG) {
                    Log.d(TAG, "Got Image from bitmapLruCache. url: " + url);
                }
                return bitmap;
            } else {
                if (ins.readImageFromDisk(url, listener)) {
                    if (DEBUG) {
                        Log.d(TAG, "Got Image from disk. url: " + url);
                    }
                    return null;
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "Loading Image from internet. url: " + url);
                    }
                    ins.loadImageFromInternet(url, listener);
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
            System.out.println("isDiskFileValid: " + filepath + " true");
            return true;
        } else {
            System.out.println("isDiskFileValid: " + filepath + " false");
            return false;
        }
    }

    protected boolean readImageFromDisk(final String url, final ImageGotListener listener) {
        boolean value = instance.isDiskFileValid(url);
        if (!value) {
            return false;
        }
        handlerDiskReader.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                String filepath = FILE_FOLDER + buildDiskFileName(url);
                bitmap = loadFromDisk(filepath);
                if (bitmap == null) {
                    FileUtils.deleteFile(filepath);
                    postCallbackOnMainThread(listener, null);
                    return;
                }
                bitmapLruCache.put(buildCacheKey(url), bitmap);
                postCallbackOnMainThread(listener, bitmap);
            }
        });
        return true;
    }

    protected void postCallbackOnMainThread(final ImageGotListener listener, final Bitmap bitmap) {
        if (listener != null) {
            handlerMainThread.post(new Runnable() {
                @Override
                public void run() {
                    listener.OnImageGot(bitmap);
                }
            });
        }
    }

    protected String buildDiskFileName(String url) {
        return getUrlKey(url);
    }

    protected void writeImageToDisk(final String url, final Bitmap bitmap) {
        handlerDiskWriter.post(new Runnable() {
            @Override
            public void run() {
                String finalfilename = buildDiskFileName(url);
                FileUtils.deleteFile(finalfilename);
                String tempfilename = finalfilename + System.currentTimeMillis() + randomValueCreator.nextInt();
                String tempfilepath = FILE_FOLDER + finalfilename;
                //in case the saving procedure interrupted by exception.
                boolean success = ImageUtils.save(bitmap, tempfilepath, Bitmap.CompressFormat.JPEG);
                if (!success) {
                    FileUtils.deleteFile(tempfilename);
                    return;
                }
                FileUtils.rename(tempfilepath, finalfilename);
            }
        });
    }

    protected void loadImageFromInternet(final String url, final ImageGotListener listener) {
        fixedThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                InputStream in;
                try {
                    in = new java.net.URL(url).openStream();
                } catch (Exception e) {
                    e.printStackTrace();
                    postCallbackOnMainThread(listener, null);
                    return;
                }
                bitmap = BitmapFactory.decodeStream(in);
                bitmap = compressImageFromInternet(url, bitmap);
                if (DEBUG) {
                    Log.d(TAG, "Got Image from internet. url: " + url);
                }
                bitmapLruCache.put(buildCacheKey(url), bitmap);
                postCallbackOnMainThread(listener, bitmap);
                writeImageToDisk(url, bitmap);
            }
        });
    }

    protected Bitmap compressImageFromInternet(String url, Bitmap bitmap) {
        return compress(bitmap, DEFAULT_MAX_IMAGE_COMPRESS_SIZE);
    }

    protected String getUrlKey(String url) {
        if (url == null) {
            throw new RuntimeException("Null url passed in");
        } else {
            return url.replaceAll("[.:/,%?&=]", "_").replaceAll("[_]+", "_");
        }
    }

    public static void clearMemCache() {
        ImageGetter.getInstance().bitmapLruCache.evictAll();
    }

    public static void clearDiskCache() {
        //// TODO: 2017/10/25 if the file under this directory is operating?
        FileUtils.deleteAllInDir(FILE_FOLDER);
    }

    public interface ImageGotListener {
        void OnImageGot(Bitmap bitmap);
    }

    // ImageView relative functions.
    public static void loadPic(ImageView imageview, String url) {
        loadPic(imageview, url, (Integer) null, false);
    }

    public static void loadPic(ImageView imageview, String url, Integer defaultres) {
        loadPic(imageview, url, defaultres, false);
    }

    public static void loadPic(final ImageView imageview, String url, final Integer defaultres, boolean forceupdate) {
        if (imageview == null || TextUtils.isEmpty(url)) {
            return;
        }
        final WeakReference<ImageView> weakref = new WeakReference<ImageView>(imageview);
        getPic(url, forceupdate, new ImageGetter.ImageGotListener() {
            @Override
            public void OnImageGot(Bitmap bitmap) {
                if (weakref.get() != null) {
                    ImageView imageview = weakref.get();
                    if (bitmap != null) {
                        imageview.setImageBitmap(bitmap);
                    } else {
                        if (defaultres != null && defaultres > 0) {
                            imageview.setImageResource(defaultres);
                        }
                    }
                }
            }
        });
    }

    public static void loadPic(ImageView imageview, String url, Bitmap defaultbitmap) {
        loadPic(imageview, url, defaultbitmap, false);
    }

    public static void loadPic(final ImageView imageview, String url, final Bitmap defaultbitmap, boolean forceupdate) {
        if (imageview == null) {
            return;
        }
        final WeakReference<ImageView> weakref = new WeakReference<ImageView>(imageview);
        getPic(url, forceupdate, new ImageGetter.ImageGotListener() {
            @Override
            public void OnImageGot(Bitmap bitmap) {
                if (weakref.get() != null) {
                    ImageView imageview = weakref.get();
                    if (bitmap != null) {
                        imageview.setImageBitmap(bitmap);
                    } else {
                        if (defaultbitmap != null) {
                            imageview.setImageBitmap(defaultbitmap);
                        }
                    }
                }
            }
        });
    }

    public static Bitmap resize(Bitmap bitmap, int newwidth, int newheight) {
        return Bitmap.createScaledBitmap(bitmap, newwidth, newheight, true);
    }

    public static Bitmap compress(Bitmap bitmmap, long maxsize) {
        return ImageUtils.compressByQuality(bitmmap, maxsize);
    }
}
