package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.positive
import com.oxygenupdater.ui.theme.warn

@Composable
fun ServerMessagesSheet(list: List<ServerMessage>) {
    @Suppress("NAME_SHADOWING")
    val list = if (list.isNotEmpty()) rememberSaveable(list) { list } else return

    SheetHeader(R.string.settings_push_from_server)

    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(list, { it.id }, contentType = { it.priority }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                RichText(it.text, Modifier.padding(start = 16.dp), contentColor = color ?: LocalContentColor.current)
            }
        }
    }
}

@PreviewThemes
@Composable
fun PreviewServerMessagesSheet() = PreviewModalBottomSheet {
    val message = "An unnecessarily long server message, to get an accurate understanding of how long titles are rendered"
    ServerMessagesSheet(
        list = listOf(
            ServerMessage(
                1L,
                text = message,
                deviceId = null,
                updateMethodId = null,
                priority = ServerMessagePriority.LOW,
            ),
            ServerMessage(
                2L,
                text = message,
                deviceId = null,
                updateMethodId = null,
                priority = ServerMessagePriority.MEDIUM,
            ),
            ServerMessage(
                3L,
                text = message,
                deviceId = null,
                updateMethodId = null,
                priority = ServerMessagePriority.HIGH,
            ),
        )
    )
}
