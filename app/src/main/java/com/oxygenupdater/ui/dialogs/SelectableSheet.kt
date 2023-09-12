package com.oxygenupdater.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.SelectableModel
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.onboarding.NOT_SET
import com.oxygenupdater.ui.onboarding.NOT_SET_L
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun <T : SelectableModel> ColumnScope.SelectableSheet(
    hide: () -> Unit,
    list: List<T>,
    initialIndex: Int,
    @StringRes titleResId: Int, @StringRes captionResId: Int,
    keyId: String,
    onClick: (T) -> Unit,
) {
    SheetHeader(titleResId)

    if (list.isEmpty()) return

    val selectedId = remember(list, initialIndex) {
        PrefManager.getLong(keyId, list.getOrNull(initialIndex)?.id ?: NOT_SET_L)
    }
    val initialFirstVisibleItemIndex = if (selectedId == NOT_SET_L) 0 else remember(list, selectedId) {
        list.indexOfFirst { it.id == selectedId }.let { if (it == NOT_SET) 0 else it }
    }

    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(Modifier.weight(1f, false), rememberLazyListState(initialFirstVisibleItemIndex)) {
        itemsIndexed(list, { _, it -> it.id }) { index, item ->
            Row(
                Modifier
                    .animatedClickable {
                        onClick(item)
                        hide()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp), // must be after `clickable`
                verticalAlignment = Alignment.CenterVertically
            ) {
                val primary = colorScheme.primary
                val name = item.name ?: return@itemsIndexed
                val selected = selectedId == item.id
                if (selected) Icon(
                    Icons.Rounded.Done, stringResource(R.string.summary_on),
                    Modifier.padding(end = 16.dp),
                    tint = primary,
                ) else Spacer(Modifier.size(40.dp)) // 24 + 16

                Text(
                    name,
                    Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    if (selected) primary else Color.Unspecified,
                    style = MaterialTheme.typography.titleSmall
                )

                if (index == initialIndex) Icon(
                    Icons.Rounded.AutoAwesome, stringResource(R.string.theme_auto),
                    Modifier.padding(start = 16.dp),
                    tint = colorScheme.secondary
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
        list = listOf(
            Device(
                id = 1,
                name = "OnePlus 7 Pro",
                productNames = listOf("OnePlus7Pro"),
                enabled = true,
            ),
            Device(
                id = 2,
                name = "OnePlus 8T",
                productNames = listOf("OnePlus8T"),
                enabled = true,
            ),
        ),
        initialIndex = 1,
        titleResId = R.string.onboarding_device_chooser_title,
        captionResId = R.string.onboarding_device_chooser_caption,
        keyId = PrefManager.PROPERTY_DEVICE_ID,
    ) {}
}

@PreviewThemes
@Composable
fun PreviewMethodSheet() = PreviewModalBottomSheet {
    SelectableSheet(
        hide = {},
        list = listOf(
            UpdateMethod(
                id = 1,
                name = "Stable (full)",
                recommendedForRootedDevice = true,
                recommendedForNonRootedDevice = false,
                supportsRootedDevice = true,
            ),
            UpdateMethod(
                id = 2,
                name = "Stable (incremental)",
                recommendedForRootedDevice = false,
                recommendedForNonRootedDevice = true,
                supportsRootedDevice = false,
            )
        ),
        initialIndex = 1,
        titleResId = R.string.onboarding_method_chooser_title,
        captionResId = R.string.onboarding_method_chooser_caption,
        keyId = PrefManager.PROPERTY_UPDATE_METHOD_ID,
    ) {}
}
