package com.oxygenupdater.ui.dialogs

import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onChildren
import com.oxygenupdater.BuildConfig.SUPPORTED_LANGUAGES
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.get
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.validateLazyColumn
import org.junit.Test
import java.util.Locale

class LanguageSheetTest : ComposeBaseTest() {

    private val itemCount = SUPPORTED_LANGUAGES.size

    @Test
    fun languageSheet() {
        val systemLocale = with(Resources.getSystem().configuration) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) locales[0]
            else @Suppress("DEPRECATION") locale
        }
        lateinit var selectedLocale: Locale
        setContent {
            selectedLocale = currentLocale()
            Column {
                LanguageSheet(
                    onClick = { trackCallback("onClick: $it") },
                    selectedLocale = selectedLocale,
                )
            }
        }

        rule[BottomSheet_HeaderTestTag].assertHasTextExactly(R.string.label_language)
        val rows = rule[LanguageSheet_LazyColumnTestTag, true].validateLazyColumn(itemCount)
        val nodes = rows.fetchSemanticsNodes()

        repeat(nodes.size) { index ->
            val tag = SUPPORTED_LANGUAGES[index]
            val locale = Locale.forLanguageTag(tag)
            val children = rows[index].assertAndPerformClick().onChildren()
            ensureCallbackInvokedExactlyOnce("onClick: $tag")

            // Icon (on) or Spacer (off)
            isToggleable().matches(children[0].fetchSemanticsNode())

            // Title
            children[1].assertHasTextExactly(locale.getDisplayName(locale).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            })

            // Subtitle
            val systemLocalizedName = locale.getDisplayName(systemLocale).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(systemLocale) else it.toString()
            }
            children[2].assertHasTextExactly("$systemLocalizedName [$tag]")

            // "Recommended" icon
            children[3].run {
                if (locale.approxEquals(selectedLocale)) assertExists() else assertDoesNotExist()
            }
        }
    }
}
