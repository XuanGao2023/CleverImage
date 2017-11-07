package com.pan.testapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.pan.cleverimage.CleverImageView;
import com.pan.cleverimage.ImageGetter;

public class MainActivity extends AppCompatActivity {
    private TextView textViewTest;
    private CleverImageView cleverImageView;
    private static final String FILE_URL0 = "https://ss2.baidu.com/6ONYsjip0QIZ8tyhnq/it/" +
            "u=522425600,1333123193&fm=173&s=9225BD08EA322A8EF73D7401030060C9&w=218&h=146&img.JPEG";
    private static final String FILE_URL1 = "https://ss0.baidu.com/6ONWsjip0QIZ8tyhnq/it/" +
            "u=2861671602,4205204930&fm=173&s=50019D5786616CA4793D90CB03008031&w=218&h=146&img.JPEG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();

        ImageGetter.initInstance();

        cleverImageView = (CleverImageView) findViewById(R.id.cleverImageView);
        findViewById(R.id.btnGetPic0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageGetter.getPic(FILE_URL0, new ImageGetter.ImageGotListener() {
                    @Override
                    public void OnImageGot(Bitmap bitmap) {
                        System.out.println("OnImageGot: " + FILE_URL0 + bitmap);
                        cleverImageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
        findViewById(R.id.btnGetPic1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageGetter.getPic(FILE_URL1, true, new ImageGetter.ImageGotListener() {
                    @Override
                    public void OnImageGot(Bitmap bitmap) {
                        System.out.println("OnImageGot: " + FILE_URL1 + bitmap);
                        cleverImageView.setImageBitmap(bitmap);
                    }
                });
            }
        });

        findViewById(R.id.btnClearMemCache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageGetter.getInstance().clearMemCache();
            }
        });
        findViewById(R.id.btnClearDiskCache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageGetter.getInstance().clearDiskCache();
            }
        });
        findViewById(R.id.btnCircle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleverImageView.setDisableCircularTransformation(false);
            }
        });
        findViewById(R.id.btnDisableCircle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleverImageView.setDisableCircularTransformation(true);
            }
        });
    }

    public void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
}
