package com.pan.cleverimage.task.module;

import android.graphics.Bitmap;
import android.os.Environment;

import com.pan.cleverimage.task.base.Setting;
import com.pan.cleverimage.task.base.Task;
import com.pan.cleverimage.util.FileUtils;
import com.pan.cleverimage.util.ImageUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * Created by pan on 18/11/2017.
 */

public class BitmapSaver extends Task<Bitmap, Boolean> {
    private static final String TAG = "BitmapSaver";

    private static String FILE_FOLDER = Environment.getExternalStorageDirectory() + File.separator;

    public BitmapSaver(ExecutorService executor, Bitmap input) {
        super(executor, input);
    }

    public BitmapSaver(ExecutorService executorService) {
        super(executorService);
    }

    @Override
    public Boolean OnProcessing(Bitmap input, Setting setting) {
        String filename = FILE_FOLDER + "test.png";
        //todo add extra information.
        //in case the saving procedure interrupted by exception.
        boolean success = ImageUtils.save(input, filename, Bitmap.CompressFormat.PNG);
        if (!success) {
            FileUtils.deleteFile(filename);
            return false;
        }
        return true;
    }
}
