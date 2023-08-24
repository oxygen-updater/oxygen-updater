package com.oxygenupdater.compose.ui.install

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdView
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.compose.ui.common.BannerAd
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.ListItemTextIndent
import com.oxygenupdater.compose.ui.common.RichText
import com.oxygenupdater.compose.ui.common.animatedClickable
import com.oxygenupdater.compose.ui.common.withPlaceholder
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.models.InstallGuide

@Composable
fun InstallGuideScreen(
    state: RefreshAwareState<List<InstallGuide>>,
    showAds: Boolean,
    bannerAdInit: (AdView) -> Unit,
) = Column {
    val (refreshing, data) = state
    val list = if (!refreshing) rememberSaveable(data) { data } else data
    val lastIndex = list.lastIndex
    val adLoaded = remember { mutableStateOf(false) }

    LazyColumn(Modifier.weight(1f)) {
        itemsIndexed(list, key = { _, it -> it.id }) { index, it ->
            InstallGuideItem(refreshing, it, index == lastIndex, adLoaded.value)
        }
    }

    if (showAds) BannerAd(BuildConfig.AD_BANNER_INSTALL_ID, adLoaded, bannerAdInit)
}

@Composable
private fun InstallGuideItem(
    refreshing: Boolean,
    item: InstallGuide,
    last: Boolean,
    adLoaded: Boolean,
) {
    var expanded by remember { item.expanded }
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        Modifier
            .fillMaxWidth()
            .animatedClickable { expanded = !expanded }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            stringResource(R.string.icon),
            Modifier.padding(end = 16.dp),
            MaterialTheme.colorScheme.primary
        )

        val typography = MaterialTheme.typography
        Column {
            Text(
                item.title,
                Modifier.withPlaceholder(refreshing),
                style = typography.titleMedium
            )

            Text(
                item.subtitle,
                Modifier.withPlaceholder(refreshing),
                contentColor,
                style = typography.bodySmall
            )
        }
    }

    AnimatedVisibility(
        expanded,
        if (last) {
            // Don't re-consume navigation bar insets
            if (adLoaded) Modifier else Modifier.navigationBarsPadding()
        } else Modifier,
        enter = remember {
            expandVertically(
                spring(visibilityThreshold = IntSize.VisibilityThreshold)
            ) + fadeIn(initialAlpha = .3f)
        },
        exit = remember {
            shrinkVertically(
                spring(visibilityThreshold = IntSize.VisibilityThreshold)
            ) + fadeOut()
        },
    ) {
        RichText(
            item.body,
            Modifier
                .padding(start = 20.dp, end = 16.dp, bottom = 16.dp)
                .withPlaceholder(refreshing),
            textIndent = ListItemTextIndent,
            contentColor = contentColor,
        )
    }

    if (!last) ItemDivider()
}

@PreviewThemes
@Composable
fun PreviewInstallGuideScreen() = PreviewAppTheme {
    InstallGuideScreen(
        RefreshAwareState(
            false, listOf(
                InstallGuide(
                    id = 1,
                    title = PreviewTitle,
                    subtitle = PreviewBodyPrefix,
                    body = PreviewBodyHtml,
                ),
                InstallGuide(
                    id = 2,
                    title = PreviewTitle,
                    subtitle = PreviewBodyPrefix,
                    body = PreviewBodyHtml,
                ),
            )
        ),
        showAds = true,
        bannerAdInit = {},
    )
}

private const val PreviewTitle = "An unnecessarily long guide entry, to get an accurate understanding of how long text is rendered"
private const val PreviewBodyPrefix = "More information about this guide entry"
private const val PreviewBodyHtml = """HTML markup, should be correctly styled:
<span style="background:red">backg</span><span style="background-color:red">round</span>&nbsp;<span style="color:red">foreground</span>
<small>small</small>&nbsp;<big>big</big>
<s>strikethrough</s>
<strong>bo</strong><b>ld</b>&nbsp;<em>ita</em><i>lic</i>&nbsp;<strong><em>bold&amp;</em></strong><em><strong>italic</strong></em>
<sub>sub</sub><sup>super</sup>script
<u>underline</u>
<a href="https://oxygenupdater.com/">link</a>
<script>script tag should render as plain text</script>"""
