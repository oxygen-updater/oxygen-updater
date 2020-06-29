package com.oxygenupdater.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.oxygenupdater.R
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.models.SelectableModel
import com.oxygenupdater.utils.ThemeUtils

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class ChooserOnboardingAdapter(
    private val context: Context?,
    private val data: List<SelectableModel>,
    initialSelectedIndex: Int,
    private val onItemSelectedCallback: KotlinCallback<SelectableModel>
) : RecyclerView.Adapter<ChooserOnboardingAdapter.ItemSelectionViewHolder>() {

    private var selectedItem: SelectableModel

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemSelectionViewHolder = ItemSelectionViewHolder(
        LayoutInflater.from(context).inflate(R.layout.onboarding_chooser, parent, false)
    )

    override fun onBindViewHolder(holder: ItemSelectionViewHolder, position: Int) = holder.setItem(data[position])

    override fun getItemCount() = data.size

    fun setSelection(position: Int) {
        val previousId = selectedItem.id

        selectedItem = if (position != -1) {
            data[position]
        } else {
            data[0]
        }

        // invoke callback if selection has changed
        if (previousId != selectedItem.id) {
            onItemSelectedCallback.invoke(selectedItem)

            // update UI for previous selection
            notifyItemChanged(data.indexOfFirst { it.id == previousId })

            // update UI for current selection
            notifyItemChanged(position)
        }
    }

    inner class ItemSelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val textView: AppCompatTextView = itemView.findViewById(R.id.textView)

        fun setItem(item: SelectableModel) = textView.let {
            it.text = item.name
            it.setOnClickListener { setSelection(adapterPosition) }

            if (item.id == selectedItem.id) {
                markSelected()
            } else {
                markUnselected()
            }
        }

        private fun markSelected() = textView.let {
            val selectedColor = ContextCompat.getColor(context!!, R.color.colorPositive)

            TextViewCompat.setCompoundDrawableTintList(it, ColorStateList.valueOf(selectedColor))
            it.setTextColor(selectedColor)
            it.setBackgroundResource(R.drawable.rounded_overlay)
        }

        private fun markUnselected() = textView.let {
            TextViewCompat.setCompoundDrawableTintList(it, ColorStateList.valueOf(0))
            it.setTextColor(ThemeUtils.getTextColorTertiary(context!!))

            TypedValue().let { outValue ->
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                it.setBackgroundResource(outValue.resourceId)
            }
        }
    }

    init {
        selectedItem = if (initialSelectedIndex != -1) {
            data[initialSelectedIndex]
        } else {
            data[0]
        }

        // invoke callback for auto-selection
        onItemSelectedCallback.invoke(selectedItem)
    }
}
