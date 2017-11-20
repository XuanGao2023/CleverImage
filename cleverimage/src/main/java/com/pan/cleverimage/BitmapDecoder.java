package com.pan.cleverimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;

/**
 * Created by pan on 19/11/2017.
 */

public class BitmapDecoder extends Task<InputStream, Bitmap> {
    private static final String TAG = "UrlGetter";

    public BitmapDecoder(ExecutorService executorService) {
        super(executorService);
    }

    public BitmapDecoder(ExecutorService executor, InputStream input) {
        super(executor, input);
    }

    @Override
    Bitmap OnProcessing(InputStream input) {
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        return bitmap;
    }
}