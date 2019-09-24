package com.arjanvlek.oxygenupdater.views

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.ExceptionUtils
import com.arjanvlek.oxygenupdater.internal.FunctionalAsyncTask
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.Worker
import com.arjanvlek.oxygenupdater.internal.i18n.Locale
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.internal.server.NetworkException
import com.arjanvlek.oxygenupdater.internal.server.RedirectingResourceStream
import com.arjanvlek.oxygenupdater.news.NewsActivity
import com.arjanvlek.oxygenupdater.news.NewsActivity.Companion.INTENT_NEWS_ITEM_ID
import com.arjanvlek.oxygenupdater.news.NewsItem
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.NewsAdapter.NewsViewHolder
import com.google.android.gms.ads.AdListener
import java8.util.function.Function
import org.joda.time.LocalDateTime
import java.net.MalformedURLException

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class NewsAdapter(private val context: Context?, private val activity: AppCompatActivity?,
                  private val newsItemList: List<NewsItem>) : Adapter<NewsViewHolder>() {
    private val settingsManager: SettingsManager = SettingsManager(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val inflater = LayoutInflater.from(context)

        return NewsViewHolder(inflater.inflate(R.layout.news_item, parent, false))
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        // Logic to set the title, subtitle and image of each individual news item.
		val locale = Locale.locale

        val newsItem = newsItemList[position]

        holder.apply {
            title.text = newsItem.getTitle(locale)
            subtitle.text = newsItem.getSubtitle(locale)
            container.setOnClickListener { openNewsItem(newsItem) }
        }

        if (newsItem.isRead) {
            holder.apply {
                title.alpha = 0.5f
                subtitle.alpha = 0.7f
            }
        }

        // Obtain the thumbnail image from the server.
        FunctionalAsyncTask<Void, Void, Bitmap>(object : Worker {
            override fun start() {
                holder.apply {
                    image.visibility = View.INVISIBLE
                    imagePlaceholder.visibility = View.VISIBLE
                }
            }
        }, Function {
            if (newsItem.id == null) {
				return@Function null
			}

			var image = imageCache.get(newsItem.id!!.toInt())

			if (image != null) {
				return@Function image
			}

			image = doGetImage(newsItem.imageUrl)
			imageCache.put(newsItem.id!!.toInt(), image)

			return@Function image
        }, java8.util.function.Consumer { image ->
            if ((context == null) or (activity == null)) {
                return@Consumer
            }

            // If a fragment is not attached, do not crash the entire application but return an empty view.
			try {
				context?.resources
			} catch (e: Exception) {
				return@Consumer
			}

            if (image == null) {
				val errorImage = ResourcesCompat.getDrawable(context!!.resources, R.drawable.image,
                        null)
				holder.image.setImageDrawable(errorImage)
			} else {
				holder.image.setImageBitmap(image)
			}

            holder.image.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
            holder.image.visibility = View.VISIBLE

            holder.imagePlaceholder.visibility = View.INVISIBLE
        }).execute()

    }

    override fun getItemCount(): Int {
        return newsItemList.size
    }

    private fun openNewsItem(newsItem: NewsItem) {
        if (activity is MainActivity) {
            val mainActivity = activity as MainActivity?
            if (mainActivity!!.mayShowNewsAd() && Utils.checkNetworkConnection(context)) {
                try {
                    val ad = mainActivity.newsInterstitialAd
                    ad!!.adListener = object : AdListener() {
                        override fun onAdClosed() {
                            super.onAdClosed()
                            doOpenNewsItem(newsItem)
                            ad.loadAd(ApplicationData.buildAdRequest())
                        }
                    }
                    ad.show()

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

    private fun doGetImage(imageUrl: String?, retryCount: Int = 0): Bitmap? {
        try {
            val `in` = imageUrl?.let { RedirectingResourceStream.getInputStream(it) }
            return BitmapFactory.decodeStream(`in`)
        } catch (e: MalformedURLException) {
            // No retry, because malformed url will never work.
            logError(TAG, NetworkException(String.format("Error displaying news image: Invalid image URL <%s>", imageUrl)))
            return null
        } catch (e: Exception) {
            return if (retryCount < 5) {
                doGetImage(imageUrl, retryCount + 1)
            } else {
                if (ExceptionUtils.isNetworkError(e)) {
                    logWarning(TAG, NetworkException(String.format("Error obtaining news image from <%s>.", imageUrl)))
                } else {
                    logError(TAG, String.format("Error obtaining news image from <%s>", imageUrl), e)
                }
                null
            }
        }

    }

    private fun doOpenNewsItem(newsItem: NewsItem) {
        val intent = Intent(context, NewsActivity::class.java)
        intent.putExtra(INTENT_NEWS_ITEM_ID, newsItem.id)
        context!!.startActivity(intent)

        Handler().postDelayed({ newsItem.isRead = true }, 2000)
    }

    inner class NewsViewHolder(itemView: View) : ViewHolder(itemView) {
        val container: RelativeLayout = itemView.findViewById(R.id.newsItemContainer)
        val image: ImageView = itemView.findViewById(R.id.newsItemImage)
        val imagePlaceholder: ImageView = itemView.findViewById(R.id.newsItemImagePlaceholder)
        val title: TextView = itemView.findViewById(R.id.newsItemTitle)
        val subtitle: TextView = itemView.findViewById(R.id.newsItemSubTitle)
    }

    companion object {
        private const val TAG = "NewsAdapter"
        private val imageCache = SparseArray<Bitmap>()
    }
}
