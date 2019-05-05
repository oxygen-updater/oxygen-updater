package com.arjanvlek.oxygenupdater.updateinformation;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.text.style.ClickableSpan;
import android.view.View;

import com.arjanvlek.oxygenupdater.internal.logger.Logger;


public class FormattedURLSpan extends ClickableSpan {

    private final String url;

    public FormattedURLSpan(String url) {
        this.url = url;
    }

    @Override
    public void onClick(View widget) {
        Uri uri = Uri.parse(this.url);
        Context context = widget.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Logger.logError("FormattedURLSpan", "Activity was not found for intent", e);
        }
    }
}
