package com.example.uvarov.customrecycle;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

class RecycleItem extends RecyclerView.ViewHolder {

    private TextView titleView;

    public RecycleItem(View root) {
        super(root);
        titleView = root.findViewById(R.id.title);
    }

    public void onBindViewHolder(final String title) {
        titleView.setText(title);
    }
}