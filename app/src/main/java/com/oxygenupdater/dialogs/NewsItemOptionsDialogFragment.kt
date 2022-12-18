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
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.oxygenupdater.R
import com.oxygenupdater.adapters.NewsListAdapter
import com.oxygenupdater.databinding.BottomSheetNewsItemOptionsBinding
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.viewmodels.NewsViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Wrapper around [BottomSheetDialogFragment]
 */
class NewsItemOptionsDialogFragment : BottomSheetDialogFragment() {

    lateinit var newsItem: NewsItem

    private var clipboard: ClipboardManager? = null

    private val newsViewModel by activityViewModel<NewsViewModel>()

    /** Only valid between `onCreateView` and `onDestroyView` */
    private var binding: BottomSheetNewsItemOptionsBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = BottomSheetNewsItemOptionsBinding.inflate(inflater, container, false).run {
        binding = this
        root
    }.also {
        clipboard = context?.getSystemService()
    }

    override fun onDestroyView() = super.onDestroyView().also {
        binding = null
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) = ifInitialized {
        setupViews()
    }

    private fun setupViews() {
        val read = newsItem.read
        val title = newsItem.title
        val fullUrl = newsItem.webUrl

        binding?.markAsReadTextView?.apply {
            text = getString(
                if (read) R.string.news_mark_unread
                else R.string.news_mark_read
            )

            setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (read) R.drawable.cancel
                else R.drawable.done_outline,
                0, 0, 0
            )

            setOnClickListener {
                if (newsItem.id != null) {
                    newsViewModel.toggleReadStatus(newsItem)
                    NewsListAdapter.itemReadStatusChangedListener?.invoke(newsItem.id!!, !newsItem.read)
                }

                dismiss()
            }
        }

        binding?.openInBrowserTextView?.setOnClickListener {
            // Bypassing intent filters is hacky and likely to break in newer APIs
            // (because it involves resolving the intent and checking package names).
            Intent.makeMainSelectorActivity(
                Intent.ACTION_MAIN,
                Intent.CATEGORY_APP_BROWSER
            ).apply {
                data = fullUrl.toUri()
                startActivity(this)
            }

            dismiss()
        }

        binding?.shareLinkTextView?.setOnClickListener {
            val prefix = "${getString(R.string.app_name)}: $title"
            val intent = Intent(Intent.ACTION_SEND)
                // Rich text preview: should work only on API 29+
                .putExtra(Intent.EXTRA_TITLE, title)
                // Main share content: will work on all API levels
                .putExtra(
                    Intent.EXTRA_TEXT,
                    "$prefix\n\n$fullUrl"
                ).setType("text/plain")

            startActivity(Intent.createChooser(intent, null))

            dismiss()
        }

        binding?.copyLinkTextView?.setOnClickListener {
            val clip = ClipData.newPlainText(getString(R.string.app_name), fullUrl)
            clipboard?.setPrimaryClip(clip)?.also {
                Toast.makeText(
                    context,
                    getString(androidx.browser.R.string.copy_toast_msg),
                    Toast.LENGTH_LONG
                ).show()
            }

            dismiss()
        }
    }

    private fun ifInitialized(block: () -> Unit) = if (this::newsItem.isInitialized) {
        block.invoke()
    } else {
        logWarning(TAG, "`newsItem` hasn't been initialized yet")
    }

    companion object {
        const val TAG = "NewsItemOptionsDialogFragment"
    }
}
