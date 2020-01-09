package com.arjanvlek.oxygenupdater.internal.i18n

import com.arjanvlek.oxygenupdater.ApplicationData

enum class Locale {
    NL,
    EN;

    companion object {
        val locale: Locale
            get() {
                return if (java.util.Locale.getDefault().displayLanguage == ApplicationData.LOCALE_DUTCH) NL else EN
            }
    }
}
