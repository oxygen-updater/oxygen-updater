package com.oxygenupdater.compose.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.common.DropdownMenuItem

@Composable
fun MainMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    showMarkAllRead: Boolean, markAllRead: () -> Unit,
    showBecomeContributor: Boolean, openContributorSheet: () -> Unit,
) = DropdownMenu(expanded, onDismiss) {
    // Mark all articles read
    if (showMarkAllRead) DropdownMenuItem(Icons.Rounded.PlaylistAddCheck, R.string.news_mark_all_read) {
        markAllRead()
        onDismiss()
    }

    // OTA URL contribution
    if (showBecomeContributor) DropdownMenuItem(Icons.Outlined.GroupAdd, R.string.contribute) {
        openContributorSheet()
        onDismiss()
    }
}
