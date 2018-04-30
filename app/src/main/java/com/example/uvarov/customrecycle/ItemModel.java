package com.example.uvarov.customrecycle;

public class ItemModel {
    private String mText;
    private int mColor;

    public ItemModel(String text, int color) {
        this.mText = text;
        this.mColor = color;
    }

    public String getText() {
        return mText;
    }

    public int getColor() {
        return mColor;
    }
}
