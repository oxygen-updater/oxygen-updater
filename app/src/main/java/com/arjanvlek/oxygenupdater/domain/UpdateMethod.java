package com.arjanvlek.oxygenupdater.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateMethod {

    private long id;
    private String englishName;
    private String dutchName;

    @JsonIgnore
    private boolean recommended;
    private boolean recommendedWithRoot;
    private boolean recommendedWithoutRoot;
    private boolean forRootedDevice;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @JsonProperty("english_name")
    public String getEnglishName() {
        return englishName;
    }

    @JsonProperty("english_name")
    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    @JsonProperty("dutch_name")
    public String getDutchName() {
        return dutchName;
    }

    @JsonProperty("dutch_name")
    public void setDutchName(String dutchName) {
        this.dutchName = dutchName;
    }

    @JsonIgnore
    public boolean isRecommended() {
        return recommended;
    }

    @JsonIgnore
    public UpdateMethod setRecommended(String recommended) {
        this.recommended = (recommended != null && recommended.equals("1"));
        return this;
    }

    @JsonProperty("recommended_for_rooted_device")
    public boolean isRecommendedWithRoot() {
        return recommendedWithRoot;
    }

    @JsonProperty("recommended_for_rooted_device")
    public void setRecommendedWithRoot(String recommendedWithRoot) {
        this.recommendedWithRoot = recommendedWithRoot != null && recommendedWithRoot.equals("1");
    }

    @JsonProperty("recommended_for_non_rooted_device")
    public boolean isRecommendedWithoutRoot() {
        return recommendedWithoutRoot;
    }

    @JsonProperty("recommended_for_non_rooted_device")
    public void setRecommendedWithoutRoot(String recommendedWithoutRoot) {
        this.recommendedWithoutRoot = recommendedWithoutRoot != null && recommendedWithoutRoot.equals("1");
    }

    @JsonProperty("supports_rooted_device")
    public boolean isForRootedDevice() {
        return forRootedDevice;
    }

    @JsonProperty("supports_rooted_device")
    public void setForRootedDevice(String forRootedDevice) {
        this.forRootedDevice = forRootedDevice != null && forRootedDevice.equals("1");
    }
}
