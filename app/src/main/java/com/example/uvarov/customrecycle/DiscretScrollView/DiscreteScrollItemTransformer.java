package com.example.uvarov.customrecycle.DiscretScrollView;

import android.view.View;

public interface DiscreteScrollItemTransformer {
    void transformItem(View item, float position);
}