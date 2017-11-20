package com.pan.cleverimage.task.module;

import android.graphics.Bitmap;

import com.pan.cleverimage.task.base.Setting;
import com.pan.cleverimage.task.base.Task;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;

/**
 * Created by pan on 18/11/2017.
 */

public class UrlGetter extends Task<String, InputStream> {
    private static final String TAG = "UrlGetter";

    public UrlGetter(ExecutorService executorService) {
        super(executorService);
    }

    public UrlGetter(ExecutorService executor, String input) {
        super(executor, input);
    }

    @Override
    public InputStream OnProcessing(String input, Setting setting) {
        Bitmap bitmap = null;
        InputStream in;
        try {
            in = new java.net.URL(input).openStream();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return in;
    }
}
