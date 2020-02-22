package com.arjanvlek.oxygenupdater.models

enum class AppLocale {
    NL,
    EN;

    companion object {
        private const val LOCALE_DUTCH = "Nederlands"

        fun get() = if (java.util.Locale.getDefault().displayLanguage == LOCALE_DUTCH) NL else EN
    }
}
