package com.arjanvlek.oxygenupdater.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import static java.lang.Long.parseLong;

public class LongListPreference extends ListPreference {

	@SuppressWarnings("unused")
	public LongListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@SuppressWarnings("unused")
	public LongListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@SuppressWarnings("unused")
	public LongListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@SuppressWarnings("unused")
	public LongListPreference(Context context) {
		super(context);
	}

	@Override
	protected boolean persistString(String value) {
		long longValue = parseLong(value);
		return persistLong(longValue);
	}

	@Override
	protected String getPersistedString(String defaultReturnValue) {
		long longValue;

		if (defaultReturnValue != null) {
			long intDefaultReturnValue = parseLong(defaultReturnValue);
			longValue = getPersistedLong(intDefaultReturnValue);
		} else {
			// We haven't been given a default return value, but we need to specify one when retrieving the value

			if (getPersistedLong(0) == getPersistedLong(1)) {
				// The default value is being ignored, so we're good to go
				longValue = getPersistedLong(0);
			} else {
				throw new IllegalArgumentException("Cannot get an int without a default return value");
			}
		}

		return Long.toString(longValue);
	}

}
