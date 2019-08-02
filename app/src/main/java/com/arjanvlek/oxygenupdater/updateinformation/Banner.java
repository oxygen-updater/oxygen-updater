package com.arjanvlek.oxygenupdater.updateinformation;


import android.content.Context;

public interface Banner {

	CharSequence getBannerText(Context context);

	int getColor(Context context);
}
