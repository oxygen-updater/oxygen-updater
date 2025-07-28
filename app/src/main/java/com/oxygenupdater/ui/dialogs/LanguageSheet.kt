package com.oxygenupdater.ui.dialogs

import android.content.res.Resources
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.icons.Check
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.icons.WandStars
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStart
import com.oxygenupdater.ui.common.modifierSemanticsNotSelected
import com.oxygenupdater.ui.common.modifierSemanticsSelected
import com.oxygenupdater.ui.theme.PreviewThemes
import java.util.Locale

@Composable
fun LanguageSheet(onClick: (String) -> Unit, selectedLocale: Locale) {
    SheetHeader(R.string.label_language)

    val list = remember { BuildConfig.SUPPORTED_LANGUAGES }
    val systemConfig = Resources.getSystem().configuration
    val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        systemConfig.locales[0]
    } else @Suppress("DEPRECATION") systemConfig.locale

    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    // Perf: re-use common modifiers to avoid recreating the same object repeatedly
    val iconSpacerSizeModifier = Modifier.size(40.dp) // 24 + 16
    LazyColumn(Modifier.testTag(LanguageSheet_LazyColumnTestTag)) {
        items(list, { it }) { tag ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animatedClickable { onClick(tag) }
                    .then(modifierDefaultPadding) // must be after `clickable`
                    .testTag(BottomSheet_ItemRowTestTag)
            ) {
                val primary = colorScheme.primary
                val locale = Locale.forLanguageTag(tag)
                val selected = locale.approxEquals(selectedLocale)
                if (selected) Icon(
                    imageVector = Symbols.Check,
                    contentDescription = stringResource(R.string.summary_on),
                    tint = primary,
                    modifier = Modifier.padding(end = 16.dp) then modifierSemanticsSelected
                ) else Spacer(iconSpacerSizeModifier then modifierSemanticsNotSelected)

                Column(Modifier.weight(1f)) {
                    // App-level localized name, which is displayed both as a title and summary
                    val appLocalizedName = locale.getDisplayName(locale).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                    }
                    // System-level localized name, which is displayed as a fallback for better
                    // UX (e.g. if user mistakenly clicked some other language, they should still
                    // be able to re-select the correct one because we've provided a localization
                    // based on their system language). The language tag is also shown, which
                    // could help translators.
                    val systemLocalizedName = locale.getDisplayName(systemLocale).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(systemLocale) else it.toString()
                    }

                    Text(
                        text = appLocalizedName,
                        color = if (selected) primary else Color.Unspecified,
                        style = typography.titleSmall,
                    )

                    Text(
                        text = "$systemLocalizedName [$tag]",
                        color = if (selected) primary else colorScheme.onSurfaceVariant,
                        style = typography.bodySmall,
                    )
                }

                if (locale.approxEquals(systemLocale)) Icon(
                    imageVector = Symbols.WandStars,
                    contentDescription = stringResource(R.string.theme_auto),
                    tint = colorScheme.secondary,
                    modifier = modifierDefaultPaddingStart
                )
            }
        }
    }
}

/**
 * The app's supported language list is generated from `values` directories, which only has `language` info,
 * and at max `country` as well. We compare only these two values because [Locale.equals] is too strict.
 *
 * Note: [LocaleListCompat.matchesLanguageAndScript] could be used in place of this, but that is too complex
 * and makes unnecessary computations for our use-case.
 */
@VisibleForTesting
fun Locale.approxEquals(other: Locale): Boolean {
    val country = country
    return language == other.language && (country.isBlank() || country == other.country)
}

private const val TAG = "LanguageSheet"

@VisibleForTesting
const val LanguageSheet_LazyColumnTestTag = TAG + "_LazyColumn"

@PreviewThemes
@Composable
fun PreviewLanguageSheet() = PreviewModalBottomSheet {
    LanguageSheet(
        onClick = {},
        selectedLocale = Locale.forLanguageTag("en"),
    )
}
