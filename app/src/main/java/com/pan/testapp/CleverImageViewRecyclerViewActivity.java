package com.pan.testapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pan.cleverimage.CleverImageView;
import com.pan.cleverimage.ImageGetter;

/**
 * Created by pan on 24/11/2017.
 */

public class CleverImageViewRecyclerViewActivity extends AppCompatActivity {
    private static final String TAG = "CleverImageViewRecycler";

    ImageAdapter imageAdapter;
    RecyclerView recyclerView;
    String[] images = Images.imageUrls;
    private static int activityCount = 0;
    int activityId;

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
        activityCount ++;
        activityId = activityCount;
        setTitle("" + activityId);
        Log.d(TAG, "startActivity id: " + activityId);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_imageviewrecyclerview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clearMemCache:
                ImageGetter.clearMemCache();
                return true;
            case R.id.clearDiskCache:
                ImageGetter.clearDiskCache();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ItemViewHolder> {

        class ItemViewHolder extends RecyclerView.ViewHolder {
            public CleverImageView cleverImageView;

            public ItemViewHolder(View itemView) {
                super(itemView);
                cleverImageView = (CleverImageView) itemView.findViewById(R.id.cleverImageView);
            }
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ItemViewHolder viewholder = new ItemViewHolder(
                    LayoutInflater.from(getBaseContext()).inflate(R.layout.recyerview_cleverimageview_item, parent, false));
            return viewholder;
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            if (position < images.length) {
                String url = images[position];
                holder.cleverImageView.setImageUrl(url);
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
        Log.d(TAG, "finalizeActivity id: " + activityId);
    }
}
