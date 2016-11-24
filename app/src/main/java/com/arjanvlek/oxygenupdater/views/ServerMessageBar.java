package com.arjanvlek.oxygenupdater.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.R;

public class ServerMessageBar extends RelativeLayout {

    private final RelativeLayout view;

    public ServerMessageBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.view = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.server_message_bar, this);
    }

    public RelativeLayout getView() {
        return view;
    }

    public View getBackgroundBar() {
        return view.findViewById(R.id.server_message_background_bar);
    }

    public TextView getTextView() {
        return (TextView) view.findViewById(R.id.server_message_text_view);
    }
}
