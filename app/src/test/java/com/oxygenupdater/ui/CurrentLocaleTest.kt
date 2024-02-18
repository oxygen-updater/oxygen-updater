package com.oxygenupdater.ui

import com.oxygenupdater.BuildConfig
import java.util.Locale
import kotlin.test.Test

class CurrentLocaleTest {

    @Test
    fun `check if languageOrLanguageTag works`() = BuildConfig.SUPPORTED_LANGUAGES.forEach { languageTag ->
        val locale = Locale.forLanguageTag(languageTag)
        val language = locale.language
        val shouldUseLanguageTag = BuildConfig.SUPPORTED_LANGUAGES.any {
            val index = it.indexOf('-')
            index > 0 && language == it.substring(0, index)
        }

        val result = locale.languageOrLanguageTag()

        assert(result == if (shouldUseLanguageTag) languageTag else language) {
            "Assertion failed for $languageTag"
        }
    }
}
