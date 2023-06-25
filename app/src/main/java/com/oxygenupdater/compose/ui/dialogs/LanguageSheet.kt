package com.oxygenupdater.compose.ui.dialogs

import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.common.animatedClickable
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.compose.ui.theme.positive
import com.oxygenupdater.extensions.toLocale
import com.oxygenupdater.internal.settings.PrefManager

@Composable
fun ColumnScope.LanguageSheet(
    hide: () -> Unit,
    selectedLanguageCode: String,
    onClick: (String) -> Unit,
) {
    SheetHeader(R.string.label_language, hide)

    val list = remember { BuildConfig.SUPPORTED_LANGUAGES }
    val systemConfig = Resources.getSystem().configuration
    val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        systemConfig.locales[0]
    } else @Suppress("DEPRECATION") systemConfig.locale

    val colors = MaterialTheme.colors
    val typography = MaterialTheme.typography
    LazyColumn {
        items(list, { it }) { code ->
            Row(
                Modifier
                    .animatedClickable {
                        PrefManager.putString(PrefManager.PROPERTY_LANGUAGE_ID, code)
                        hide() // hide first; activity may recreate on change
                        onClick(code)
                    }
                    .padding(16.dp), // must be after `clickable`
                verticalAlignment = Alignment.CenterVertically
            ) {
                val positive = colors.positive
                val selected = selectedLanguageCode == code
                if (selected) Icon(
                    Icons.Rounded.Done, stringResource(R.string.summary_on),
                    Modifier.padding(end = 16.dp),
                    tint = positive,
                ) else Spacer(Modifier.size(40.dp)) // 24 + 16

                val locale = code.toLocale()
                // App-level localized name, which is displayed both as a title and summary
                val appLocalizedName = locale.displayName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }
                // System-level localized name, which is displayed as a fallback for better
                // UX (e.g. if user mistakenly clicked some other language, they should still
                // be able to re-select the correct one because we've provided a localization
                // based on their system language). The language code is also shown, which
                // could help translators.
                val systemLocalizedName = locale.getDisplayName(systemLocale).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(systemLocale) else it.toString()
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        appLocalizedName,
                        color = if (selected) positive else Color.Unspecified,
                        style = typography.subtitle2
                    )

                    Text(
                        "$systemLocalizedName [$code]",
                        Modifier.alpha(ContentAlpha.high),
                        color = if (selected) positive else Color.Unspecified,
                        style = typography.caption
                    )
                }

                val language = locale.language
                val country = locale.country
                if (language == systemLocale.language && (country.isBlank() || country == systemLocale.country)) Icon(
                    Icons.Rounded.AutoAwesome, stringResource(R.string.theme_auto),
                    Modifier.padding(start = 16.dp),
                    tint = colors.secondary
                )
            }
        }
    }
}

@PreviewThemes
@Composable
fun PreviewLanguageSheet() = PreviewModalBottomSheet {
    LanguageSheet(
        hide = {},
        selectedLanguageCode = "en",
    ) {}
}
