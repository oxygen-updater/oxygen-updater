package com.arjanvlek.oxygenupdater.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateMethod {

    private long id;
    private String englishName;
    private String dutchName;
    private boolean recommended;

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

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(String recommended) {
        this.recommended = (recommended != null && recommended.equals("1"));
    }
}
