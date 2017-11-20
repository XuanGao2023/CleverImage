package com.pan.cleverimage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by pan on 18/11/2017.
 */

public class Composer {
    public void process() {
        ExecutorService getterExecutorService = Executors.newFixedThreadPool(5);
        ExecutorService loaderExecutorService = Executors.newFixedThreadPool(5);

        UrlGetter urlGetter = new UrlGetter(getterExecutorService);
        final BitmapSaver bitmapSaver = new BitmapSaver(loaderExecutorService);
        urlGetter.setNextTask(bitmapSaver);

        urlGetter.start();
    }
}
