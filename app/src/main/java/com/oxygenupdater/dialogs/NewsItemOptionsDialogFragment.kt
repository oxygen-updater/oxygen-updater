package com.oxygenupdater.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.getSystemService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.oxygenupdater.R
import com.oxygenupdater.adapters.NewsListAdapter
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.viewmodels.NewsViewModel
import kotlinx.android.synthetic.main.bottom_sheet_news_item_options.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

/**
 * Wrapper around [BottomSheetDialogFragment]
 */
class NewsItemOptionsDialogFragment : BottomSheetDialogFragment() {

    lateinit var newsItem: NewsItem

    private var clipboard: ClipboardManager? = null

    private val newsViewModel by sharedViewModel<NewsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(
        R.layout.bottom_sheet_news_item_options,
        container,
        false
    ).also {
        clipboard = context?.getSystemService()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) = setupViews()

    private fun setupViews() {
        markAsReadTextView.apply {
            text = getString(
                if (newsItem.read) {
                    R.string.news_mark_unread
                } else {
                    R.string.news_mark_read
                }
            )

            setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (newsItem.read) {
                    R.drawable.cancel
                } else {
                    R.drawable.done_outline
                }, 0, 0, 0
            )

            setOnClickListener {
                if (checkInitialized() && newsItem.id != null) {
                    newsViewModel.toggleReadStatus(newsItem)
                    NewsListAdapter.itemReadStatusChangedListener?.invoke(
                        newsItem.id!!,
                        !newsItem.read
                    )
                }

                dismiss()
            }
        }

        openInBrowserTextView.setOnClickListener {
            @Suppress("ControlFlowWithEmptyBody")
            if (checkInitialized()) {
                // no-op for now because the app has custom intent filters that
                // may cause the article to open in the app itself (e.g. if the
                // user has chosen "Set to always open"). Bypassing intent
                // filters is hacky and likely to break in newer APIs (because
                // it involves resolving the intent and checking package names).
            }

            dismiss()
        }

        shareLinkTextView.setOnClickListener {
            if (checkInitialized()) {
                val prefix = "${getString(R.string.app_name)}: ${newsItem.title}"
                val intent = Intent(Intent.ACTION_SEND)
                    // Rich text preview: should work only on API 29+
                    .putExtra(Intent.EXTRA_TITLE, newsItem.title)
                    // Main share content: will work on all API levels
                    .putExtra(
                        Intent.EXTRA_TEXT,
                        "$prefix\n\n${newsItem.url}"
                    ).setType("text/plain")

                startActivity(Intent.createChooser(intent, null))
            }

            dismiss()
        }

        copyLinkTextView.setOnClickListener {
            if (checkInitialized() && clipboard != null) {
                ClipData.newPlainText(
                    getString(R.string.app_name),
                    newsItem.url
                ).let {
                    clipboard!!.setPrimaryClip(it)

                    Toast.makeText(
                        context,
                        getString(R.string.copy_toast_msg),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            dismiss()
        }
    }

    private fun checkInitialized() = if (this::newsItem.isInitialized) {
        true
    } else {
        logWarning(TAG, "`newsItem` hasn't been initialized yet")
        false
    }

    companion object {
        const val TAG = "NewsItemOptionsDialogFragment"
    }
}
