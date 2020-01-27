package com.arjanvlek.oxygenupdater.views

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.buildAdRequest
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.ExceptionUtils
import com.arjanvlek.oxygenupdater.internal.FunctionalAsyncTask
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.internal.server.NetworkException
import com.arjanvlek.oxygenupdater.internal.server.RedirectingResourceStream
import com.arjanvlek.oxygenupdater.models.AppLocale
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.news.NewsActivity
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.NewsAdapter.NewsItemViewHolder
import com.google.android.gms.ads.AdListener
import org.joda.time.LocalDateTime
import java.net.MalformedURLException

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class NewsAdapter(
    private val context: Context?,
    private val activity: AppCompatActivity?,
    private var newsItemList: List<NewsItem>,
    newsItemReadListener: KotlinCallback<Int>
) : RecyclerView.Adapter<NewsItemViewHolder>() {

    private val settingsManager: SettingsManager = SettingsManager(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsItemViewHolder = NewsItemViewHolder(
        LayoutInflater.from(context).inflate(R.layout.news_item, parent, false)
    )

    override fun onBindViewHolder(holder: NewsItemViewHolder, position: Int) {
        // Logic to set the title, subtitle and image of each individual news item.
        val locale = AppLocale.get()
        val newsItem = newsItemList[position]

        holder.title.text = newsItem.getTitle(locale)
        holder.subtitle.text = newsItem.getSubtitle(locale)
        holder.container.setOnClickListener { openNewsItem(newsItem, position) }

        if (newsItem.read) {
            holder.title.alpha = 0.5f
            holder.subtitle.alpha = 0.7f
        }

        // Obtain the thumbnail image from the server.
        FunctionalAsyncTask<Void?, Void, Bitmap?>({
            holder.image.visibility = INVISIBLE
            holder.imagePlaceholder.visibility = VISIBLE
        }, {
            if (newsItem.id == null) {
                null
            } else {
                var image = imageCache[newsItem.id.toInt()]
                if (image != null) {
                    image
                } else {
                    image = doGetImage(newsItem.imageUrl)
                    imageCache.put(newsItem.id.toInt(), image)
                    image
                }
            }
        }, Callback@{ image: Bitmap? ->
            if (context == null || activity == null) {
                return@Callback
            }

            // If a fragment is not attached, do not crash the entire application but return an empty view.
            try {
                context.resources
            } catch (e: Exception) {
                return@Callback
            }

            if (image == null) {
                val errorImage = ResourcesCompat.getDrawable(context.resources, R.drawable.image, null)
                holder.image.setImageDrawable(errorImage)
            } else {
                holder.image.setImageBitmap(image)
            }

            holder.image.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
            holder.image.visibility = VISIBLE
            holder.imagePlaceholder.visibility = INVISIBLE
        }).execute()
    }

    override fun getItemCount() = newsItemList.size

    /**
     * Update underlying data set using [RecyclerView]'s powerful [DiffUtil] tool
     */
    fun updateList(newList: List<NewsItem>) {
        DiffUtil.calculateDiff(NewsItemDiffUtil(newsItemList, newList)).dispatchUpdatesTo(this)
        newsItemList = newList
    }

    private fun openNewsItem(newsItem: NewsItem, position: Int) {
        if (activity is MainActivity) {
            if (activity.mayShowNewsAd() && Utils.checkNetworkConnection(context)) {
                try {
                    activity.getNewsAd()?.apply {
                        adListener = object : AdListener() {
                            override fun onAdClosed() {
                                super.onAdClosed()
                                doOpenNewsItem(newsItem, position)

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
                doOpenNewsItem(newsItem, position)
            }
        } else {
            // If not attached to main activity or coming from other activity, open the news item.
            doOpenNewsItem(newsItem, position)
        }
    }

    private fun doGetImage(imageUrl: String?, retryCount: Int = 0): Bitmap? {
        return try {
            BitmapFactory.decodeStream(RedirectingResourceStream.getInputStream(imageUrl))
        } catch (e: MalformedURLException) {
            // No retry, because malformed url will never work.
            logError(TAG, NetworkException("Error displaying news image: Invalid image URL <$imageUrl>"))
            null
        } catch (e: Exception) {
            if (retryCount < 5) {
                doGetImage(imageUrl, retryCount + 1)
            } else {
                if (ExceptionUtils.isNetworkError(e)) {
                    logWarning(TAG, NetworkException("Error obtaining news image from <$imageUrl>."))
                } else {
                    logError(TAG, "Error obtaining news image from <$imageUrl>", e)
                }

                null
            }
        }
    }

    private fun doOpenNewsItem(newsItem: NewsItem, position: Int) {
        val intent = Intent(context, NewsActivity::class.java)
            .putExtra(NewsActivity.INTENT_NEWS_ITEM_ID, newsItem.id)
            .putExtra(NewsActivity.INTENT_NEWS_ITEM_POSITION, position)

        context!!.startActivity(intent)

        Handler().postDelayed({ newsItem.read = false }, 2000)
    }

    inner class NewsItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: RelativeLayout = itemView.findViewById(R.id.newsItemContainer)
        val image: ImageView = itemView.findViewById(R.id.newsItemImage)
        val imagePlaceholder: ImageView = itemView.findViewById(R.id.newsItemImagePlaceholder)
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
        private const val TAG = "NewsAdapter"
        private val imageCache = SparseArray<Bitmap?>()
        lateinit var newsItemReadListener: KotlinCallback<Int>
    }

    init {
        Companion.newsItemReadListener = newsItemReadListener
    }
}
