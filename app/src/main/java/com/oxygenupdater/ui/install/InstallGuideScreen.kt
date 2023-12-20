package com.oxygenupdater.ui.install

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygenupdater.R
import com.oxygenupdater.models.InstallGuide
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.ExpandCollapse
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.common.ListItemTextIndent
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun InstallGuideScreen(
    viewModel: InstallGuideViewModel,
    downloaded: Boolean,
    setSubtitleResId: (Int) -> Unit,
) {
    LaunchedEffect(Unit) { setSubtitleResId(R.string.install_guide) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    InstallGuideScreen(
        state = state,
        onRefresh = viewModel::refresh,
        downloaded = downloaded,
    )
}

@Composable
private fun InstallGuideScreen(
    state: RefreshAwareState<List<InstallGuide>>,
    onRefresh: () -> Unit,
    downloaded: Boolean,
) = PullRefresh(
    state = state,
    shouldShowProgressIndicator = { it.isEmpty() },
    onRefresh = onRefresh,
) {
    Column {
        val (refreshing, data) = state
        val list = if (!refreshing) rememberSaveable(data) { data } else data
        val lastIndex = list.lastIndex

        LazyColumn(Modifier.weight(1f)) {
            // Show download instructions only if user hasn't downloaded yet
            if (!downloaded) item {
                val bodyMedium = MaterialTheme.typography.bodyMedium
                Text(
                    text = AnnotatedString(
                        stringResource(R.string.install_guide_download_instructions),
                        bodyMedium.toSpanStyle(),
                        bodyMedium.toParagraphStyle().copy(textIndent = ListItemTextIndent)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = bodyMedium,
                    modifier = modifierDefaultPadding
                )
                ItemDivider()
            }

            itemsIndexed(items = list, key = { _, it -> it.id }) { index, it ->
                InstallGuideItem(
                    refreshing = refreshing,
                    item = it,
                    last = index == lastIndex,
                )
            }
        }
    }
}

@Composable
private fun InstallGuideItem(
    refreshing: Boolean,
    item: InstallGuide,
    last: Boolean,
) {
    var expanded by remember { item.expanded }
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    val typography = MaterialTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifierMaxWidth
            .animatedClickable { expanded = !expanded }
            .then(modifierDefaultPadding)
    ) {
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = stringResource(R.string.icon),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp)
        )

        Column {
            Text(
                text = item.title,
                style = typography.titleMedium,
                modifier = Modifier.withPlaceholder(refreshing, typography.titleMedium)
            )

            Text(
                text = item.subtitle,
                color = contentColor,
                style = typography.bodySmall,
                modifier = Modifier.withPlaceholder(refreshing, typography.bodySmall)
            )
        }
    }

    ExpandCollapse(expanded) {
        RichText(
            text = item.body,
            textIndent = ListItemTextIndent,
            contentColor = contentColor,
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp, bottom = 16.dp)
                .withPlaceholder(refreshing, typography.bodyMedium)
        )
    }

    if (last) Spacer(Modifier.navigationBarsPadding()) else ItemDivider()
}

@PreviewThemes
@Composable
fun PreviewInstallGuideScreen() = PreviewAppTheme {
    InstallGuideScreen(
        state = RefreshAwareState(
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
        onRefresh = {},
        downloaded = false,
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
