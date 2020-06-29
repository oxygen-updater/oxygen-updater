package com.oxygenupdater.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.oxygenupdater.R
import com.oxygenupdater.activities.NewsActivity
import com.oxygenupdater.adapters.NewsAdapter.NewsItemViewHolder
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.models.NewsItem

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class NewsAdapter(
    private val context: Context?,
    private var newsItemList: List<NewsItem>,
    newsItemReadListener: KotlinCallback<Long>
) : RecyclerView.Adapter<NewsItemViewHolder>() {

    val itemList: List<NewsItem>
        get() = newsItemList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsItemViewHolder = NewsItemViewHolder(
        LayoutInflater.from(context).inflate(R.layout.news_item, parent, false)
    )

    override fun onBindViewHolder(holder: NewsItemViewHolder, position: Int) {
        // Logic to set the title, subtitle and image of each individual news item.
        val newsItem = newsItemList[position]

        holder.title.text = newsItem.title
        holder.subtitle.text = newsItem.subtitle
        holder.container.setOnClickListener { openNewsItem(newsItem) }

        if (newsItem.read) {
            holder.title.alpha = 0.5f
            holder.subtitle.alpha = 0.5f
        } else {
            // Needs to be explicitly set because RecyclerView re-uses layouts,
            // which causes unread articles to also sometimes have the same alpha as a read article
            holder.title.alpha = 1f
            holder.subtitle.alpha = 1f
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
        val intent = Intent(context, NewsActivity::class.java)
            .putExtra(NewsActivity.INTENT_NEWS_ITEM_ID, newsItem.id)

        context!!.startActivity(intent)
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
        var newsItemReadListener: KotlinCallback<Long>? = null
    }

    init {
        Companion.newsItemReadListener = newsItemReadListener
    }
}
