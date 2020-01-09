package com.arjanvlek.oxygenupdater.updateinformation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Browser
import android.text.style.ClickableSpan
import android.view.View
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError

class FormattedURLSpan(private val url: String?) : ClickableSpan() {

    override fun onClick(widget: View) {
        val uri = Uri.parse(url)
        val context = widget.context

        val intent = Intent(Intent.ACTION_VIEW, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logError("FormattedURLSpan", "Activity was not found for intent", e)
        }
    }
}
