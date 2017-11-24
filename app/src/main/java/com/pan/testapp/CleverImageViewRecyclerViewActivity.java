package com.pan.testapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pan.cleverimage.CleverImageView;

/**
 * Created by pan on 24/11/2017.
 */

public class CleverImageViewRecyclerViewActivity extends AppCompatActivity {
    ImageAdapter imageAdapter;
    RecyclerView recyclerView;
    String[] images = Images.imageUrls;

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
            public CleverImageView cleverImageView;
            public TextView textView;

            public ItemViewHolder(View itemView) {
                super(itemView);
                cleverImageView = (CleverImageView) itemView.findViewById(R.id.cleverImageView);
                textView = (TextView) itemView.findViewById(R.id.textView);
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
                holder.textView.setText("" + position);
            }
        }

        @Override
        public int getItemCount() {
            return images.length;
        }
    }
}
