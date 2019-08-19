package com.arjanvlek.oxygenupdater.updateinformation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.R;

public class ServerMessageBar extends LinearLayout {

	private final LinearLayout view;

	public ServerMessageBar(Context context) {
		super(context, null);
		view = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.server_message_bar, this);
	}

	public LinearLayout getView() {
		return view;
	}

	public View getBackgroundBar() {
		return view.findViewById(R.id.server_message_background_bar);
	}

	public TextView getTextView() {
		return (TextView) view.findViewById(R.id.server_message_text_view);
	}
}
