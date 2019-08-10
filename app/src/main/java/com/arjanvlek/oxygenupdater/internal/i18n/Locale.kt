package com.arjanvlek.oxygenupdater.internal.i18n

import com.arjanvlek.oxygenupdater.ApplicationData.Companion.LOCALE_DUTCH

enum class Locale {

    NL,
    EN;

    companion object {
        val locale: Locale
            get() {
                val appLocale = java.util.Locale.getDefault().displayLanguage

                return if (appLocale == LOCALE_DUTCH) {
                    NL
                } else EN

            }
    }
}
