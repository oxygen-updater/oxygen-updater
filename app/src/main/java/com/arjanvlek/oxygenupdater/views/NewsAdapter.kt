package com.arjanvlek.oxygenupdater.views

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.buildAdRequest
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.makeSceneTransitionAnimationBundle
import com.arjanvlek.oxygenupdater.models.AppLocale
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.news.NewsActivity
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.NewsAdapter.NewsItemViewHolder
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import org.joda.time.LocalDateTime

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class NewsAdapter(
    private val context: Context?,
    private val activity: AppCompatActivity?,
    private var newsItemList: List<NewsItem>,
    newsItemReadListener: KotlinCallback<Long>
) : RecyclerView.Adapter<NewsItemViewHolder>() {

    private val settingsManager: SettingsManager = SettingsManager(context)

    val itemList: List<NewsItem>
        get() = newsItemList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsItemViewHolder = NewsItemViewHolder(
        LayoutInflater.from(context).inflate(R.layout.news_item, parent, false)
    )

    override fun onBindViewHolder(holder: NewsItemViewHolder, position: Int) {
        // Logic to set the title, subtitle and image of each individual news item.
        val locale = AppLocale.get()
        val newsItem = newsItemList[position]

        holder.title.text = newsItem.getTitle(locale)
        holder.subtitle.text = newsItem.getSubtitle(locale)
        holder.container.setOnClickListener { openNewsItem(newsItem) }

        if (newsItem.read) {
            holder.title.alpha = 0.5f
            holder.subtitle.alpha = 0.7f
        }

        Glide.with(holder.itemView.context)
            .load(newsItem.imageUrl)
            .placeholder(R.drawable.image)
            .error(R.drawable.image)
            .into(holder.image)
    }

    override fun getItemCount() = newsItemList.size

    /**
     * Update underlying data set using [RecyclerView]'s powerful [DiffUtil] tool
     */
    fun updateList(newList: List<NewsItem>) {
        DiffUtil.calculateDiff(NewsItemDiffUtil(newsItemList, newList)).dispatchUpdatesTo(this)
        newsItemList = newList
    }

    fun markItemAsRead(newsItemId: Long) {
        newsItemList.indexOfFirst { it.id == newsItemId }.also { index ->
            if (index != -1) {
                val previousValueOfRead = newsItemList[index].read
                newsItemList[index].read = true

                if (!previousValueOfRead) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    private fun openNewsItem(newsItem: NewsItem) {
        if (activity is MainActivity) {
            if (activity.mayShowNewsAd() && Utils.checkNetworkConnection(context)) {
                try {
                    activity.getNewsAd()?.apply {
                        adListener = object : AdListener() {
                            override fun onAdClosed() {
                                super.onAdClosed()
                                doOpenNewsItem(newsItem)

                                loadAd(buildAdRequest())
                            }
                        }

                        show()
                    }

                    // Store the last date when the ad was shown. Used to limit the ads to one per 5 minutes.
                    settingsManager.savePreference(SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN, LocalDateTime.now().toString())
                } catch (e: NullPointerException) {
                    // Ad is not loaded, because the user bought the ad-free upgrade. Nothing to do here...
                }
            } else {
                // If offline, too many ads are shown or the user has bought the ad-free upgrade, open the news item directly.
                doOpenNewsItem(newsItem)
            }
        } else {
            // If not attached to main activity or coming from other activity, open the news item.
            doOpenNewsItem(newsItem)
        }
    }

    private fun doOpenNewsItem(newsItem: NewsItem) {
        val intent = Intent(context, NewsActivity::class.java)
            .putExtra(NewsActivity.INTENT_NEWS_ITEM_ID, newsItem.id)

        context!!.startActivity(intent, activity!!.makeSceneTransitionAnimationBundle())
    }

    inner class NewsItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: ConstraintLayout = itemView.findViewById(R.id.newsItemContainer)
        val image: ImageView = itemView.findViewById(R.id.newsItemImage)
        val title: TextView = itemView.findViewById(R.id.newsItemTitle)
        val subtitle: TextView = itemView.findViewById(R.id.newsItemSubTitle)
    }

    private inner class NewsItemDiffUtil(
        private val oldList: List<NewsItem>,
        private val newList: List<NewsItem>
    ) : DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition] == newList[newItemPosition]
    }

    companion object {
        lateinit var newsItemReadListener: KotlinCallback<Long>
    }

    init {
        Companion.newsItemReadListener = newsItemReadListener
    }
}
