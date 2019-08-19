package com.arjanvlek.oxygenupdater.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMethod {

	private long id;

	@JsonProperty("english_name")
	private String englishName;

	@JsonProperty("dutch_name")
	private String dutchName;

	@JsonIgnore
	private boolean recommended;

	@JsonProperty("recommended_for_rooted_device")
	private boolean recommendedWithRoot;

	@JsonProperty("recommended_for_non_rooted_device")
	private boolean recommendedWithoutRoot;

	@JsonProperty("supports_rooted_device")
	private boolean forRootedDevice;

	public UpdateMethod setRecommended(String recommended) {
		this.recommended = (recommended != null && recommended.equals("1"));
		return this;
	}

	public void setRecommendedWithRoot(String recommendedWithRoot) {
		this.recommendedWithRoot = recommendedWithRoot != null && recommendedWithRoot.equals("1");
	}

	public void setRecommendedWithoutRoot(String recommendedWithoutRoot) {
		this.recommendedWithoutRoot = recommendedWithoutRoot != null && recommendedWithoutRoot.equals("1");
	}

	public void setForRootedDevice(String forRootedDevice) {
		this.forRootedDevice = forRootedDevice != null && forRootedDevice.equals("1");
	}
}
