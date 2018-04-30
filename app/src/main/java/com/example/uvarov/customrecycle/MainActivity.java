package com.example.uvarov.customrecycle;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.example.uvarov.customrecycle.DiscretScrollView.DiscreteScrollLayoutManager;
import com.example.uvarov.customrecycle.DiscretScrollView.DiscreteScrollView;
import com.example.uvarov.customrecycle.DiscretScrollView.ScaleTransformer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DiscreteScrollView mDiscreteScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDiscreteScrollView = findViewById(R.id.recycle_view);
//        mDiscreteScrollView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        DiscreteScrollLayoutManager layoutManager = new DiscreteScrollLayoutManager(this);
        layoutManager.addItemTransformer(new ScaleTransformer());
        mDiscreteScrollView.setLayoutManager(layoutManager);

        List<ItemModel> titles = new ArrayList<>();
        titles.add(new ItemModel("1", Color.BLUE));
        titles.add(new ItemModel("2", Color.BLACK));
        titles.add(new ItemModel("3", Color.CYAN));
        titles.add(new ItemModel("4", Color.GREEN));
        titles.add(new ItemModel("5", Color.GRAY));
        titles.add(new ItemModel("6", Color.MAGENTA));
        titles.add(new ItemModel("7", Color.RED));
        titles.add(new ItemModel("8", Color.DKGRAY));
        titles.add(new ItemModel("9", Color.YELLOW));
        RecycleAdapter adapter = new RecycleAdapter(this, titles);
        mDiscreteScrollView.setAdapter(adapter);
    }

    private class RecycleAdapter extends RecyclerView.Adapter<RecycleItem> {

        private Context context;
        private List<ItemModel> titles;

        public RecycleAdapter(Context context, List<ItemModel> titles) {
            this.context = context;
            this.titles = titles;
        }

        @Override
        public int getItemCount() {
            return titles.size();
        }

        @Override
        public RecycleItem onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecycleItem(LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecycleItem holder, int position) {
            holder.onBindViewHolder(titles.get(position));
        }
    }

}
