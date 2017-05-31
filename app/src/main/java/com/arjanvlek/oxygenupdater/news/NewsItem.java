package com.arjanvlek.oxygenupdater.news;

import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.LocalDateTime;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsItem implements Serializable{

    private Long id;

    private String dutchTitle;
    private String englishTitle;

    private String dutchSubtitle;
    private String englishSubtitle;

    private String imageUrl;

    private String dutchText;
    private String englishText;

    private String datePublished;
    private String dateLastEdited;
    private String authorName;

    private boolean read;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDutchTitle() {
        return dutchTitle;
    }

    @JsonProperty("dutch_title")
    public void setDutchTitle(String dutchTitle) {
        this.dutchTitle = dutchTitle;
    }

    public String getEnglishTitle() {
        return englishTitle;
    }

    @JsonProperty("english_title")
    public void setEnglishTitle(String englishTitle) {
        this.englishTitle = englishTitle;
    }

    public String getDutchSubtitle() {
        return dutchSubtitle;
    }

    @JsonProperty("dutch_subtitle")
    public void setDutchSubtitle(String dutchSubtitle) {
        this.dutchSubtitle = dutchSubtitle;
    }

    public String getEnglishSubtitle() {
        return englishSubtitle;
    }

    @JsonProperty("english_subtitle")
    public void setEnglishSubtitle(String englishSubtitle) {
        this.englishSubtitle = englishSubtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @JsonProperty("image_url")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDutchText() {
        return dutchText;
    }

    @JsonProperty("dutch_text")
    public void setDutchText(String dutchText) {
        this.dutchText = dutchText;
    }

    public String getEnglishText() {
        return englishText;
    }

    @JsonProperty("english_text")
    public void setEnglishText(String englishText) {
        this.englishText = englishText;
    }

    public String getDatePublished() {
        return datePublished;
    }

    @JsonProperty("date_published")
    public void setDatePublished(String datePublished) {
        this.datePublished = datePublished;
    }

    public String getDateLastEdited() {
        return dateLastEdited;
    }

    @JsonProperty("date_last_edited")
    public void setDateLastEdited(String dateLastEdited) {
        this.dateLastEdited = dateLastEdited;
    }

    public String getAuthorName() {
        return authorName;
    }

    @JsonProperty("author_name")
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public boolean isRead() {
        return read;
    }

    @JsonIgnore
    public void setRead(boolean read) {
        this.read = read;
    }

    /* Custom methods */

    public String getTitle(Locale locale) {
        if(locale == Locale.NL) return getDutchTitle();
        return getEnglishTitle();
    }

    public String getSubtitle(Locale locale) {
        if(locale == Locale.NL) return getDutchSubtitle();
        return getEnglishSubtitle();
    }

    public String getText(Locale locale) {
        if(locale == Locale.NL) return getDutchText();
        return getEnglishText();
    }
}
