package com.pan.cleverimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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
    InputStream OnProcessing(String input) {
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
