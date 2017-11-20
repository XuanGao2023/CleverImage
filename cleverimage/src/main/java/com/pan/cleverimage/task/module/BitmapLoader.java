package com.pan.cleverimage.task.module;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.pan.cleverimage.task.base.Setting;
import com.pan.cleverimage.task.base.Task;

import java.util.concurrent.ExecutorService;

/**
 * Created by pan on 20/11/2017.
 */


public class BitmapLoader extends Task<String, Bitmap> {

    public BitmapLoader(ExecutorService executorService) {
        super(executorService);
    }

    @Override
    public Bitmap OnProcessing(String input, Setting setting) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(input, options);
    }
}
