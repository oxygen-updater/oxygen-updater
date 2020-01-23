package com.arjanvlek.oxygenupdater.models

import com.arjanvlek.oxygenupdater.models.AppLocale.NL
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class NewsItem(
    val id: Long?,
    val dutchTitle: String?,
    val englishTitle: String?,
    val dutchSubtitle: String?,
    val englishSubtitle: String?,
    val imageUrl: String?,
    val dutchText: String?,
    val englishText: String?,
    val datePublished: String?,
    val dateLastEdited: String?,
    val authorName: String?,

    @JsonIgnore
    var read: Boolean = false
) : Serializable {

    /* Custom methods */
    fun getTitle(appLocale: AppLocale): String? {
        return if (appLocale == NL) dutchTitle else englishTitle
    }

    fun getSubtitle(appLocale: AppLocale): String? {
        return if (appLocale == NL) dutchSubtitle else englishSubtitle
    }

    fun getText(appLocale: AppLocale): String? {
        return if (appLocale == NL) dutchText else englishText
    }

    val isFullyLoaded: Boolean
        get() = id != null && dutchTitle != null && englishTitle != null && dutchText != null && englishText != null

    companion object {
        private const val serialVersionUID = 6270363342908901533L
    }
}
