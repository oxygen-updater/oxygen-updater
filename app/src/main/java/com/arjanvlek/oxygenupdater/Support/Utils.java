package com.arjanvlek.oxygenupdater.Support;

import android.app.Activity;
import android.util.TypedValue;

public class Utils {

    /**
     * Converts DiP units to pixels
     */
    public static int diPToPixels(Activity activity, int numberOfPixels) {
        if (activity != null && activity.getResources() != null) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    numberOfPixels,
                    activity.getResources().getDisplayMetrics()
            );
        } else {
            return 0;
        }
    }
}
