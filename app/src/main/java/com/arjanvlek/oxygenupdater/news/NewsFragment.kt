package com.arjanvlek.oxygenupdater.news

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.AbstractFragment
import com.arjanvlek.oxygenupdater.views.AlphaInAnimationAdapter
import com.arjanvlek.oxygenupdater.views.MainActivity
import com.arjanvlek.oxygenupdater.views.NewsAdapter
import kotlinx.android.synthetic.main.fragment_news.*

class NewsFragment : AbstractFragment() {

    private var hasBeenLoadedOnce = false
    private var isShowingOnlyUnreadArticles = false

    private lateinit var newsAdapter: NewsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_news, container, false).also {
            it.post { addPlaceholderItemsForShimmer(inflater, container, it) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Load the news after up to 3 seconds to allow the update info screen to load first
        // This way, the app feels a lot faster. Also, it doesn't affect users that much, as they will always see the update info screen first.
        Handler().postDelayed({ refreshNews {} }, loadDelayMilliseconds.toLong())

        newsRefreshContainer.apply {
            setOnRefreshListener { refreshNews { isRefreshing = false } }
        }
    }

    private fun refreshNews(callback: () -> Unit) {
        // If the view was suspended during the 3-second delay, stop performing any further actions.
        if (!isAdded || activity == null) {
            return
        }

        shimmerFrameLayout.visibility = VISIBLE

        val deviceId = settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager!!.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        serverConnector!!.getNews(applicationData, deviceId, updateMethodId) { displayNewsItems(it, callback) }
    }

    private fun displayNewsItems(newsItems: List<NewsItem>, callback: () -> Unit) {
        shimmerFrameLayout.visibility = GONE

        if (newsItems.isNullOrEmpty()) {
            displayEmptyState()
            return
        }

        updateBannerText(getString(R.string.news_unread_count, newsItems.count { !it.read }))

        newsContainer.let { recyclerView ->
            newsAdapter = NewsAdapter(context, activity as MainActivity?, newsItems) { newsItemId ->
                newsAdapter.markItemAsRead(newsItemId)
                updateBannerText(getString(R.string.news_unread_count, newsAdapter.itemList.count { !it.read }))
            }

            // animate items when they load
            @Suppress("UNCHECKED_CAST")
            AlphaInAnimationAdapter(newsAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>).apply {
                setFirstOnly(false)
                recyclerView.adapter = this
            }

            recyclerView.layoutManager = LinearLayoutManager(context)

            // toggle between showing only reading articles and showing all articles
            bannerLayout.setOnClickListener {
                newsAdapter.updateList(
                    if (isShowingOnlyUnreadArticles) newsItems
                    else newsItems.filter { !it.read }
                )

                isShowingOnlyUnreadArticles = !isShowingOnlyUnreadArticles

                if (newsAdapter.itemList.isEmpty()) {
                    if (isShowingOnlyUnreadArticles) {
                        displayEmptyState(true)
                    } else {
                        displayEmptyState()
                    }
                }
            }
        }

        if (!isAdded) {
            logDebug(TAG, "isAdded() returned false (displayNewsItems)")
            return
        }

        if (activity == null) {
            logError(TAG, OxygenUpdaterException("getActivity() returned null (displayNewsItems)"))
            return
        }

        callback.invoke()
    }

    /**
     * Inflates and adds as many placeholderItems as necessary, as per the calculation: rootView.height / placeholderItemHeight
     *
     * @param inflater the LayoutInflater
     * @param container the container
     * @param rootView this fragment's rootView
     */
    private fun addPlaceholderItemsForShimmer(inflater: LayoutInflater, container: ViewGroup?, rootView: View) {
        val parent = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
        }

        // calculate how many placeholderItems to add
        // placeholderItem's height is 2x 16dp padding + 64dp image = 96dp
        // we have to hardcode the height, because View.getHeight() returns 0 for views that haven't been drawn yet
        // View.post() won't work either, because control goes into the lambda only after the view has been drawn
        val count = rootView.height / Utils.dpToPx(context!!, 96f).toInt()

        // add `count + 1` placeholderItems
        for (i in 0..count) {
            parent.addView(
                // each placeholderItem must be inflated within the loop to avoid
                // the "The specified child already has a parent. You must call removeView() on the child's parent first." error
                inflater.inflate(R.layout.placeholder_news_item, container, false)
            )
        }

        shimmerFrameLayout.addView(parent)
    }

    private fun updateBannerText(string: String) {
        bannerLayout.visibility = VISIBLE
        bannerTextView.text = string
    }

    private fun displayEmptyState(isAllReadEmptyState: Boolean = false) {
        emptyStateLayout.visibility = VISIBLE

        emptyStateHeader.text = if (isAllReadEmptyState) {
            getString(R.string.news_empty_state_all_read_header)
        } else {
            getString(R.string.news_empty_state_none_available_header)
        }

        emptyStateText.text = if (isAllReadEmptyState) {
            getString(R.string.news_empty_state_all_read_text)
        } else {
            getString(R.string.news_empty_state_none_available_text)
        }
    }

    private val loadDelayMilliseconds = if (!hasBeenLoadedOnce) {
        hasBeenLoadedOnce = true
        3000
    } else {
        10
    }

    companion object {
        private const val TAG = "NewsFragment"
    }
}
