package com.oxygenupdater.ui.dialogs

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Error
import com.oxygenupdater.icons.Info
import com.oxygenupdater.icons.Warning
import com.oxygenupdater.models.ServerMessage
import com.oxygenupdater.models.ServerMessage.ServerMessagePriority
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStart
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.positive
import com.oxygenupdater.ui.theme.warn

@Composable
fun ServerMessagesSheet(list: List<ServerMessage>) {
    @Suppress("NAME_SHADOWING")
    val list = if (list.isNotEmpty()) rememberSaveable(list) { list } else return

    SheetHeader(R.string.settings_push_from_server)

    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.testTag(ServerMessagesSheet_LazyColumnTestTag)
    ) {
        items(items = list, key = { it.id }, contentType = { it.priority }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifierDefaultPadding.testTag(BottomSheet_ItemRowTestTag)
            ) {
                val priority = it.priority
                val (icon, color) = remember(priority, colorScheme) {
                    when (priority) {
                        ServerMessagePriority.LOW -> CustomIcons.Info to colorScheme.positive
                        ServerMessagePriority.MEDIUM -> CustomIcons.Warning to colorScheme.warn
                        ServerMessagePriority.HIGH -> CustomIcons.Error to colorScheme.error
                        else -> CustomIcons.Info to null
                    }
                }

                Icon(icon, stringResource(R.string.icon), tint = color ?: LocalContentColor.current)
                RichText(
                    text = it.text,
                    contentColor = color ?: LocalContentColor.current,
                    modifier = modifierDefaultPaddingStart
                )
            }
        }
    }
}

private const val TAG = "ServerMessagesSheet"

@VisibleForTesting
const val ServerMessagesSheet_LazyColumnTestTag = TAG + "_LazyColumn"

@VisibleForTesting
val PreviewServerMessagesList = "An unnecessarily long server message, to get an accurate understanding of how long titles are rendered".let { message ->
    listOf(
        ServerMessage(
            1L,
            text = message,
            priority = null,
        ),
        ServerMessage(
            2L,
            text = message,
            priority = ServerMessagePriority.LOW,
        ),
        ServerMessage(
            3L,
            text = message,
            priority = ServerMessagePriority.MEDIUM,
        ),
        ServerMessage(
            4L,
            text = message,
            priority = ServerMessagePriority.HIGH,
        ),
    )
}

@PreviewThemes
@Composable
fun PreviewServerMessagesSheet() = PreviewModalBottomSheet {
    ServerMessagesSheet(PreviewServerMessagesList)
}
