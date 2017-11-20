package com.pan.testapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.pan.cleverimage.ImageGetter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by pan on 18/11/2017.
 */

public class TestActivity extends AppCompatActivity {
    private static final String FILE_URL0 = "https://ss2.baidu.com/6ONYsjip0QIZ8tyhnq/it/" +
            "u=522425600,1333123193&fm=173&s=9225BD08EA322A8EF73D7401030060C9&w=218&h=146&img.JPEG";
    Button btnTest;
    ImageView imageView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        btnTest = (Button) findViewById(R.id.btnTest);
        imageView = (ImageView) findViewById(R.id.imageView);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExecutorService getter = Executors.newFixedThreadPool(5);
                ExecutorService decoder = Executors.newFixedThreadPool(5);
                ExecutorService saver = Executors.newFixedThreadPool(5);
                ImageGetter builder = ImageGetter.getInstance();
                builder.getImage(FILE_URL0, new ImageGetter.Callback() {
                    @Override
                    public void OnImageGot(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }
}
