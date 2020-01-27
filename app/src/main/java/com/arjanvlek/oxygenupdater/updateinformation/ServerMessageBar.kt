package com.arjanvlek.oxygenupdater.updateinformation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.arjanvlek.oxygenupdater.R
import kotlinx.android.synthetic.main.server_message_bar.view.*

class ServerMessageBar(context: Context?) : LinearLayout(context, null) {

    val view = LayoutInflater.from(context).inflate(R.layout.server_message_bar, this) as LinearLayout

    val backgroundBar: View = server_message_background_bar

    val textView: TextView = server_message_text_view
}
