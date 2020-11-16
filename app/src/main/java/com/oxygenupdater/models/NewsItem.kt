package com.oxygenupdater.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.models.AppLocale.NL
import java.io.Serializable

@Entity(tableName = "news_item")
@JsonIgnoreProperties(ignoreUnknown = true)
data class NewsItem(
    @PrimaryKey
    val id: Long?,

    @ColumnInfo(name = "dutch_title")
    val dutchTitle: String?,

    @ColumnInfo(name = "english_title")
    val englishTitle: String?,

    @ColumnInfo(name = "dutch_subtitle")
    val dutchSubtitle: String?,

    @ColumnInfo(name = "english_subtitle")
    val englishSubtitle: String?,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?,

    @ColumnInfo(name = "dutch_text")
    val dutchText: String?,

    @ColumnInfo(name = "english_text")
    val englishText: String?,

    @ColumnInfo(name = "date_published")
    val datePublished: String?,

    @ColumnInfo(name = "date_last_edited")
    val dateLastEdited: String?,

    @ColumnInfo(name = "author_name")
    val authorName: String?,

    @ColumnInfo(defaultValue = "0")
    @JsonIgnore
    var read: Boolean = false
) : Serializable {

    @Ignore
    val title = if (AppLocale.get() == NL) dutchTitle else englishTitle

    @Ignore
    val subtitle = if (AppLocale.get() == NL) dutchSubtitle else englishSubtitle

    @Ignore
    val text = if (AppLocale.get() == NL) dutchText else englishText

    @Ignore
    val url = "${BuildConfig.SERVER_BASE_URL}news-content/$id/" +
            (if (AppLocale.get() == NL) "NL" else "EN") + "/"

    val isFullyLoaded: Boolean
        get() = id != null && dutchTitle != null && englishTitle != null && dutchText != null && englishText != null

    companion object {
        private const val serialVersionUID = 6270363342908901533L
    }
}
