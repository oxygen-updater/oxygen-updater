package com.oxygenupdater.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.oxygenupdater.R
import com.oxygenupdater.adapters.NewsListAdapter.NewsItemViewHolder
import com.oxygenupdater.dialogs.NewsItemOptionsDialogFragment
import com.oxygenupdater.extensions.startNewsActivity
import com.oxygenupdater.internal.NewsItemReadStatusChangedListener
import com.oxygenupdater.internal.NewsListChangedListener
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.utils.ThemeUtils
import com.oxygenupdater.utils.Utils
import org.threeten.bp.LocalDateTime

/**
 * Takes advantage of [ListAdapter], which is a convenient wrapper that handles
 * computing diffs between lists on a background thread
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class NewsListAdapter(
    private val activity: FragmentActivity,
    private val listChangedListener: NewsListChangedListener,
    itemReadStatusChangedListener: NewsItemReadStatusChangedListener,
) : ListAdapter<NewsItem, NewsItemViewHolder>(DIFF_CALLBACK) {

    private val isNightThemeActive = ThemeUtils.isNightModeActive(activity)
    private val optionsDialog by lazy(LazyThreadSafetyMode.NONE) {
        NewsItemOptionsDialogFragment()
    }

    init {
        // Performance optimization, useful if `notifyDataSetChanged` is called
        setHasStableIds(true)
        Companion.itemReadStatusChangedListener = itemReadStatusChangedListener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = NewsItemViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_news,
            parent,
            false
        )
    )

    override fun onBindViewHolder(
        holder: NewsItemViewHolder,
        position: Int
    ) = holder.bindTo(getItem(position))

    override fun getItemId(position: Int) = getItem(position).id ?: RecyclerView.NO_ID

    override fun onCurrentListChanged(
        previousList: List<NewsItem>,
        currentList: List<NewsItem>
    ) = listChangedListener.invoke(
        currentList.count { !it.read },
        currentList.isEmpty()
    )

    fun changeItemReadStatus(newsItemId: Long, isRead: Boolean) {
        currentList.indexOfFirst { it.id == newsItemId }.also { index ->
            if (index != -1) {
                val previousValueOfRead = currentList[index].read
                currentList[index].read = isRead

                if (previousValueOfRead != isRead) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NewsItem>() {
            override fun areItemsTheSame(
                oldItem: NewsItem,
                newItem: NewsItem
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: NewsItem,
                newItem: NewsItem
            ) = oldItem.read == newItem.read
                    && oldItem.imageUrl == newItem.imageUrl
                    && oldItem.englishTitle == newItem.englishTitle
                    && oldItem.dutchTitle == newItem.dutchTitle
                    && oldItem.englishSubtitle == newItem.englishSubtitle
                    && oldItem.dutchSubtitle == newItem.dutchSubtitle
                    && oldItem.datePublished == newItem.datePublished
                    && oldItem.dateLastEdited == newItem.dateLastEdited
                    && oldItem.authorName == newItem.authorName
        }

        var itemReadStatusChangedListener: NewsItemReadStatusChangedListener? = null
    }

    inner class NewsItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val containerLayout: ConstraintLayout = itemView.findViewById(R.id.newsItemContainer)
        val thumbnailView: ImageView = itemView.findViewById(R.id.newsItemImage)
        val unreadIndicator: ImageView = itemView.findViewById(R.id.newsUnreadIndicator)
        val titleView: TextView = itemView.findViewById(R.id.newsItemTitle)
        val subtitleView: TextView = itemView.findViewById(R.id.newsItemSubtitle)
        val timestampAndAuthorView: TextView = itemView.findViewById(R.id.newsItemTimestampAndAuthor)
        val optionsButton: ImageButton = itemView.findViewById(R.id.newsItemOptions)

        /**
         * Show the dialog fragment only if it hasn't been added already. This
         * can happen if the user clicks in rapid succession, which can cause
         * the `java.lang.IllegalStateException: Fragment already added` error
         */
        private fun showOptionsDialog(item: NewsItem) {
            if (!activity.isFinishing && !optionsDialog.isAdded) {
                optionsDialog.apply {
                    newsItem = item
                }.show(
                    activity.supportFragmentManager,
                    NewsItemOptionsDialogFragment.TAG
                )
            }
        }

        fun bindTo(item: NewsItem) {
            unreadIndicator.isVisible = !item.read

            if (item.read) {
                // Make color approximately equal to `textColorSecondary`
                if (isNightThemeActive) .7f else .54f
            } else {
                // Needs to be explicitly set because RecyclerView re-uses layouts,
                // which causes unread articles to also sometimes have the same alpha as a read article
                1f
            }.let {
                thumbnailView.alpha = it
                titleView.alpha = it
            }

            thumbnailView.transitionName = itemView.context.getString(
                R.string.news_item_transition_name,
                item.id
            )

            containerLayout.apply {
                setOnLongClickListener {
                    showOptionsDialog(item)
                    true
                }

                setOnClickListener {
                    activity.startNewsActivity(item, thumbnailView)
                }
            }

            optionsButton.setOnClickListener {
                showOptionsDialog(item)
            }

            titleView.text = item.title
            subtitleView.apply {
                text = item.subtitle
                // Manually set `maxLines` so that `ellipsize="end"` can work.
                // This is necessary because this TextView's height fills
                // available space, which might result in content getting
                // cut off instead.
                post { maxLines = height / lineHeight }
            }

            timestampAndAuthorView.apply {
                isSelected = true // for enabling marquee effect

                val dateTimePrefix = if (item.dateLastEdited == null && item.datePublished != null) {
                    item.datePublished
                } else {
                    item.dateLastEdited
                }?.let {
                    val userDateTime = LocalDateTime.parse(it.replace(" ", "T"))
                        .atZone(Utils.SERVER_TIME_ZONE)

                    DateUtils.getRelativeTimeSpanString(
                        userDateTime.toInstant().toEpochMilli(),
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL
                    )
                }

                val authorName = item.authorName ?: "Unknown Author"
                text = if (dateTimePrefix != null) {
                    "$dateTimePrefix \u2022 $authorName"
                } else {
                    authorName
                }
            }

            Glide.with(itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.image)
                .error(R.drawable.image)
                .into(thumbnailView)
        }
    }
}
