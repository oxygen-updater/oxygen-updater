package com.oxygenupdater.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.oxygenupdater.R
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.adapters.AlphaInAnimationAdapter
import com.oxygenupdater.adapters.NewsListAdapter
import com.oxygenupdater.exceptions.OxygenUpdaterException
import com.oxygenupdater.extensions.addPlaceholderItemsForShimmer
import com.oxygenupdater.internal.NewsListChangedListener
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.PROPERTY_DEVICE_ID
import com.oxygenupdater.internal.settings.PrefManager.PROPERTY_UPDATE_METHOD_ID
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.viewmodels.MainViewModel
import com.oxygenupdater.viewmodels.NewsViewModel
import kotlinx.android.synthetic.main.fragment_news_list.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class NewsListFragment : Fragment(R.layout.fragment_news_list) {

    private var hasBeenLoadedOnce = false
    private var isShowingOnlyUnreadArticles = false

    private lateinit var newsListAdapter: NewsListAdapter

    private val mainViewModel by sharedViewModel<MainViewModel>()
    private val newsViewModel by sharedViewModel<NewsViewModel>()

    /**
     * Re-use the same observer to avoid duplicated callbacks
     */
    private val fetchNewsObserver = Observer<List<NewsItem>> { newsItems ->
        displayNewsItems(newsItems)

        swipeRefreshLayout.isRefreshing = false
    }

    /**
     * Update banner text and empty states when the adapter reports that the list
     * has changed. The lambda param is a pair of `(unreadCount, isEmpty)`.
     */
    private val adapterListChangedListener: NewsListChangedListener = { unreadCount, isEmpty ->
        if (isAdded) {
            bannerTextView.text = getString(
                if (isShowingOnlyUnreadArticles) {
                    R.string.news_unread_count_2
                } else {
                    R.string.news_unread_count_1
                },
                unreadCount
            )

            if (isEmpty) {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = super.onCreateView(inflater, container, savedInstanceState).also {
        it?.post {
            // placeholderItem's height is 2x 16dp padding + 64dp image = 96dp
            addPlaceholderItemsForShimmer(inflater, container, it, R.layout.placeholder_item_news, 96f)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        updateBannerText(0)

        // Load the news after up to 3 seconds to allow the update info screen to load first
        // This way, the app feels a lot faster. Also, it doesn't affect users that much, as they will always see the update info screen first.
        Handler(Looper.getMainLooper()).postDelayed({ refreshNews() }, loadDelayMilliseconds.toLong())

        mainViewModel.menuClicked.observe(viewLifecycleOwner) {
            if (it == R.id.action_mark_articles_read) {
                markAllRead()
            }
        }

        mainViewModel.settingsChanged.observe(viewLifecycleOwner) {
            if (it == PROPERTY_DEVICE_ID || it == PROPERTY_UPDATE_METHOD_ID) {
                refreshNews()
            }
        }

        swipeRefreshLayout.apply {
            setOnRefreshListener { refreshNews() }
            setColorSchemeResources(R.color.colorPrimary)
        }
    }

    private fun setupRecyclerView() {
        newsContainer.let { recyclerView ->
            newsListAdapter = NewsListAdapter(requireActivity(), adapterListChangedListener) { newsItemId, isRead ->
                newsListAdapter.changeItemReadStatus(newsItemId, isRead)
                updateBannerText(newsListAdapter.currentList.count { !it.read })

                if (isShowingOnlyUnreadArticles) {
                    // Remove the item if the user is seeing only unread articles
                    newsListAdapter.submitList(
                        newsListAdapter.currentList.filter { !it.read }
                    )
                }
            }

            // animate items when they load
            @Suppress("UNCHECKED_CAST")
            AlphaInAnimationAdapter(newsListAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>).apply {
                setFirstOnly(false)
                // Performance optimization
                recyclerView.setHasFixedSize(true)
                recyclerView.adapter = this
            }
        }
    }

    private fun refreshNews() {
        // If the view was suspended during the 3-second delay, stop performing any further actions.
        if (!isAdded || activity == null) {
            return
        }

        shimmerFrameLayout.isVisible = true

        val deviceId = PrefManager.getLong(PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = PrefManager.getLong(PROPERTY_UPDATE_METHOD_ID, -1L)

        newsViewModel.fetchNewsList(deviceId, updateMethodId).observe(viewLifecycleOwner, fetchNewsObserver)
    }

    private fun displayNewsItems(newsItems: List<NewsItem>) {
        if (!isAdded) {
            logDebug(TAG, "isAdded() returned false (displayNewsItems)")
            return
        }

        if (activity == null) {
            logError(TAG, OxygenUpdaterException("getActivity() returned null (displayNewsItems)"))
            return
        }

        shimmerFrameLayout.isVisible = false

        if (newsItems.isEmpty()) {
            displayEmptyState()
            return
        }

        updateBannerText(newsItems.count { !it.read })

        // respect unread articles filtering
        val itemList = if (isShowingOnlyUnreadArticles) {
            newsItems.filter { !it.read }
        } else {
            newsItems
        }

        newsListAdapter.submitList(itemList)

        // toggle between showing only reading articles and showing all articles
        bannerLayout.setOnClickListener {
            newsListAdapter.submitList(
                if (isShowingOnlyUnreadArticles) newsItems
                else newsItems.filter { !it.read }
            )

            isShowingOnlyUnreadArticles = !isShowingOnlyUnreadArticles
        }
    }

    private fun markAllRead() {
        newsListAdapter.currentList.filter {
            !it.read
        }.forEach { newsItem ->
            if (newsItem.id != null) {
                newsViewModel.toggleReadStatus(newsItem, true)
                newsListAdapter.changeItemReadStatus(newsItem.id, true)
            }
        }

        updateBannerText(0)

        if (isShowingOnlyUnreadArticles) {
            displayEmptyState(true)
        }
    }

    private fun updateBannerText(count: Int) {
        if (!isAdded) {
            return
        }

        bannerLayout.apply {
            isVisible = true

            if (count != 0) {
                setOnLongClickListener { view ->
                    if (isAdded && context != null) {
                        PopupMenu(requireContext(), view).apply {
                            inflate(R.menu.menu_mark_articles_read)
                            show()

                            setOnMenuItemClickListener {
                                markAllRead()
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

        bannerTextView.text = getString(
            if (isShowingOnlyUnreadArticles) {
                R.string.news_unread_count_2
            } else {
                R.string.news_unread_count_1
            },
            count
        )

        // display badge with the number of unread news articles
        // if there aren't any unread articles, the badge is hidden
        (activity as MainActivity?)?.updateTabBadge(R.id.page_news, count != 0, count)
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
