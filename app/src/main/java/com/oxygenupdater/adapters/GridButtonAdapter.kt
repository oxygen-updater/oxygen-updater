package com.oxygenupdater.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.oxygenupdater.R
import com.oxygenupdater.adapters.GridButtonAdapter.GridButtonViewHolder
import com.oxygenupdater.internal.GridButtonClickedListener
import com.oxygenupdater.models.GridButton

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
open class GridButtonAdapter(
    private val data: Array<GridButton>
) : RecyclerView.Adapter<GridButtonViewHolder>() {

    protected lateinit var onItemClickListener: GridButtonClickedListener

    /**
     * `setHasStableIds(true)` => Performance optimization, useful if `notifyDataSetChanged` is called
     */
    protected fun init(
        onItemClickListener: GridButtonClickedListener
    ) = setHasStableIds(true).also {
        this.onItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = GridButtonViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_grid_button,
            parent,
            false
        )
    )

    override fun onBindViewHolder(
        holder: GridButtonViewHolder,
        position: Int
    ) = holder.bindTo(data[position])

    override fun getItemId(position: Int) = data[position].drawableResId.toLong()

    override fun getItemCount() = data.size

    inner class GridButtonViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val buttonTextView: AppCompatTextView = itemView.findViewById(R.id.buttonTextView)

        fun bindTo(item: GridButton) = buttonTextView.run {
            text = item.text
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                item.drawableResId,
                0,
                0,
                0
            )

            setOnClickListener {
                onItemClickListener.invoke(it, item)
            }
        }
    }
}
