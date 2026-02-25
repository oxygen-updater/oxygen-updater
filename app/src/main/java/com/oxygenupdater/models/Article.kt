package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.utils.ApiBaseUrl
import com.oxygenupdater.utils.Utils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames
import java.time.LocalDateTime

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Parcelize
@Stable
@Entity(tableName = "news_item")
data class Article(
    @PrimaryKey
    val id: Long? = 0,

    val title: String? = null,
    val subtitle: String? = null,
    val text: String? = null,

    @ColumnInfo("image_url")
    @JsonNames("image_url")
    val imageUrl: String? = null,

    @ColumnInfo("date_published")
    @JsonNames("date_published")
    val datePublished: String? = null,

    @ColumnInfo("date_last_edited")
    @JsonNames("date_last_edited")
    val dateLastEdited: String? = null,

    @ColumnInfo("author_name")
    @JsonNames("author_name")
    val authorName: String? = null,

    @ColumnInfo(defaultValue = "0")
    @Transient
    @Deprecated(
        "Don't read boolean column directly, use MutableState instead",
        ReplaceWith("readState"),
    )
    val read: Boolean = false,
) : Parcelable {

    @Suppress("DEPRECATION")
    @IgnoredOnParcel
    @get:Ignore
    var readState by mutableStateOf(read)

    @IgnoredOnParcel
    @Ignore
    @JvmField
    val apiUrl = "${ApiBaseUrl}news-content/$id"

    @IgnoredOnParcel
    @Ignore
    @JvmField
    val webUrl = "${BuildConfig.SERVER_DOMAIN}article/$id/"

    @IgnoredOnParcel
    @Ignore
    @JvmField
    val epochMilli = (dateLastEdited ?: datePublished)?.let {
        LocalDateTime.parse(it.replace(" ", "T"))
            .atZone(Utils.ServerTimeZone)
            .toInstant().toEpochMilli()
    }

    val isFullyLoaded: Boolean
        get() = id != null && title != null && text != null
}
