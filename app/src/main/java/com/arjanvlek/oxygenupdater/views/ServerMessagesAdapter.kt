package com.arjanvlek.oxygenupdater.views

import android.content.Context
import android.content.res.ColorStateList
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.models.Banner
import com.arjanvlek.oxygenupdater.views.ServerMessagesAdapter.ServerMessageViewHolder

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class ServerMessagesAdapter(
    private val context: Context?,
    private var bannerList: List<Banner>
) : RecyclerView.Adapter<ServerMessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerMessageViewHolder = ServerMessageViewHolder(
        LayoutInflater.from(context).inflate(R.layout.server_message, parent, false)
    )

    override fun onBindViewHolder(holder: ServerMessageViewHolder, position: Int) = holder.setItem(bannerList[position])

    override fun getItemCount() = bannerList.size

    inner class ServerMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val message: TextView = itemView.findViewById(R.id.bannerTextView)

        fun setItem(banner: Banner) {
            banner.getBannerText(context!!).let {
                message.text = it

                if (it is Spanned) {
                    message.movementMethod = LinkMovementMethod.getInstance()
                }
            }

            message.setTextColor(banner.getColor(context))
            message.setCompoundDrawablesRelativeWithIntrinsicBounds(banner.getDrawableRes(context), 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(message, ColorStateList.valueOf(banner.getColor(context)))
        }
    }
}
