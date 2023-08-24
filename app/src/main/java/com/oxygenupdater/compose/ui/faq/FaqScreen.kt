package com.oxygenupdater.compose.ui.faq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdView
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.compose.ui.common.BannerAd
import com.oxygenupdater.compose.ui.common.IconText
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.RichText
import com.oxygenupdater.compose.ui.common.animatedClickable
import com.oxygenupdater.compose.ui.common.withPlaceholder
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.models.InAppFaq

@Composable
fun FaqScreen(
    state: RefreshAwareState<List<InAppFaq>>,
    showAds: Boolean,
    bannerAdInit: (AdView) -> Unit,
) = Column {
    val (refreshing, data) = state
    val list = if (!refreshing) rememberSaveable(data) { data } else data
    val lastIndex = list.lastIndex
    val adLoaded = remember { mutableStateOf(false) }

    LazyColumn(Modifier.weight(1f)) {
        itemsIndexed(
            list,
            // Since the server flattens categories and items into a single JSON
            // array, we need to avoid `id` collisions. 10000 should be enough.
            key = { _, it ->
                it.id + if (it.type == TypeCategory) 10000 else 0
            },
            contentType = { _, it -> it.type },
        ) { index, it ->
            FaqItem(refreshing, it, index == lastIndex, adLoaded.value)
        }
    }

    if (showAds) BannerAd(BuildConfig.AD_BANNER_INSTALL_ID, adLoaded, bannerAdInit)
}

@Composable
private fun FaqItem(
    refreshing: Boolean,
    item: InAppFaq,
    last: Boolean,
    adLoaded: Boolean,
) {
    if (item.type == TypeCategory) Text(
        item.title ?: "",
        Modifier
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            .withPlaceholder(refreshing),
        style = MaterialTheme.typography.titleMedium
    ) else {
        var expanded by remember { item.expanded }
        IconText(
            Modifier
                .fillMaxWidth()
                .animatedClickable { expanded = !expanded }
                .padding(16.dp), // must be after `clickable`
            Modifier.withPlaceholder(refreshing),
            icon = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            text = item.title ?: "",
        )

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
                    .padding(start = 56.dp, end = 16.dp, bottom = 16.dp)
                    .withPlaceholder(refreshing),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!last) ItemDivider()
    }
}

@PreviewThemes
@Composable
fun PreviewFaqScreen() = PreviewAppTheme {
    FaqScreen(
        RefreshAwareState(
            false, listOf(
                InAppFaq(
                    id = 1,
                    title = PreviewTitle,
                    body = null,
                    type = TypeCategory,
                ),
                InAppFaq(
                    id = 1,
                    title = PreviewTitle,
                    body = PreviewBodyHtml,
                    type = TypeItem,
                ),
            )
        ),
        showAds = true,
        bannerAdInit = {},
    )
}

private const val TypeCategory = "category"
private const val TypeItem = "item"

private const val PreviewTitle = "An unnecessarily long FAQ entry, to get an accurate understanding of how long text is rendered"
private const val PreviewBodyHtml = """HTML markup, should be correctly styled:
<span style="background:red">backg</span><span style="background-color:red">round</span>&nbsp;<span style="color:red">foreground</span>
<small>small</small>&nbsp;<big>big</big>
<s>strikethrough</s>
<strong>bo</strong><b>ld</b>&nbsp;<em>ita</em><i>lic</i>&nbsp;<strong><em>bold&amp;</em></strong><em><strong>italic</strong></em>
<sub>sub</sub><sup>super</sup>script
<u>underline</u>
<a href="https://oxygenupdater.com/">link</a>
<script>script tag should render as plain text</script>"""
