package com.heimavista.fiedorasport.widget;

import android.content.Context;
import android.widget.ImageView;

public abstract class NineGridImageViewAdapter<T> {
    protected abstract void onDisplayImage(Context context, ImageView imageView, T t);

    protected ImageView generateImageView(Context context) {
        ImageView imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return imageView;
    }
}