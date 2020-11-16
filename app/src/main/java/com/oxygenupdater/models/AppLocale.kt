package com.oxygenupdater.models

enum class AppLocale {
    EN,
    NL,
    FR;

    companion object {
        fun get() = when (java.util.Locale.getDefault().language) {
            "nl" -> NL
            "fr" -> FR
            else -> EN
        }
    }
}
