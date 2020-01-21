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
import com.arjanvlek.oxygenupdater.views.NewsAdapter.NewsItemReadListener
import kotlinx.android.synthetic.main.fragment_news.*

class NewsFragment : AbstractFragment(), NewsItemReadListener {
    private var hasBeenLoadedOnce = false
    private lateinit var alphaInAnimationAdapter: AlphaInAnimationAdapter

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

        serverConnector!!.getNews(applicationData, deviceId, updateMethodId) { newsItems -> displayNewsItems(newsItems, callback) }
    }

    private fun displayNewsItems(newsItems: List<NewsItem>, callback: () -> Unit) {
        newsContainer.let {
            // animate items when they load
            @Suppress("UNCHECKED_CAST")
            alphaInAnimationAdapter = AlphaInAnimationAdapter(
                NewsAdapter(context, activity as MainActivity?, this, newsItems) as RecyclerView.Adapter<RecyclerView.ViewHolder>
            )
            alphaInAnimationAdapter.setFirstOnly(false)

            it.adapter = alphaInAnimationAdapter
            it.layoutManager = LinearLayoutManager(context)
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

    private val loadDelayMilliseconds: Int
        get() {
            if (!hasBeenLoadedOnce) {
                hasBeenLoadedOnce = true
                return 3000
            }

            return 10
        }

    override fun onNewsItemRead(position: Int) {
        alphaInAnimationAdapter.notifyItemChanged(position)
    }

    companion object {
        private const val TAG = "NewsFragment"
    }
}
