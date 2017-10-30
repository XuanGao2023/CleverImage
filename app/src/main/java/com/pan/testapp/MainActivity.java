package com.pan.testapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.pan.cleverimage.ImageGetter;

public class MainActivity extends AppCompatActivity {
	private TextView textViewTest;
	private ImageView imageViewTest;
	private static final String FILE_PATH = "https://ss2.baidu.com/6ONYsjip0QIZ8tyhnq/it/u=522425600,1333123193&fm=173&s=9225BD08EA322A8EF73D7401030060C9&w=218&h=146&img.JPEG";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		checkPermission();

		ImageGetter.initInstance();

		imageViewTest = (ImageView) findViewById(R.id.imageViewTest);
		findViewById(R.id.btnGetPic).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ImageGetter.getPic(FILE_PATH, new ImageGetter.ImageGotListener() {
					@Override
					public void OnImageGot(Bitmap bitmap) {
						System.out.println("OnImageGot: " + FILE_PATH + bitmap);
						imageViewTest.setImageBitmap(bitmap);
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
	}

	public void checkPermission() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
		}
	}
}
