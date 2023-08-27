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

    val title: String?,
    val subtitle: String?,
    val text: String?,

    @ColumnInfo("image_url")
    val imageUrl: String?,

    @ColumnInfo("date_published")
    val datePublished: String?,

    @ColumnInfo("date_last_edited")
    val dateLastEdited: String?,

    @ColumnInfo("author_name")
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
    @JsonIgnore
    @Ignore
    val readState = mutableStateOf(read)

    @IgnoredOnParcel
    @Ignore
    @JvmField
    val apiUrl = "${BuildConfig.SERVER_DOMAIN + BuildConfig.SERVER_API_BASE}news-content/$id"

    @IgnoredOnParcel
    @Ignore
    @JvmField
    val webUrl = "${BuildConfig.SERVER_DOMAIN}article/$id/"

    @IgnoredOnParcel
    @Ignore
    @JvmField
    val epochMilli = (dateLastEdited ?: datePublished)?.let {
        LocalDateTime.parse(it.replace(" ", "T"))
            .atZone(Utils.SERVER_TIME_ZONE)
            .toInstant().toEpochMilli()
    }

    val isFullyLoaded: Boolean
        get() = id != null && title != null && text != null
}
