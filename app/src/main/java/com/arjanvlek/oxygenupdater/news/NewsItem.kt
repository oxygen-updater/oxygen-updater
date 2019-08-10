package com.arjanvlek.oxygenupdater.news

import com.arjanvlek.oxygenupdater.internal.i18n.Locale
import com.arjanvlek.oxygenupdater.internal.i18n.Locale.NL
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import lombok.Getter
import lombok.Setter
import java.io.Serializable

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class NewsItem : Serializable {
    var id: Long? = null
        set(id) {
            field = this.id
        }

    @JsonProperty("dutch_title")
    var dutchTitle: String? = null
        set(dutchTitle) {
            field = this.dutchTitle
        }

    @JsonProperty("english_title")
    var englishTitle: String? = null
        set(englishTitle) {
            field = this.englishTitle
        }

    @JsonProperty("dutch_subtitle")
    var dutchSubtitle: String? = null
        set(dutchSubtitle) {
            field = this.dutchSubtitle
        }

    @JsonProperty("english_subtitle")
    var englishSubtitle: String? = null
        set(englishSubtitle) {
            field = this.englishSubtitle
        }

    @JsonProperty("image_url")
    var imageUrl: String? = null
        set(imageUrl) {
            field = this.imageUrl
        }

    @JsonProperty("dutch_text")
    var dutchText: String? = null
        set(dutchText) {
            field = this.dutchText
        }

    @JsonProperty("english_text")
    var englishText: String? = null
        set(englishText) {
            field = this.englishText
        }

    @JsonProperty("date_published")
    var datePublished: String? = null
        set(datePublished) {
            field = this.datePublished
        }

    @JsonProperty("date_last_edited")
    var dateLastEdited: String? = null
        set(dateLastEdited) {
            field = this.dateLastEdited
        }

    @JsonProperty("author_name")
    var authorName: String? = null
        set(authorName) {
            field = this.authorName
        }

    @JsonIgnore
    var isRead: Boolean = false
        set(read) {
            field = isRead
        }

    val isFullyLoaded: Boolean
        get() = this.id != null && this.dutchTitle != null && this.englishTitle != null && this.dutchText != null && this.englishText != null

    /* Custom methods */

    fun getTitle(locale: Locale): String? {
        return if (locale == NL)
            dutchTitle
        else
            englishTitle
    }

    fun getSubtitle(locale: Locale): String? {
        return if (locale == NL)
            dutchSubtitle
        else
            englishSubtitle
    }

    fun getText(locale: Locale): String? {
        return if (locale == NL)
            dutchText
        else
            englishText
    }

    companion object {
        private const val serialVersionUID = 6270363342908901533L
    }
}
