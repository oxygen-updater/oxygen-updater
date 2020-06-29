package com.oxygenupdater.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.oxygenupdater.R
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.adapters.AlphaInAnimationAdapter
import com.oxygenupdater.adapters.NewsAdapter
import com.oxygenupdater.database.NewsDatabaseHelper
import com.oxygenupdater.exceptions.OxygenUpdaterException
import com.oxygenupdater.extensions.addPlaceholderItemsForShimmer
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_news.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class NewsFragment : AbstractFragment(R.layout.fragment_news) {

    private var hasBeenLoadedOnce = false
    private var isShowingOnlyUnreadArticles = false

    private lateinit var newsAdapter: NewsAdapter

    private val newsDatabaseHelper by inject<NewsDatabaseHelper>()
    private val mainViewModel by sharedViewModel<MainViewModel>()

    /**
     * Re-use the same observer to avoid duplicated callbacks
     */
    private val fetchNewsObserver = Observer<List<NewsItem>> { newsItems ->
        displayNewsItems(newsItems)

        swipeRefreshLayout.isRefreshing = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = super.onCreateView(inflater, container, savedInstanceState).also {
        it?.post {
            // placeholderItem's height is 2x 16dp padding + 64dp image = 96dp
            addPlaceholderItemsForShimmer(inflater, container, it, R.layout.placeholder_news_item, 96f)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Load the news after up to 3 seconds to allow the update info screen to load first
        // This way, the app feels a lot faster. Also, it doesn't affect users that much, as they will always see the update info screen first.
        Handler().postDelayed({ refreshNews() }, loadDelayMilliseconds.toLong())

        swipeRefreshLayout.apply {
            setOnRefreshListener { refreshNews() }
            setColorSchemeResources(R.color.colorPrimary)
        }
    }

    private fun refreshNews() {
        // If the view was suspended during the 3-second delay, stop performing any further actions.
        if (!isAdded || activity == null) {
            return
        }

        shimmerFrameLayout.isVisible = true

        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        mainViewModel.fetchNews(deviceId, updateMethodId).observe(viewLifecycleOwner, fetchNewsObserver)
    }

    private fun displayNewsItems(newsItems: List<NewsItem>) {
        shimmerFrameLayout.isVisible = false

        if (newsItems.isNullOrEmpty()) {
            displayEmptyState()
            return
        }

        updateBannerText(newsItems.count { !it.read })

        newsContainer.let { recyclerView ->
            // respect unread articles filtering
            val itemList = if (isShowingOnlyUnreadArticles) {
                newsItems.filter { !it.read }
            } else {
                newsItems
            }

            newsAdapter = NewsAdapter(context, itemList) { newsItemId ->
                newsAdapter.markItemAsRead(newsItemId)
                updateBannerText(newsAdapter.itemList.count { !it.read })

                if (isShowingOnlyUnreadArticles) {
                    // Remove the item if the user is seeing only unread articles
                    newsAdapter.updateList(
                        newsAdapter.itemList.filter { !it.read }
                    )
                }
            }

            // animate items when they load
            @Suppress("UNCHECKED_CAST")
            AlphaInAnimationAdapter(newsAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>).apply {
                setFirstOnly(false)
                recyclerView.adapter = this
            }

            // toggle between showing only reading articles and showing all articles
            bannerTextView.apply {
                setOnClickListener {
                    newsAdapter.updateList(
                        if (isShowingOnlyUnreadArticles) newsItems
                        else newsItems.filter { !it.read }
                    )

                    isShowingOnlyUnreadArticles = !isShowingOnlyUnreadArticles

                    text = getString(
                        if (isShowingOnlyUnreadArticles) {
                            R.string.news_unread_count_2
                        } else {
                            R.string.news_unread_count_1
                        },
                        newsAdapter.itemList.count { !it.read }
                    )

                    if (newsAdapter.itemList.isEmpty()) {
                        if (isShowingOnlyUnreadArticles) {
                            displayEmptyState(true)
                        } else {
                            displayEmptyState()
                        }
                    } else {
                        emptyStateLayout.isVisible = false
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
    }

    private fun updateBannerText(count: Int) {
        if (!isAdded) {
            return
        }

        bannerTextView.apply {
            isVisible = true
            text = getString(
                if (isShowingOnlyUnreadArticles) {
                    R.string.news_unread_count_2
                } else {
                    R.string.news_unread_count_1
                },
                count
            )

            if (count != 0) {
                setOnLongClickListener { view ->
                    if (isAdded && context != null) {
                        PopupMenu(requireContext(), view).apply {
                            inflate(R.menu.menu_mark_articles_read)
                            show()

                            setOnMenuItemClickListener {
                                newsAdapter.itemList.filter {
                                    !it.read
                                }.forEach { newsItem ->
                                    if (newsItem.id != null) {
                                        // Mark the item as read on the device.
                                        newsDatabaseHelper.markNewsItemRead(newsItem)
                                        newsAdapter.markItemAsRead(newsItem.id)
                                    }
                                }

                                updateBannerText(0)

                                if (isShowingOnlyUnreadArticles) {
                                    displayEmptyState(true)
                                }

                                true
                            }
                        }
                    }
                    true
                }
            } else {
                setOnLongClickListener(null)
            }
        }

        // display badge with the number of unread news articles
        // if there aren't any unread articles, the badge is hidden
        (activity as MainActivity?)?.updateTabBadge(0, count != 0, count)
    }

    private fun displayEmptyState(isAllReadEmptyState: Boolean = false) {
        emptyStateLayout.apply {
            isVisible = true
            startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
        }

        emptyStateHeader.text = getString(
            if (isAllReadEmptyState) {
                R.string.news_empty_state_all_read_header
            } else {
                R.string.news_empty_state_none_available_header
            }
        )

        emptyStateText.text = getString(
            if (isAllReadEmptyState) {
                R.string.news_empty_state_all_read_text
            } else {
                R.string.news_empty_state_none_available_text
            }
        )
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
