package com.example.uvarov.customrecycle;

import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

class RecycleItem extends RecyclerView.ViewHolder {

    private TextView titleView;
    private FrameLayout container;

    public RecycleItem(View root) {
        super(root);
        titleView = root.findViewById(R.id.title);
        container = root.findViewById(R.id.container);
    }

    public void onBindViewHolder(final ItemModel itemModel) {
        titleView.setText(itemModel.getText());
        container.setBackground(new ColorDrawable(itemModel.getColor()));
    }
}