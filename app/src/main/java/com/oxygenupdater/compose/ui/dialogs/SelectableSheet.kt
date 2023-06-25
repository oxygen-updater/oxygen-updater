package com.oxygenupdater.compose.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.common.animatedClickable
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.compose.ui.theme.positive
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.SelectableModel
import com.oxygenupdater.models.UpdateMethod

@Composable
fun <T : SelectableModel> ColumnScope.SelectableSheet(
    hide: () -> Unit,
    listState: LazyListState, list: List<T>,
    initialIndex: Int,
    @StringRes titleResId: Int, @StringRes captionResId: Int,
    keyId: String, keyName: String,
    onClick: (T) -> Unit,
) {
    SheetHeader(titleResId, hide)

    val selectedId = PrefManager.getLong(keyId, -1L)

    var selectedIndex by remember { mutableIntStateOf(-1) }
    LaunchedEffect(selectedIndex, listState) {
        if (selectedIndex != -1) listState.animateScrollToItem(selectedIndex)
    }

    val colors = MaterialTheme.colors
    LazyColumn(Modifier.weight(1f, false), state = listState) {
        itemsIndexed(list, { _, it -> it.id }) { index, item ->
            Row(
                Modifier
                    .animatedClickable {
                        PrefManager.putLong(keyId, item.id)
                        PrefManager.putString(keyName, item.name)
                        onClick(item)
                        hide()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp), // must be after `clickable`
                verticalAlignment = Alignment.CenterVertically
            ) {
                val positive = colors.positive
                val name = item.name ?: return@itemsIndexed
                val selected = selectedId == item.id
                if (selected) selectedIndex = index
                if (selected) Icon(
                    Icons.Rounded.Done, stringResource(R.string.summary_on),
                    Modifier.padding(end = 16.dp),
                    tint = positive,
                ) else Spacer(Modifier.size(40.dp)) // 24 + 16

                Text(
                    name,
                    Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    if (selected) positive else Color.Unspecified,
                    style = MaterialTheme.typography.subtitle2
                )

                if (index == initialIndex) Icon(
                    Icons.Rounded.AutoAwesome, stringResource(R.string.theme_auto),
                    Modifier.padding(start = 16.dp),
                    tint = colors.secondary
                )
            }
        }
    }

    SheetCaption(captionResId)
}

@PreviewThemes
@Composable
fun PreviewDeviceSheet() = PreviewModalBottomSheet {
    SelectableSheet(
        hide = {},
        listState = rememberLazyListState(),
        list = listOf(
            Device(
                id = 1,
                name = "OnePlus 7 Pro",
                productName = "OnePlus7Pro",
            ),
            Device(
                id = 2,
                name = "OnePlus 8T",
                productName = "OnePlus8T",
            ),
        ),
        initialIndex = 1,
        titleResId = R.string.onboarding_page_2_title,
        captionResId = R.string.onboarding_page_2_caption,
        keyId = PrefManager.PROPERTY_DEVICE_ID, keyName = PrefManager.PROPERTY_DEVICE,
    ) {}
}

@PreviewThemes
@Composable
fun PreviewMethodSheet() = PreviewModalBottomSheet {
    SelectableSheet(
        hide = {},
        listState = rememberLazyListState(),
        list = listOf(
            UpdateMethod(
                id = 1,
                englishName = "Stable (full)",
                dutchName = "Stabiel (volledig)",
                recommended = false,
                recommendedForRootedDevice = true,
                recommendedForNonRootedDevice = false,
                supportsRootedDevice = true,
            ), UpdateMethod(
                id = 2,
                englishName = "Stable (incremental)",
                dutchName = "Stabiel (incrementeel)",
                recommended = true,
                recommendedForRootedDevice = false,
                recommendedForNonRootedDevice = true,
                supportsRootedDevice = false,
            )
        ),
        initialIndex = 1,
        titleResId = R.string.onboarding_page_3_title,
        captionResId = R.string.onboarding_page_3_caption,
        keyId = PrefManager.PROPERTY_UPDATE_METHOD_ID, keyName = PrefManager.PROPERTY_UPDATE_METHOD,
    ) {}
}
