package com.arjanvlek.oxygenupdater.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.UpdateMethod;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.i18n.Locale;

import java.util.List;

public class CustomDropdown {

	public static View initCustomDeviceDropdown(int currentPosition, View convertView, ViewGroup parent, @LayoutRes int layoutType, List<Device> devices, int recommendedPosition, Context context) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) Utils.getSystemService(context, Context.LAYOUT_INFLATER_SERVICE);
			if (inflater == null) {
				return new View(context);
			}

			// Create either the selected item or the dropdown menu.
			convertView = inflater.inflate(layoutType, parent, false);
		}

		// Set the text of the item to the device's name
		TextView textView = convertView.findViewById(android.R.id.text1);
		textView.setText(devices.get(currentPosition).getName());

		// If this device is the recommended device, change the text color to green
		// Otherwise, set it to the default foreground
		if (recommendedPosition != -1) {
			if (currentPosition == recommendedPosition) {
				textView.setTextColor(ContextCompat.getColor(context, R.color.colorPositive));
			} else {
				textView.setTextColor(ContextCompat.getColor(context, R.color.foreground));
			}
		} else {
			textView.setTextColor(ContextCompat.getColor(context, R.color.foreground));
		}

		return convertView;
	}

	public static View initCustomUpdateMethodDropdown(int currentPosition, View convertView, ViewGroup parent, @LayoutRes int layoutType, List<UpdateMethod> updateMethods, int[] recommendedPositions, Context context) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) Utils.getSystemService(context, Context.LAYOUT_INFLATER_SERVICE);
			if (inflater == null) {
				return new View(context);
			}

			convertView = inflater.inflate(layoutType, parent, false);
		}

		TextView textView = convertView.findViewById(android.R.id.text1);

		// Set the view to the update method's name, according to the app language.
		String updateMethodName = Locale.getLocale() == Locale.NL
				? updateMethods.get(currentPosition).getDutchName()
				: updateMethods.get(currentPosition).getEnglishName();

		textView.setText(updateMethodName);

		textView.setTextColor(ContextCompat.getColor(context, R.color.foreground));

		// If this is a recommended update method, make the text green
		if (recommendedPositions != null) {
			for (Integer recommendedPosition : recommendedPositions) {
				if (currentPosition == recommendedPosition) {
					textView.setTextColor(ContextCompat.getColor(context, R.color.colorPositive));
				}
			}
		}

		return convertView;
	}
}
