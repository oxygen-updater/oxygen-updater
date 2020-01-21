package com.arjanvlek.oxygenupdater.internal.i18n

import com.arjanvlek.oxygenupdater.ApplicationData

enum class AppLocale {
    NL,
    EN;

    companion object {
        fun get() = if (java.util.Locale.getDefault().displayLanguage == ApplicationData.LOCALE_DUTCH) NL else EN
    }
}
