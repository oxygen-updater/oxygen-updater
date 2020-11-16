package com.oxygenupdater.models

import com.oxygenupdater.models.AppLocale.FR
import com.oxygenupdater.models.AppLocale.NL

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
    val type: String
) {

    val title = when (AppLocale.get()) {
        FR -> frenchTitle
        NL -> dutchTitle
        else -> englishTitle
    }

    val body = when (AppLocale.get()) {
        NL -> dutchBody
        FR -> frenchBody
        else -> englishBody
    }

    /**
     * To preserver expand/collapse state in [androidx.recyclerview.widget.RecyclerView]
     */
    var expanded = false
}
