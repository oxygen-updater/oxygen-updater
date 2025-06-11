package com.oxygenupdater.ui.dialogs

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.Backspace
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.internal.NotSet
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.models.SelectableModel
import com.oxygenupdater.ui.SettingsListConfig
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPaddingStart
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.modifierSemanticsNotSelected
import com.oxygenupdater.ui.common.modifierSemanticsSelected
import com.oxygenupdater.ui.settings.DeviceSearchFilter
import com.oxygenupdater.ui.settings.DeviceSettingsListConfig
import com.oxygenupdater.ui.settings.MethodSettingsListConfig
import com.oxygenupdater.ui.theme.PreviewThemes
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : SelectableModel> ColumnScope.SelectableSheet(
    hide: () -> Unit,
    config: SettingsListConfig<T>,
    @StringRes titleResId: Int,
    @StringRes captionResId: Int,
    filter: ((T, String) -> Boolean)? = null,
    onClick: (item: T) -> Unit,
) {
    var query by remember { mutableStateOf("") } // not saveable
    if (filter == null) SheetHeader(titleResId) else {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it.trim() },
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = {
                        Text(
                            text = stringResource(titleResId),
                            overflow = TextOverflow.Ellipsis, maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 4.dp) // line up with list
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(android.R.string.search_go),
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.requiredWidth(40.dp)
                        ) {
                            Icon(CustomIcons.Backspace, "Clear")
                        }
                    },
                    modifier = Modifier.testTag(SelectableSheet_SearchBarFieldTestTag)
                )
            },
            expanded = false,
            onExpandedChange = {},
            windowInsets = WindowInsets(0.dp),
            modifier = modifierMaxWidth.testTag(SelectableSheet_SearchBarTestTag)
        ) {}
    }

    val (list, recommendedId, selectedId) = config
    if (list.isEmpty()) return

    val initialFirstVisibleItemIndex = if (selectedId == NotSetL) 0 else remember(list, selectedId) {
        list.indexOfFirst { it.id == selectedId }.let { if (it == NotSet) 0 else it }
    }

    // Debounce search query changes to filter list
    var items by remember { mutableStateOf(list) }
    LaunchedEffect(query) {
        items = if (filter == null || query.isEmpty()) list else {
            delay(100) // debounce by 100ms
            list.filter { filter(it, query) }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        state = rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisibleItemIndex),
        modifier = Modifier
            .weight(1f, false)
            .testTag(SelectableSheet_LazyColumnTestTag)
    ) {
        items(items = items, key = { it.id }) { item ->
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
                val name = item.name ?: return@items
                val id = item.id
                val selected = selectedId == id
                if (selected) Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = stringResource(R.string.summary_on),
                    tint = primary,
                    modifier = Modifier.padding(end = 16.dp) then modifierSemanticsSelected
                ) else Spacer(modifierSemanticsNotSelected.size(40.dp)) // 24 + 16

                Column(
                    Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = name,
                        color = if (selected) primary else Color.Unspecified,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.basicMarquee()
                    )

                    item.subtitle?.let {
                        Text(
                            text = it,
                            color = if (selected) primary else colorScheme.onSurfaceVariant,
                            overflow = TextOverflow.Ellipsis, maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (id == recommendedId) Icon(
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

@VisibleForTesting
const val SelectableSheet_SearchBarTestTag = TAG + "_SearchBar"

@VisibleForTesting
const val SelectableSheet_SearchBarFieldTestTag = TAG + "_SearchBarField"

@SuppressLint("VisibleForTests")
@PreviewThemes
@Composable
fun PreviewDeviceSheet() = PreviewModalBottomSheet {
    SelectableSheet(
        hide = {},
        config = DeviceSettingsListConfig,
        titleResId = R.string.settings_search_devices,
        captionResId = R.string.onboarding_device_chooser_caption,
        filter = DeviceSearchFilter,
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
        titleResId = R.string.settings_update_method,
        captionResId = R.string.onboarding_method_chooser_caption,
        onClick = {},
    )
}
