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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oxygenupdater.R
import com.oxygenupdater.adapters.ChooserOnboardingAdapter.ItemSelectionViewHolder
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.models.SelectableModel

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class ChooserOnboardingAdapter(
    private val context: Context?,
    private val initialSelectedIndex: Int,
    private val onItemSelectedCallback: KotlinCallback<SelectableModel>
) : ListAdapter<SelectableModel, ItemSelectionViewHolder>(DIFF_CALLBACK) {

    private lateinit var selectedItem: SelectableModel
    private val selectedColor = ContextCompat.getColor(context!!, R.color.colorPositive)
    private val unselectedColor = ContextCompat.getColor(context!!, R.color.foreground)

    init {
        // Performance optimization, useful if `notifyDataSetChanged` is called
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = ItemSelectionViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.onboarding_chooser,
            parent,
            false
        )
    )

    override fun onBindViewHolder(
        holder: ItemSelectionViewHolder,
        position: Int
    ) = holder.bindTo(getItem(position))

    override fun getItemId(position: Int) = getItem(position).id

    override fun onCurrentListChanged(
        previousList: List<SelectableModel>,
        currentList: List<SelectableModel>
    ) {
        /**
         * [previousList] will be empty only if it's the first change
         */
        if (previousList.isEmpty()) {
            selectedItem = getItem(
                if (initialSelectedIndex != -1) initialSelectedIndex
                else 0
            )

            // invoke callback for auto-selection
            onItemSelectedCallback.invoke(selectedItem)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SelectableModel>() {
            override fun areItemsTheSame(
                oldItem: SelectableModel,
                newItem: SelectableModel
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: SelectableModel,
                newItem: SelectableModel
            ) = oldItem.name == newItem.name
        }
    }

    inner class ItemSelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val textView: AppCompatTextView = itemView.findViewById(R.id.textView)

        fun bindTo(item: SelectableModel) = textView.let {
            it.text = item.name
            it.setOnClickListener { setSelection(adapterPosition) }

            if (item.id == selectedItem.id) {
                markSelected()
            } else {
                markUnselected()
            }
        }

        private fun setSelection(position: Int) {
            val previousId = selectedItem.id

            selectedItem = getItem(
                if (position != -1) position
                else 0
            )

            // invoke callback if selection has changed
            if (previousId != selectedItem.id) {
                onItemSelectedCallback.invoke(selectedItem)

                // update UI for previous selection
                notifyItemChanged(currentList.indexOfFirst { it.id == previousId })

                // update UI for current selection
                notifyItemChanged(position)
            }
        }

        private fun markSelected() = textView.let {
            TextViewCompat.setCompoundDrawableTintList(it, ColorStateList.valueOf(selectedColor))
            it.setTextColor(selectedColor)
            it.setBackgroundResource(R.drawable.rounded_overlay)
        }

        private fun markUnselected() = textView.let {
            TextViewCompat.setCompoundDrawableTintList(it, ColorStateList.valueOf(0))
            it.setTextColor(unselectedColor)

            TypedValue().let { outValue ->
                context!!.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                it.setBackgroundResource(outValue.resourceId)
            }
        }
    }
}
