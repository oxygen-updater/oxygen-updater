package com.arjanvlek.oxygenupdater.updateinformation;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

public class ServerMessage implements Banner {
	private long id;
	private String englishMessage;
	private String dutchMessage;
	private Long deviceId;
	private Long updateMethodId;
	private ServerMessagePriority priority;

	@Override
	public String getBannerText(Context ignored) {
		return Locale.getDefault()
				.getDisplayLanguage()
				.equals(ApplicationData.LOCALE_DUTCH) ? getDutchMessage() : getEnglishMessage();
	}

	@Override
	public int getColor(Context context) {
		switch (getPriority()) {
			case LOW:
				return ContextCompat.getColor(context, R.color.holo_green_light);
			case MEDIUM:
				return ContextCompat.getColor(context, R.color.holo_orange_light);
			case HIGH:
				return ContextCompat.getColor(context, R.color.holo_red_light);
			default:
				return ContextCompat.getColor(context, R.color.holo_green_light);
		}
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getEnglishMessage() {
		return englishMessage;
	}

	@JsonProperty("english_message")
	public void setEnglishMessage(String englishMessage) {
		this.englishMessage = englishMessage;
	}

	public String getDutchMessage() {
		return dutchMessage;
	}

	@JsonProperty("dutch_message")
	public void setDutchMessage(String dutchMessage) {
		this.dutchMessage = dutchMessage;
	}

	public long getDeviceId() {
		return deviceId;
	}

	@JsonProperty("device_id")
	public void setDeviceId(Long deviceId) {
		this.deviceId = deviceId;
	}

	public long getUpdateMethodId() {
		return updateMethodId;
	}

	@JsonProperty("update_method_id")
	public void setUpdateMethodId(Long updateMethodId) {
		this.updateMethodId = updateMethodId;
	}

	public ServerMessagePriority getPriority() {
		return priority;
	}

	public void setPriority(ServerMessagePriority priority) {
		this.priority = priority;
	}

	@SuppressWarnings("WeakerAccess")
	public enum ServerMessagePriority {
		LOW,
		MEDIUM,
		HIGH
	}

}


