package com.arjanvlek.oxygenupdater.updateinformation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.arjanvlek.oxygenupdater.R

class ServerMessageBar(context: Context?) : LinearLayout(context, null) {
    val view: LinearLayout = LayoutInflater.from(context).inflate(R.layout.server_message_bar, this) as LinearLayout

    val backgroundBar: View
        get() = view.findViewById(R.id.server_message_background_bar)

    val textView: TextView
        get() = view.findViewById<View>(R.id.server_message_text_view) as TextView

}
