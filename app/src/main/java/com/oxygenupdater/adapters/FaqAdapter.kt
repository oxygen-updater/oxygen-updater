package com.oxygenupdater.adapters

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oxygenupdater.R
import com.oxygenupdater.adapters.FaqAdapter.FaqCategoryViewHolder
import com.oxygenupdater.models.InAppFaq

/**
 * Takes advantage of [ListAdapter], which is a convenient wrapper that handles
 * computing diffs between lists on a background thread
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class FaqAdapter : ListAdapter<InAppFaq, FaqCategoryViewHolder>(DIFF_CALLBACK) {

    init {
        // Performance optimization, useful if `notifyDataSetChanged` is called
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = FaqCategoryViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_faq,
            parent,
            false
        ),
        viewType
    )

    override fun onBindViewHolder(
        holder: FaqCategoryViewHolder,
        position: Int
    ) = holder.bindTo(getItem(position), position)

    override fun getItemViewType(
        position: Int
    ) = when (getItem(position).type) {
        "category" -> 0
        else -> 1
    }

    override fun getItemId(position: Int) = getItem(position).run {
        // Since the server flattens categories and items into a single JSON
        // array, we need to avoid `id` collisions. 10000 should be enough.
        id + if (type == "category") 10000 else 1
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<InAppFaq>() {
            override fun areItemsTheSame(
                oldItem: InAppFaq,
                newItem: InAppFaq
            ) = oldItem.id == newItem.id && oldItem.type == newItem.type

            override fun areContentsTheSame(
                oldItem: InAppFaq,
                newItem: InAppFaq
            ) = oldItem.englishTitle == newItem.englishTitle
                    && oldItem.dutchTitle == newItem.dutchTitle
                    && oldItem.frenchTitle == newItem.frenchTitle
                    && oldItem.englishBody == newItem.englishBody
                    && oldItem.dutchBody == newItem.dutchBody
                    && oldItem.frenchBody == newItem.frenchBody
        }
    }

    inner class FaqCategoryViewHolder(
        itemView: View,
        private val viewType: Int
    ) : RecyclerView.ViewHolder(itemView) {
        val categoryTitleView: AppCompatTextView = itemView.findViewById(R.id.faqCategoryTitle)
        val itemTitleView: AppCompatTextView = itemView.findViewById(R.id.faqItemTitle)
        val itemBodyView: AppCompatTextView = itemView.findViewById(R.id.faqItemBody)
        val dividerView: View = itemView.findViewById(R.id.divider)

        fun bindTo(item: InAppFaq, position: Int) {
            val isCategory = viewType == 0

            categoryTitleView.isVisible = isCategory
            itemTitleView.isVisible = !isCategory
            itemBodyView.isVisible = !isCategory && item.expanded
            dividerView.isVisible = !isCategory

            if (isCategory) {
                categoryTitleView.text = item.title
            } else {
                itemTitleView.apply {
                    text = item.title
                    setCompoundDrawablesRelativeWithIntrinsicBounds(
                        if (item.expanded) {
                            R.drawable.expand
                        } else {
                            R.drawable.collapse
                        }, 0, 0, 0
                    )

                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            if (item.important) {
                                R.color.colorPrimary
                            } else {
                                R.color.foreground
                            }
                        )
                    )

                    setOnClickListener {
                        item.expanded = !item.expanded

                        // Let RecyclerView handle redrawing this item,
                        // as it comes with built-in animations as well
                        notifyItemChanged(position)
                    }
                }

                itemBodyView.apply {
                    text = HtmlCompat.fromHtml(
                        item.body?.replace("\n", "<br>") ?: "",
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    ).also {
                        // Make the links clickable
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
        }
    }
}
