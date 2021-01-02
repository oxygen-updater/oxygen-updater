package com.oxygenupdater.adapters

import android.content.res.ColorStateList
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oxygenupdater.R
import com.oxygenupdater.adapters.ServerMessagesAdapter.ServerMessageViewHolder
import com.oxygenupdater.models.Banner
import com.oxygenupdater.models.ServerMessage

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class ServerMessagesAdapter : ListAdapter<ServerMessage, ServerMessageViewHolder>(DIFF_CALLBACK) {

    init {
        // Performance optimization, useful if `notifyDataSetChanged` is called
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = ServerMessageViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_server_message,
            parent,
            false
        )
    )

    override fun onBindViewHolder(
        holder: ServerMessageViewHolder,
        position: Int
    ) = holder.bindTo(getItem(position))

    override fun getItemId(position: Int) = getItem(position).id

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ServerMessage>() {
            override fun areItemsTheSame(
                oldItem: ServerMessage,
                newItem: ServerMessage
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ServerMessage,
                newItem: ServerMessage
            ) = oldItem.englishMessage == newItem.englishMessage
                    && oldItem.dutchMessage == newItem.dutchMessage
                    && oldItem.priority == newItem.priority
        }
    }

    inner class ServerMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val message: TextView = itemView.findViewById(R.id.bannerTextView)

        fun bindTo(banner: Banner) = message.run {
            banner.getBannerText(context).let {
                text = it

                if (it is Spanned) {
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }

            val color = banner.getColor(context)

            setTextColor(color)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                banner.getDrawableRes(context),
                0,
                0,
                0
            )
            TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(color))
        }
    }
}
