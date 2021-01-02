package com.oxygenupdater.adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oxygenupdater.R
import com.oxygenupdater.adapters.BottomSheetItemAdapter.BottomSheetItemViewHolder
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.BottomSheetItem
import com.oxygenupdater.internal.settings.SettingsManager.getPreference

/**
 * Takes advantage of [ListAdapter], which is a convenient wrapper that handles
 * computing diffs between lists on a background thread
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class BottomSheetItemAdapter(
    private val context: Context,
    private val key: String,
    private val secondaryKey: String? = null,
    private val onClickListener: KotlinCallback<BottomSheetItem>
) : ListAdapter<BottomSheetItem, BottomSheetItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = BottomSheetItemViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_bottom_sheet_preference,
            parent,
            false
        )
    )

    override fun onBindViewHolder(
        holder: BottomSheetItemViewHolder,
        position: Int
    ) = holder.bindTo(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BottomSheetItem>() {
            override fun areItemsTheSame(
                oldItem: BottomSheetItem,
                newItem: BottomSheetItem
            ) = oldItem.value == newItem.value
                    && oldItem.secondaryValue == newItem.secondaryValue

            override fun areContentsTheSame(
                oldItem: BottomSheetItem,
                newItem: BottomSheetItem
            ) = oldItem.title == newItem.title
                    && oldItem.subtitle == newItem.subtitle
        }
    }

    inner class BottomSheetItemViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val rootLayout: ConstraintLayout = itemView.findViewById(R.id.dialog_item_layout)
        val checkmarkView: ImageView = itemView.findViewById(R.id.dialog_item_checkmark)
        val titleView: TextView = itemView.findViewById(R.id.dialog_item_title)
        val subtitleView: TextView = itemView.findViewById(R.id.dialog_item_subtitle)

        fun bindTo(item: BottomSheetItem) {
            titleView.apply {
                isVisible = item.title != null
                text = item.title
            }

            subtitleView.apply {
                isVisible = item.subtitle != null
                text = item.subtitle
            }

            rootLayout.setOnClickListener {
                onClickListener.invoke(item)
            }

            val currentValue = getPreference<Any?>(key, null)
            val currentSecondaryValue = getPreference<Any?>(secondaryKey, null)
            val secondaryValue = item.secondaryValue

            // value is mandatory, secondary value is optional
            if (item.value == currentValue || secondaryValue != null && secondaryValue == currentSecondaryValue) {
                // Mark selected
                checkmarkView.isVisible = true
                rootLayout.setBackgroundResource(R.drawable.rounded_overlay)
            } else {
                // Mark unselected
                checkmarkView.isInvisible = true

                TypedValue().apply {
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
                    rootLayout.setBackgroundResource(resourceId)
                }
            }
        }
    }
}
