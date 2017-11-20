package com.pan.cleverimage;

import android.graphics.Bitmap;
import android.os.Handler;

import com.pan.cleverimage.task.base.Setting;
import com.pan.cleverimage.task.base.Task;
import com.pan.cleverimage.task.module.BitmapDecoder;
import com.pan.cleverimage.task.module.BitmapSaver;
import com.pan.cleverimage.task.module.UrlGetter;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;

/**
 * Created by pan on 19/11/2017.
 */

public class ImageGetterBuilder {
    protected ExecutorService networkRequestExecutorService;
    protected ExecutorService imageDecodeExecutorService;
    protected ExecutorService saveToDiskExecutorService;
    public static final Handler handlerMainThread = new Handler();

    public ImageGetterBuilder(ExecutorService network, ExecutorService decoder, ExecutorService saveToDisk) {
        this.networkRequestExecutorService = network;
        this.imageDecodeExecutorService = decoder;
        this.saveToDiskExecutorService = saveToDisk;
    }

    public void getImage(String url, final Callback callback) {
        UrlGetter urlGetter = new UrlGetter(networkRequestExecutorService, url);
        BitmapDecoder bitmapDecoder = new BitmapDecoder(imageDecodeExecutorService);
        BitmapSaver bitmapSaver = new BitmapSaver(saveToDiskExecutorService);
        urlGetter.setNextTask(bitmapDecoder);
        bitmapDecoder.setNextTask(bitmapSaver);

        bitmapDecoder.setCallback(new Task.Callback<InputStream, Bitmap>() {
            @Override
            public void OnFinish(InputStream input, Bitmap output, Setting setting) {
                if (callback != null) {
                    callback.OnImageGot(output);
                }
            }
        });
        Setting setting = new Setting();
        setting.put("test", "123456");
        urlGetter.start(setting);
        bitmapSaver.setCallback(new Task.Callback<Bitmap, Boolean>() {
            @Override
            public void OnFinish(Bitmap input, Boolean output, Setting setting) {
                String key = (String) setting.get("test");
                System.out.println("setting.get(test): " + key);
            }
        });
    }

    public interface Callback {
        void OnImageGot(Bitmap bitmap);
    }
}
