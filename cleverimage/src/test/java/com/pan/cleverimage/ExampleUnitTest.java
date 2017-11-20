package com.pan.cleverimage;

import android.graphics.Bitmap;
import android.util.Log;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    private static final String TAG = "ExampleUnitTest";
    private static final String FILE_URL0 = "https://ss2.baidu.com/6ONYsjip0QIZ8tyhnq/it/" +
            "u=522425600,1333123193&fm=173&s=9225BD08EA322A8EF73D7401030060C9&w=218&h=146&img.JPEG";

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void baseFunctionTest() {
        ExecutorService getterExecutorService = Executors.newFixedThreadPool(5);
        ExecutorService loaderExecutorService = Executors.newFixedThreadPool(5);

        UrlGetter urlGetter = new UrlGetter(getterExecutorService, FILE_URL0);
        BitmapSaver bitmapSaver = new BitmapSaver(loaderExecutorService);
        urlGetter.setNextTask(bitmapSaver);
        urlGetter.start();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}