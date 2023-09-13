package com.oxygenupdater.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.Announcement
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.ServerMessage
import com.oxygenupdater.ui.common.DropdownMenuItem
import com.oxygenupdater.ui.common.rememberCallback
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.dialogs.ContributorSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.dialogs.ServerMessagesSheet
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.utils.ContributorUtils

@Composable
fun MainMenu(
    serverMessages: List<ServerMessage>,
    showMarkAllRead: Boolean, markAllRead: () -> Unit,
) {
    AnnouncementsMenuItem(serverMessages)

    // Don't show menu if there are no items in it
    // Box layout is required to make DropdownMenu position correctly (directly under icon)
    val showBecomeContributor = ContributorUtils.isAtLeastQAndPossiblyRooted
    if (!showMarkAllRead && !showBecomeContributor) return

    Box {
        // Hide other menu items behind overflow icon
        var showMenu by rememberSaveableState("showMenu", LocalInspectionMode.current)
        IconButton({ showMenu = true }, Modifier.requiredWidth(40.dp)) {
            Icon(Icons.Rounded.MoreVert, stringResource(androidx.compose.ui.R.string.dropdown_menu))
        }

        val hide = rememberCallback { showMenu = false }
        DropdownMenu(showMenu, hide) {
            // Mark all articles read
            if (showMarkAllRead) DropdownMenuItem(Icons.AutoMirrored.Rounded.PlaylistAddCheck, R.string.news_mark_all_read) {
                markAllRead()
                hide()
            }

            // OTA URL contribution
            if (showBecomeContributor) ContributorMenuItem(hide)
        }
    }
}

/** Server-provided info & warning messages */
@Composable
private fun AnnouncementsMenuItem(serverMessages: List<ServerMessage>) {
    if (serverMessages.isEmpty()) return

    var showSheet by rememberSaveableState("showServerMessagesSheet", false)
    IconButton({ showSheet = true }, Modifier.requiredWidth(40.dp)) {
        Icon(CustomIcons.Announcement, stringResource(R.string.update_information_banner_server))
    }

    if (showSheet) ModalBottomSheet({ showSheet = false }) { ServerMessagesSheet(serverMessages) }
}

@Composable
private fun ContributorMenuItem(onDismiss: () -> Unit) {
    var showSheet by rememberSaveableState("showContributorSheet", false)

    LaunchedEffect(Unit) { // run only on init
        // Offer contribution to users from app versions below v2.4.0 and v5.10.1
        if (ContributorUtils.isAtLeastQAndPossiblyRooted && !PrefManager.contains(PrefManager.KeyContribute)) {
            showSheet = true
        }
    }

    DropdownMenuItem(Icons.Outlined.GroupAdd, R.string.contribute) {
        showSheet = true
        onDismiss()
    }

    if (showSheet) ModalBottomSheet({ showSheet = false }) { ContributorSheet(it, true) }
}

@PreviewThemes
@Composable
fun PreviewMainMenu() = PreviewAppTheme {
    val message = "An unnecessarily long server message, to get an accurate understanding of how long titles are rendered"
    MainMenu(
        serverMessages = listOf(
            ServerMessage(
                1L,
                text = message,
                deviceId = null,
                updateMethodId = null,
                priority = ServerMessage.ServerMessagePriority.LOW,
            ),
            ServerMessage(
                2L,
                text = message,
                deviceId = null,
                updateMethodId = null,
                priority = ServerMessage.ServerMessagePriority.MEDIUM,
            ),
            ServerMessage(
                3L,
                text = message,
                deviceId = null,
                updateMethodId = null,
                priority = ServerMessage.ServerMessagePriority.HIGH,
            ),
        ),
        showMarkAllRead = true,
        markAllRead = {},
    )
}
