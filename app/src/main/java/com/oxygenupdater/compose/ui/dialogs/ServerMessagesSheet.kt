package com.oxygenupdater.compose.ui.dialogs

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.Error
import com.oxygenupdater.compose.icons.Info
import com.oxygenupdater.compose.icons.Warning
import com.oxygenupdater.compose.ui.common.RichText
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.compose.ui.theme.positive
import com.oxygenupdater.compose.ui.theme.warn
import com.oxygenupdater.models.ServerMessage
import com.oxygenupdater.models.ServerMessage.ServerMessagePriority

@Composable
fun ColumnScope.ServerMessagesSheet(
    hide: () -> Unit,
    list: List<ServerMessage>,
) {
    @Suppress("NAME_SHADOWING")
    val list = if (list.isNotEmpty()) rememberSaveable { list } else return

    SheetHeader(R.string.settings_push_from_server, hide)

    val colors = MaterialTheme.colors
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(list, { it.id }, contentType = { it.priority }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                val priority = it.priority
                val (icon, color) = remember(priority, colors) {
                    when (priority) {
                        ServerMessagePriority.LOW -> CustomIcons.Info to colors.positive
                        ServerMessagePriority.MEDIUM -> CustomIcons.Warning to colors.warn
                        ServerMessagePriority.HIGH -> CustomIcons.Error to colors.error
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
        hide = {},
        list = listOf(
            ServerMessage(
                1L,
                englishMessage = message,
                dutchMessage = message,
                deviceId = null,
                updateMethodId = null,
                priority = ServerMessagePriority.LOW,
            ),
            ServerMessage(
                2L,
                englishMessage = message,
                dutchMessage = message,
                deviceId = null,
                updateMethodId = null,
                priority = ServerMessagePriority.MEDIUM,
            ),
            ServerMessage(
                3L,
                englishMessage = message,
                dutchMessage = message,
                deviceId = null,
                updateMethodId = null,
                priority = ServerMessagePriority.HIGH,
            ),
        )
    )
}

