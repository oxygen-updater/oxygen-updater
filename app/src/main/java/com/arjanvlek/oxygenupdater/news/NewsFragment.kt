package com.arjanvlek.oxygenupdater.news

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
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
        return inflater.inflate(R.layout.fragment_news, container, false)
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

        val deviceId = settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager!!.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        serverConnector!!.getNews(applicationData, deviceId, updateMethodId) { displayNewsItems(it, callback) }
    }

    private fun displayNewsItems(newsItems: List<NewsItem>, callback: () -> Unit) {
        updateBannerText(getString(R.string.news_unread_count, newsItems.count { !it.read }))

        newsContainer.let { recyclerView ->
            newsAdapter = NewsAdapter(context, activity as MainActivity?, newsItems) { position ->
                newsAdapter.notifyItemChanged(position)
                updateBannerText(getString(R.string.news_unread_count, newsItems.count { !it.read }))
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

    private fun updateBannerText(string: String) {
        bannerLayout.visibility = View.VISIBLE
        bannerTextView.text = string
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
