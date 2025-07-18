package com.oxygenupdater.ui.main

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.Announcement
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.models.ServerMessage
import com.oxygenupdater.ui.common.DropdownMenuItem
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.dialogs.ContributorSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.dialogs.PreviewServerMessagesList
import com.oxygenupdater.ui.dialogs.ServerMessagesSheet
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewGetPrefBool
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.utils.ContributorUtils

@Suppress("UnusedReceiverParameter")
@Composable
fun RowScope.MainMenu(
    serverMessages: List<ServerMessage>,
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    showMarkAllRead: Boolean,
    onMarkAllReadClick: () -> Unit,
    onContributorEnrollmentChange: (Boolean) -> Unit,
) {
    AnnouncementsMenuItem(serverMessages = serverMessages)

    // Don't show menu if there are no items in it
    val showBecomeContributor = ContributorUtils.isAtLeastQAndPossiblyRooted
    if (!showMarkAllRead && !showBecomeContributor) return

    // Box layout is required to make DropdownMenu position correctly (directly under icon)
    Box {
        // Hide other menu items behind overflow icon
        var showMenu by rememberSaveableState("showMenu", false)
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier
                .requiredWidth(40.dp)
                .testTag(MainMenu_OverflowButtonTestTag)
        ) {
            Icon(Icons.Rounded.MoreVert, stringResource(androidx.compose.ui.R.string.dropdown_menu))
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.testTag(MainMenu_DropdownMenuTestTag)
        ) {
            // Mark all articles read
            if (showMarkAllRead) DropdownMenuItem(
                icon = Icons.AutoMirrored.Rounded.PlaylistAddCheck,
                textResId = R.string.news_mark_all_read,
            ) {
                onMarkAllReadClick()
                showMenu = false
            }

            // OTA URL contribution
            if (showBecomeContributor) ContributorMenuItem(
                onDismiss = { showMenu = false },
                getPrefBool = getPrefBool,
                onContributorEnrollmentChange = onContributorEnrollmentChange,
            )
        }
    }
}

/** Server-provided info & warning messages */
@Composable
private fun AnnouncementsMenuItem(serverMessages: List<ServerMessage>) {
    if (serverMessages.isEmpty()) return

    var showSheet by rememberSaveableState("showServerMessagesSheet", false)
    IconButton(
        onClick = { showSheet = true },
        modifier = Modifier
            .requiredWidth(40.dp)
            .testTag(MainMenu_AnnouncementsButtonTestTag)
    ) {
        Icon(CustomIcons.Announcement, stringResource(R.string.update_information_banner_server))
    }

    if (showSheet) ModalBottomSheet({ showSheet = false }) { ServerMessagesSheet(serverMessages) }
}

@Composable
private fun ContributorMenuItem(
    onDismiss: () -> Unit,
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    onContributorEnrollmentChange: (Boolean) -> Unit,
) {
    var showSheet by rememberSaveableState("showContributorSheet", false)

    DropdownMenuItem(Icons.Outlined.GroupAdd, R.string.contribute) {
        showSheet = true
    }

    if (showSheet) ModalBottomSheet({
        showSheet = false
        onDismiss()
    }) {
        ContributorSheet(
            hide = it,
            getPrefBool = getPrefBool,
            confirm = onContributorEnrollmentChange,
        )
    }
}

private const val TAG = "MainMenu"

@VisibleForTesting
const val MainMenu_AnnouncementsButtonTestTag = TAG + "_AnnouncementsButton"

@VisibleForTesting
const val MainMenu_OverflowButtonTestTag = TAG + "_OverflowButton"

@VisibleForTesting
const val MainMenu_DropdownMenuTestTag = TAG + "_DropdownMenu"

@SuppressLint("VisibleForTests")
@PreviewThemes
@Composable
fun PreviewMainMenu() = PreviewAppTheme {
    Row {
        MainMenu(
            serverMessages = PreviewServerMessagesList,
            showMarkAllRead = true,
            getPrefBool = PreviewGetPrefBool,
            onMarkAllReadClick = {},
            onContributorEnrollmentChange = {},
        )
    }
}
