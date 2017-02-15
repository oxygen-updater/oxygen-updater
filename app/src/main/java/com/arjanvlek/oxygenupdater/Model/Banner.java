package com.arjanvlek.oxygenupdater.Model;


import android.content.Context;

public interface Banner {

    CharSequence getBannerText(Context context);
    int getColor(Context context);
}
