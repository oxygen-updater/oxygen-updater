package com.oxygenupdater.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.oxygenupdater.R
import com.oxygenupdater.adapters.AboutButtonAdapter.ButtonViewHolder
import com.oxygenupdater.extensions.openDiscord
import com.oxygenupdater.extensions.openEmail
import com.oxygenupdater.extensions.openGitHub
import com.oxygenupdater.extensions.openPatreon
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.openWebsite
import com.oxygenupdater.extensions.startFaqActivity
import com.oxygenupdater.extensions.startHelpActivity
import com.oxygenupdater.models.AboutButton

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class AboutButtonAdapter(
    private val activity: Activity
) : RecyclerView.Adapter<ButtonViewHolder>() {

    init {
        // Performance optimization, useful if `notifyDataSetChanged` is called
        setHasStableIds(true)
    }

    private val data = arrayOf(
        AboutButton(activity.getString(R.string.help), R.drawable.help),
        AboutButton(activity.getString(R.string.faq_menu_item), R.drawable.faq),
        AboutButton(activity.getString(R.string.about_discord_button_text), R.drawable.discord),
        AboutButton(activity.getString(R.string.about_email_button_text), R.drawable.email),
        AboutButton(activity.getString(R.string.about_github_button_text), R.drawable.github),
        AboutButton(activity.getString(R.string.about_website_button_text), R.drawable.link),
        AboutButton(activity.getString(R.string.about_patreon_button_text), R.drawable.patreon),
        AboutButton(activity.getString(R.string.about_rate_button_text), R.drawable.rate),
    )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = ButtonViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.about_button_item,
            parent,
            false
        )
    )

    override fun onBindViewHolder(
        holder: ButtonViewHolder,
        position: Int
    ) = holder.bindTo(data[position])

    override fun getItemId(position: Int) = data[position].drawableResId.toLong()

    override fun getItemCount() = data.size

    inner class ButtonViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val buttonTextView: AppCompatTextView = itemView.findViewById(R.id.buttonTextView)

        fun bindTo(item: AboutButton) = buttonTextView.run {
            text = item.text
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                item.drawableResId,
                0,
                0,
                0
            )

            setOnClickListener {
                when (item.drawableResId) {
                    R.drawable.help -> activity.startHelpActivity(it)
                    R.drawable.faq -> activity.startFaqActivity(it)
                    R.drawable.discord -> activity.openDiscord()
                    R.drawable.email -> activity.openEmail()
                    R.drawable.github -> activity.openGitHub()
                    R.drawable.link -> activity.openWebsite()
                    R.drawable.patreon -> activity.openPatreon()
                    R.drawable.rate -> activity.openPlayStorePage()
                }
            }
        }
    }
}
