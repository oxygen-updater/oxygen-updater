package com.oxygenupdater.ui.main

import androidx.compose.runtime.Immutable

@Immutable
@JvmInline
value class ChildScreen(val value: String) {

    override fun toString() = value

    companion object {
        // Warning: changing values would require adjusting other areas in the app

        val Article = ChildScreen("article/")
        val Guide = ChildScreen("guide?")
        val Faq = ChildScreen("faq")

        /** @return true if [route] is a [ChildScreen] */
        fun check(route: String) = route.startsWith(Article.value)
                || route.startsWith(Guide.value)
                || route == Faq.value
    }
}

const val OuScheme = "oxygenupdater://"

const val IdArg = "id"
const val ExternalArg = "external"
const val DownloadedArg = "downloaded"

const val ArticleRoute = "article/{$IdArg}?$ExternalArg={$ExternalArg}"
const val GuideRoute = "guide?$DownloadedArg={$DownloadedArg}"
const val FaqRoute = "faq"
