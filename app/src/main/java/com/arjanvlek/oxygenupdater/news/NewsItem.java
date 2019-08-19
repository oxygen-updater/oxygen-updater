package com.arjanvlek.oxygenupdater.news;

import com.arjanvlek.oxygenupdater.internal.i18n.Locale;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

import static com.arjanvlek.oxygenupdater.internal.i18n.Locale.NL;

@Getter
@Setter
@SuppressWarnings("WeakerAccess")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsItem implements Serializable {

	private static final long serialVersionUID = 6270363342908901533L;
	private Long id;

	@JsonProperty("dutch_title")
	private String dutchTitle;

	@JsonProperty("english_title")
	private String englishTitle;

	@JsonProperty("dutch_subtitle")
	private String dutchSubtitle;

	@JsonProperty("english_subtitle")
	private String englishSubtitle;

	@JsonProperty("image_url")
	private String imageUrl;

	@JsonProperty("dutch_text")
	private String dutchText;

	@JsonProperty("english_text")
	private String englishText;

	@JsonProperty("date_published")
	private String datePublished;

	@JsonProperty("date_last_edited")
	private String dateLastEdited;

	@JsonProperty("author_name")
	private String authorName;

	@JsonIgnore
	private boolean read;

	/* Custom methods */

	public String getTitle(Locale locale) {
		return locale == NL
				? getDutchTitle()
				: getEnglishTitle();
	}

	public String getSubtitle(Locale locale) {
		return locale == NL
				? getDutchSubtitle()
				: getEnglishSubtitle();
	}

	public String getText(Locale locale) {
		return locale == NL
				? getDutchText()
				: getEnglishText();
	}

	public boolean isFullyLoaded() {
		return id != null && dutchTitle != null && englishTitle != null && dutchText != null && englishText != null;
	}
}
