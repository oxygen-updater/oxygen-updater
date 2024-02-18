package com.oxygenupdater.ui.dialogs

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.internal.NotSet
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.models.SelectableModel
import com.oxygenupdater.ui.SettingsListConfig
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPaddingStart
import com.oxygenupdater.ui.common.modifierSemanticsNotSelected
import com.oxygenupdater.ui.common.modifierSemanticsSelected
import com.oxygenupdater.ui.settings.DeviceSettingsListConfig
import com.oxygenupdater.ui.settings.MethodSettingsListConfig
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun <T : SelectableModel> ColumnScope.SelectableSheet(
    hide: () -> Unit,
    config: SettingsListConfig<T>,
    @StringRes titleResId: Int,
    @StringRes captionResId: Int,
    onClick: (item: T) -> Unit,
) {
    SheetHeader(titleResId)

    val (list, initialIndex, selectedId) = config
    if (list.isEmpty()) return

    val initialFirstVisibleItemIndex = if (selectedId == NotSetL) 0 else remember(list, selectedId) {
        list.indexOfFirst { it.id == selectedId }.let { if (it == NotSet) 0 else it }
    }

    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        state = rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisibleItemIndex),
        modifier = Modifier
            .weight(1f, false)
            .testTag(SelectableSheet_LazyColumnTestTag)
    ) {
        itemsIndexed(items = list, key = { _, it -> it.id }) { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animatedClickable {
                        onClick(item)
                        hide()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp) // must be after `clickable`
                    .testTag(BottomSheet_ItemRowTestTag)
            ) {
                val primary = colorScheme.primary
                val name = item.name ?: return@itemsIndexed
                val selected = selectedId == item.id
                if (selected) Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = stringResource(R.string.summary_on),
                    tint = primary,
                    modifier = Modifier.padding(end = 16.dp) then modifierSemanticsSelected
                ) else Spacer(modifierSemanticsNotSelected.size(40.dp)) // 24 + 16

                Text(
                    text = name,
                    color = if (selected) primary else Color.Unspecified,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )

                if (index == initialIndex) Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = stringResource(R.string.theme_auto),
                    tint = colorScheme.secondary,
                    modifier = modifierDefaultPaddingStart
                )
            }
        }
    }

    SheetCaption(captionResId)
}

private const val TAG = "SelectableSheet"

@VisibleForTesting
const val SelectableSheet_LazyColumnTestTag = TAG + "_LazyColumn"

@SuppressLint("VisibleForTests")
@PreviewThemes
@Composable
fun PreviewDeviceSheet() = PreviewModalBottomSheet {
    SelectableSheet(
        hide = {},
        config = DeviceSettingsListConfig,
        titleResId = R.string.onboarding_device_chooser_title,
        captionResId = R.string.onboarding_device_chooser_caption,
        onClick = {},
    )
}

@SuppressLint("VisibleForTests")
@PreviewThemes
@Composable
fun PreviewMethodSheet() = PreviewModalBottomSheet {
    SelectableSheet(
        hide = {},
        config = MethodSettingsListConfig,
        titleResId = R.string.onboarding_method_chooser_title,
        captionResId = R.string.onboarding_method_chooser_caption,
        onClick = {},
    )
}
