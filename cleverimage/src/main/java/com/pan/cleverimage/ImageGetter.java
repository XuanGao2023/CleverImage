package com.pan.cleverimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.pan.cleverimage.util.FileUtils;
import com.pan.cleverimage.util.ImageUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
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
    public static final int DISKREADER_THREAD_POOL_SIZE = 2; //default disk reader thread pool size
    public static final int DISKWRITER_THREAD_POOL_SIZE = 2; //default disk writer thread pool size
    private static final long DEFAULT_MAX_IMAGE_COMPRESS_SIZE = 2 * 1024 * 1024;
    protected static String FILE_FOLDER;
    protected static ImageGetter instance;

    protected LruCache<String, Bitmap> bitmapLruCache;
    protected Handler handlerMainThread = new Handler();
    private Random randomValueCreator = new Random();

    protected ExecutorService diskReaderThreadPool;
    protected ExecutorService diskWriterThreadPool;
    protected ExecutorService netRequestThreadPool;

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
        netRequestThreadPool = Executors.newFixedThreadPool(poolsize);
        diskReaderThreadPool = Executors.newFixedThreadPool(DISKREADER_THREAD_POOL_SIZE);
        diskWriterThreadPool = Executors.newFixedThreadPool(DISKWRITER_THREAD_POOL_SIZE);
    }

    /**
     * one imageview only has one tag.
     *
     * @param imageview
     * @param listener
     * @param url
     * @param defaultbitmap
     * @param forceupdate
     * @return
     */
    protected Request buildRequest(ImageView imageview, ImageGotListener listener, String url
            , Bitmap defaultbitmap, Boolean forceupdate) {
        Request request = new Request(imageview, listener, url, defaultbitmap, forceupdate);
        return request;
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

    protected Bitmap compressImageFromInternet(String url, Bitmap bitmap) {
        return compress(bitmap, DEFAULT_MAX_IMAGE_COMPRESS_SIZE);
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
    public static Request loadPic(ImageView imageview, String url) {
        return loadPic(imageview, url, (Integer) null, false);
    }

    public static Request loadPic(ImageView imageview, String url, Integer defaultres) {
        return loadPic(imageview, url, defaultres, false);
    }

    public static Request loadPic(final ImageView imageview, String url, final Integer defaultres, boolean forceupdate) {
        Bitmap bitmap = null;
        if (defaultres != null) {
            Context context = imageview.getContext();
            bitmap = BitmapFactory.decodeResource(context.getResources(), defaultres);
        }
        return loadPic(imageview, null, url, bitmap, forceupdate);
    }

    public static Request loadPic(ImageView imageview, String url, Bitmap defaultbitmap) {
        return loadPic(imageview, url, defaultbitmap, false);
    }

    public static Request loadPic(final ImageView imageview, String url, final Bitmap defaultbitmap, boolean forceupdate) {
        return loadPic(imageview, null, url, defaultbitmap, forceupdate);
    }

    public static Request loadPic(ImageView imageview, ImageGotListener listener, String url, Bitmap defaultbitmap, boolean forceupdate) {
        Request request = null;
        if (imageview != null) {
            Object object = imageview.getTag();
            if (object != null && object instanceof Request) {
                //reuse the request.
                request = (Request) object;
                //ignore this request.
                request.endRequest();
            }
        }
        request = getInstance().buildRequest(imageview, listener, url, defaultbitmap, forceupdate);
        if (imageview != null) {
            imageview.setTag(request);
        }
        request.getPic();
        return request;
    }

    public static Bitmap resize(Bitmap bitmap, int newwidth, int newheight) {
        return Bitmap.createScaledBitmap(bitmap, newwidth, newheight, true);
    }

    public static Bitmap compress(Bitmap bitmmap, long maxsize) {
        return ImageUtils.compressByQuality(bitmmap, maxsize);
    }

    public static Bitmap loadFromDisk(String photopath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(photopath, options);
    }

    public class Request implements ImageGotListener {
        public static final int IMAGEVIEW_TAG = 99000;

        public WeakReference<ImageView> wrImageView;
        public ImageGotListener imageGotListener;
        public String strUrl;
        public Bitmap bitmapDefault;
        public Boolean bForceUpdate;
        public Boolean requestIsOver = false;

        /**
         * Cancel this request.
         */
        public void endRequest() {
            if (requestIsOver) {
                return;
            } else {
                requestIsOver = true;
                wrImageView.clear();
                imageGotListener = null;
                bitmapDefault = null;
            }
        }

        @Override
        public void OnImageGot(Bitmap bitmap) {
            if (!requestIsOver) {
                if (imageGotListener != null) {
                    imageGotListener.OnImageGot(bitmap);
                } else {
                    ImageView imageview = wrImageView.get();
                    if (imageview != null) {
                        if (bitmap != null) {
                            imageview.setImageBitmap(bitmap);
                        } else {
                            imageview.setImageBitmap(bitmapDefault);
                        }
                    }
                }
            }
            requestIsOver = true;
        }

        protected Bitmap getPic() {
            if (requestIsOver) {
                throw new IllegalStateException("Request can't be reused!");
            }
            if (bForceUpdate) {
                if (DEBUG) {
                    Log.d(TAG, "Loading Image from internet. url: " + strUrl);
                }
                loadImageFromInternet(strUrl, this);
                return null;
            } else {
                Bitmap bitmap = bitmapLruCache.get(getUrlKey(strUrl));
                if (bitmap != null) {
                    OnImageGot(bitmap);
                    if (DEBUG) {
                        Log.d(TAG, "Got Image from bitmapLruCache. url: " + strUrl);
                    }
                    return bitmap;
                } else {
                    if (readImageFromDisk(strUrl, this)) {
                        if (DEBUG) {
                            Log.d(TAG, "Got Image from disk. url: " + strUrl);
                        }
                        return null;
                    } else {
                        if (DEBUG) {
                            Log.d(TAG, "Loading Image from internet. url: " + strUrl);
                        }
                        loadImageFromInternet(strUrl, this);
                    }
                    return null;
                }
            }
        }

        protected String getUrlKey(String url) {
            if (url == null) {
                throw new RuntimeException("Null url passed in");
            } else {
                return url.replaceAll("[.:/,%?&=]", "_").replaceAll("[_]+", "_");
            }
        }

        public Request(ImageView imageview, ImageGotListener listener, String url, Bitmap defaultbitmap, Boolean forceupdate) {
            if (imageview == null && listener == null) {
                throw new IllegalArgumentException("imageview is null and listener is null!");
            }
            if (TextUtils.isEmpty(url)) {
                throw new IllegalArgumentException("url is null!");
            }
            this.wrImageView = new WeakReference<ImageView>(imageview);
            this.imageGotListener = listener;
            this.strUrl = url;
            this.bitmapDefault = defaultbitmap;
            this.bForceUpdate = forceupdate;
        }

        protected boolean readImageFromDisk(final String url, final ImageGotListener listener) {
            boolean value = isDiskFileValid(url);
            if (!value) {
                return false;
            }
            diskReaderThreadPool.submit(new Runnable() {
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

        protected void writeImageToDisk(final String url, final Bitmap bitmap) {
            diskWriterThreadPool.submit(new Runnable() {
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
            netRequestThreadPool.submit(new Runnable() {
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

        protected String buildCacheKey(String url) {
            return getUrlKey(url);
        }

        protected String buildDiskFileName(String url) {
            return getUrlKey(url);
        }

        protected boolean isDiskFileValid(String url) {
            String filepath = FILE_FOLDER + buildDiskFileName(url);
            File file = new File(filepath);
            if (file.exists()) {
                if (DEBUG) {
                    System.out.println("isDiskFileValid: true " + filepath);
                }
                return true;
            } else {
                if (DEBUG) {
                    System.out.println("isDiskFileValid: false " + filepath);
                }
                return false;
            }
        }
    }
}
