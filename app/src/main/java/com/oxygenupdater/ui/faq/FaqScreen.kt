package com.oxygenupdater.ui.faq

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygenupdater.R
import com.oxygenupdater.icons.KeyboardArrowDown
import com.oxygenupdater.icons.KeyboardArrowUp
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.models.InAppFaq
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.ExpandCollapse
import com.oxygenupdater.ui.common.IconText
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun FaqScreen(
    viewModel: FaqViewModel,
    setSubtitleResId: (Int) -> Unit,
) {
    LaunchedEffect(Unit) { setSubtitleResId(R.string.faq) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    FaqScreen(state = state, onRefresh = viewModel::refresh)
}

@VisibleForTesting
@Composable
fun FaqScreen(
    state: RefreshAwareState<List<InAppFaq>>,
    onRefresh: () -> Unit,
) = PullRefresh(
    state = state,
    shouldShowProgressIndicator = { it.isEmpty() },
    onRefresh = onRefresh,
) {
    Column {
        val (refreshing, data) = state
        val list = if (!refreshing) rememberSaveable(data) { data } else data
        val lastIndex = list.lastIndex

        // Perf: re-use common modifiers to avoid recreating the same object repeatedly
        val typography = MaterialTheme.typography
        val titleMedium = typography.titleMedium
        val categoryModifier = modifierDefaultPaddingStartTopEnd
            .withPlaceholder(refreshing, titleMedium)
            .testTag(FaqScreen_CategoryTextTestTag)

        val itemPlaceholderModifier = Modifier.withPlaceholder(refreshing, typography.bodyMedium)
        val itemTextModifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp)

        LazyColumn(
            Modifier
                .weight(1f)
                .testTag(FaqScreen_LazyColumnTestTag)
        ) {
            itemsIndexed(
                items = list,
                // Since the server flattens categories and items into a single JSON
                // array, we need to avoid `id` collisions. 10000 should be enough.
                key = { _, it ->
                    it.id + if (it.type == TypeCategory) 10000 else 0
                },
                contentType = { _, it -> it.type },
            ) { index, it ->
                if (it.type == TypeCategory) Text(
                    text = it.title ?: "",
                    style = titleMedium,
                    modifier = categoryModifier
                ) else FaqItem(
                    item = it,
                    last = index == lastIndex,
                    textModifier = itemTextModifier,
                    modifier = itemPlaceholderModifier
                )
            }
        }
    }
}

@VisibleForTesting
@Composable
fun FaqItem(
    item: InAppFaq,
    last: Boolean,
    modifier: Modifier,
    textModifier: Modifier,
) {
    var expanded by remember { item.expanded }
    IconText(
        icon = if (expanded) Symbols.KeyboardArrowUp else Symbols.KeyboardArrowDown,
        text = item.title ?: "",
        textModifier = modifier,
        modifier = modifierMaxWidth
            .animatedClickable { expanded = !expanded }
            .then(modifierDefaultPadding) // must be after `clickable`
    )

    ExpandCollapse(expanded) {
        RichText(
            text = item.body,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = textModifier then modifier
        )
    }

    if (last) Spacer(Modifier.navigationBarsPadding()) else HorizontalDivider()
}

@VisibleForTesting
const val TypeCategory = "category"

@VisibleForTesting
const val TypeItem = "item"

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

private const val TAG = "FaqScreen"

@VisibleForTesting
const val FaqScreen_LazyColumnTestTag = TAG + "_LazyColumn"

@VisibleForTesting
const val FaqScreen_CategoryTextTestTag = TAG + "_CategoryText"

@VisibleForTesting
val PreviewFaqScreenData = listOf(
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

@PreviewThemes
@Composable
fun PreviewFaqScreen() = PreviewAppTheme {
    FaqScreen(
        state = RefreshAwareState(false, PreviewFaqScreenData),
        onRefresh = {},
    )
}
