package com.example.uvarov.customrecycle;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecycleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecycleView = findViewById(R.id.recycle_view);
        mRecycleView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        List<String> titles = new ArrayList<>();
        titles.add("1");
        titles.add("2");
        titles.add("3");
        titles.add("4");
        titles.add("5");
        titles.add("6");
        titles.add("7");
        titles.add("8");
        titles.add("9");
        RecycleAdapter adapter = new RecycleAdapter(this, titles);
        mRecycleView.setAdapter(adapter);
    }

    private class RecycleAdapter extends RecyclerView.Adapter<RecycleItem> {

        private Context context;
        private List<String> titles;

        public RecycleAdapter(Context context, List<String> titles) {
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
