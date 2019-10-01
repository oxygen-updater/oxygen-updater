package com.arjanvlek.oxygenupdater.news

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.AbstractFragment
import java8.util.function.Consumer

class NewsFragment : AbstractFragment() {
    private var hasBeenLoadedOnce = false

    private val loadDelayMilliseconds: Int
        get() {
            if (!hasBeenLoadedOnce) {
                hasBeenLoadedOnce = true
                return 3000
            }

            return 10
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Load the news after up to 3 seconds to allow the update info screen to load first
        // This way, the app feels a lot faster. Also, it doesn't affect users that much, as they will always see the update info screen first.
        Handler().postDelayed({ refreshNews(view, null) }, loadDelayMilliseconds.toLong())

        val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.newsRefreshContainer)
        refreshLayout.setOnRefreshListener { refreshNews(view, Consumer { refreshLayout.isRefreshing = false }) }
    }

    private fun refreshNews(view: View, callback: Consumer<Void>?) {
        // If the view was suspended during the 3-second delay, stop performing any further actions.
        if (!isAdded || activity == null) {
            return
        }
        val deviceId = getSettingsManager().getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = getSettingsManager().getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        serverConnector.getNews(getApplicationData(), deviceId, updateMethodId, Consumer {
            newsItems -> displayNewsItems(view, newsItems, callback)
        })
    }

    private fun displayNewsItems(view: View, newsItems: List<NewsItem>, callback: Consumer<Void>?) {
        val newsContainer = view.findViewById<RecyclerView>(R.id.newsContainer)

        // animate items when they load
        // TODO (FIX)
        /*val alphaInAnimationAdapter = AlphaInAnimationAdapter(NewsAdapter(context, activity as MainActivity?, newsItems))
        alphaInAnimationAdapter.setFirstOnly(false)
        newsContainer.adapter = alphaInAnimationAdapter*/
        newsContainer.layoutManager = LinearLayoutManager(context)

        if (!isAdded) {
            logError(TAG, OxygenUpdaterException("isAdded() returned false (displayNewsItems)"))
            return
        }

        if (activity == null) {
            logError(TAG, OxygenUpdaterException("getActivity() returned null (displayNewsItems)"))
            return
        }

        callback?.accept(null)
    }

    companion object {
        private const val TAG = "NewsFragment"
    }
}
