package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.mutableStateOf
import com.oxygenupdater.models.AppLocale.FR
import com.oxygenupdater.models.AppLocale.NL
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class InAppFaq(
    val id: Long,

    val englishTitle: String?,
    val dutchTitle: String?,
    val frenchTitle: String?,

    val englishBody: String?,
    val dutchBody: String?,
    val frenchBody: String?,

    val important: Boolean = false,

    /**
     * Either `category` or `item`
     */
    val type: String,
) : Parcelable {

    @IgnoredOnParcel
    val title = when (AppLocale.get()) {
        FR -> frenchTitle
        NL -> dutchTitle
        else -> englishTitle
    }

    @IgnoredOnParcel
    val body = when (AppLocale.get()) {
        NL -> dutchBody
        FR -> frenchBody
        else -> englishBody
    }

    /**
     * To preserve expand/collapse state in [androidx.recyclerview.widget.RecyclerView]
     */
    @IgnoredOnParcel
    var expanded = mutableStateOf(false)
}
