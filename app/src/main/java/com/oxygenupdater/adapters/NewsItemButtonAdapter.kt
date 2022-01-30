package com.oxygenupdater.adapters

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.core.content.getSystemService
import com.oxygenupdater.R
import com.oxygenupdater.models.GridButton
import com.oxygenupdater.models.NewsItem

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class NewsItemButtonAdapter(
    private val activity: Activity,
    private val newsItem: NewsItem
) : GridButtonAdapter(
    arrayOf(
        GridButton(activity.getString(androidx.browser.R.string.fallback_menu_item_share_link), R.drawable.share),
        GridButton(activity.getString(androidx.browser.R.string.fallback_menu_item_copy_link), R.drawable.link)
    )
) {

    init {
        init { _, item ->
            when (item.drawableResId) {
                R.drawable.share -> activity.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND)
                            // Rich text preview: should work only on API 29+
                            .putExtra(Intent.EXTRA_TITLE, newsItem.title)
                            // Main share content: will work on all API levels
                            .putExtra(
                                Intent.EXTRA_TEXT,
                                "${activity.getString(R.string.app_name)}: ${newsItem.title}\n\n${newsItem.webUrl}"
                            ).setType("text/plain"), null
                    )
                )
                R.drawable.link -> ClipData.newPlainText(
                    activity.getString(R.string.app_name),
                    newsItem.webUrl
                ).let {
                    activity.getSystemService<ClipboardManager>()?.setPrimaryClip(it)

                    Toast.makeText(
                        activity,
                        activity.getString(androidx.browser.R.string.copy_toast_msg),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
