package com.pan.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.pan.cleverimage.ImageGetter;

import static com.pan.testapp.Images.imageUrls;

/**
 * Created by pan on 24/11/2017.
 */

public class ImageViewRecyclerViewActivity extends AppCompatActivity {
    private static final String TAG = "ImageViewRecycler";
    ImageAdapter imageAdapter;
    RecyclerView recyclerView;
    String[] images = imageUrls;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recyclerview);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        imageAdapter = new ImageAdapter();
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(imageAdapter);
        imageAdapter.notifyDataSetChanged();
    }

    class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ItemViewHolder> {

        class ItemViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;

            public ItemViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView.findViewById(R.id.imageView);
                textView = (TextView) itemView.findViewById(R.id.textView);
            }
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ItemViewHolder viewholder = new ItemViewHolder(
                    LayoutInflater.from(getBaseContext()).inflate(R.layout.recyclerview_imageview_item, parent, false));
            return viewholder;
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            if (position < images.length) {
                String url = images[position];
                ImageGetter.loadPic(holder.imageView, url);
                holder.textView.setText("" + position);
                holder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setClass(getBaseContext(), ImageViewRecyclerViewActivity.class);
                        startActivity(intent);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return images.length;
        }
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(TAG, "finished!");
    }
}
