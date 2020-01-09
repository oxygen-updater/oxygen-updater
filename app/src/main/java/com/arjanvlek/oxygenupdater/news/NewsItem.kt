package com.arjanvlek.oxygenupdater.news

import com.arjanvlek.oxygenupdater.internal.i18n.Locale
import com.arjanvlek.oxygenupdater.internal.i18n.Locale.NL
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class NewsItem(
    @JsonProperty("id")
    val id: Long?,

    // @JsonProperty("dutch_title")
    val dutchTitle: String?,

    // @JsonProperty("english_title")
    val englishTitle: String?,

    // @JsonProperty("dutch_subtitle")
    val dutchSubtitle: String?,

    // @JsonProperty("english_subtitle")
    val englishSubtitle: String?,

    // @JsonProperty("image_url")
    val imageUrl: String?,

    // @JsonProperty("dutch_text")
    val dutchText: String?,

    // @JsonProperty("english_text")
    val englishText: String?,

    // @JsonProperty("date_published")
    val datePublished: String?,

    // @JsonProperty("date_last_edited")
    val dateLastEdited: String?,

    // @JsonProperty("author_name")
    val authorName: String?,

    @JsonIgnore
    var read: Boolean = false
) : Serializable {

    /* Custom methods */
    fun getTitle(locale: Locale): String? {
        return if (locale == NL) dutchTitle else englishTitle
    }

    fun getSubtitle(locale: Locale): String? {
        return if (locale == NL) dutchSubtitle else englishSubtitle
    }

    fun getText(locale: Locale): String? {
        return if (locale == NL) dutchText else englishText
    }

    val isFullyLoaded: Boolean
        get() = id != null && dutchTitle != null && englishTitle != null && dutchText != null && englishText != null

    companion object {
        private const val serialVersionUID = 6270363342908901533L
    }
}
