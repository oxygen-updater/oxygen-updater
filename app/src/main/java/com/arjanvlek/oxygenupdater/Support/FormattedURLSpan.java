package com.arjanvlek.oxygenupdater.Support;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;


class FormattedURLSpan extends ClickableSpan {

    private final String url;

    FormattedURLSpan(String url) {
        this.url = url;
    }

    @Override
    public void onClick(View widget) {
        Uri uri = Uri.parse(this.url);
        Context context = widget.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w("FormattedURLSpan", "Activity was not found for intent, " + intent.toString());
        }
    }
}
