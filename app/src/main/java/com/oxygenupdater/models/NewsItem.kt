package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.models.AppLocale.NL
import com.oxygenupdater.utils.Utils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
@Entity(tableName = "news_item")
@JsonIgnoreProperties(ignoreUnknown = true)
@Stable
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
    @Deprecated(
        "Don't read boolean column directly, use MutableState instead",
        ReplaceWith("readState.value"),
    )
    val read: Boolean = false,
) : Parcelable {

    @Suppress("DEPRECATION")
    @IgnoredOnParcel
    @Ignore
    val readState = mutableStateOf(read)

    @IgnoredOnParcel
    @Ignore
    val title = if (AppLocale.get() == NL) dutchTitle else englishTitle

    @IgnoredOnParcel
    @Ignore
    val subtitle = if (AppLocale.get() == NL) dutchSubtitle else englishSubtitle

    @IgnoredOnParcel
    @Ignore
    val text = if (AppLocale.get() == NL) dutchText else englishText

    @IgnoredOnParcel
    @Ignore
    val apiUrl = "${BuildConfig.SERVER_DOMAIN + BuildConfig.SERVER_API_BASE}news-content/$id/" +
            (if (AppLocale.get() == NL) "NL" else "EN") + "/"

    @IgnoredOnParcel
    @Ignore
    val webUrl = "${BuildConfig.SERVER_DOMAIN}article/$id/"

    @IgnoredOnParcel
    @Ignore
    val epochMilli = (dateLastEdited ?: datePublished)?.let {
        LocalDateTime.parse(it.replace(" ", "T"))
            .atZone(Utils.SERVER_TIME_ZONE)
            .toInstant().toEpochMilli()
    }

    val isFullyLoaded: Boolean
        get() = id != null && if (AppLocale.get() == NL) {
            dutchTitle != null && dutchText != null
        } else {
            englishTitle != null && englishText != null
        }
}
