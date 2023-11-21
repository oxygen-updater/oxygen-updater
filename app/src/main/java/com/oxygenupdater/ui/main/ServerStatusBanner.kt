package com.oxygenupdater.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Error
import com.oxygenupdater.icons.Info
import com.oxygenupdater.icons.Warning
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.models.ServerStatus.Status
import com.oxygenupdater.ui.common.IconText
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.warn

@Composable
fun ServerStatusBanner(serverStatus: ServerStatus) {
    val latestAppVersion = serverStatus.latestAppVersion
    if (latestAppVersion != null && serverStatus.shouldShowAppUpdateNotice()) {
        val context = LocalContext.current
        IconText(
            icon = CustomIcons.Info,
            text = stringResource(R.string.new_app_version, latestAppVersion),
            modifier = modifierMaxWidth
                .clickable { context.openPlayStorePage() }
                .then(modifierDefaultPadding)
        )
        return ItemDivider()
    }

    val status = serverStatus.status
    if (status?.isUserRecoverableError != true) return

    val warn = MaterialTheme.colorScheme.warn
    val error = MaterialTheme.colorScheme.error
    val (icon, @StringRes textResId, color) = remember(status, error, warn) {
        when (status) {
            Status.WARNING -> Triple(CustomIcons.Warning, R.string.server_status_warning, warn)
            Status.ERROR -> Triple(CustomIcons.Error, R.string.server_status_error, error)
            Status.UNREACHABLE -> Triple(CustomIcons.Info, R.string.server_status_unreachable, error)
            else -> Triple(ImageVector.Builder("_blank_", 0.dp, 0.dp, 0f, 0f).build(), 0, null)
        }
    }

    if (color == null) return

    IconText(
        icon = icon,
        text = stringResource(textResId),
        iconTint = color,
        style = MaterialTheme.typography.bodyMedium.copy(color = color),
        modifier = modifierMaxWidth then modifierDefaultPadding
    )
    ItemDivider()
}

@PreviewThemes
@Composable
fun PreviewAppOutdated() = PreviewAppTheme {
    Column {
        ServerStatusBanner(ServerStatus(Status.OUTDATED, "8.8.8"))
    }
}

@PreviewThemes
@Composable
fun PreviewServerWarning() = PreviewAppTheme {
    Column {
        ServerStatusBanner(ServerStatus(Status.WARNING))
    }
}

@PreviewThemes
@Composable
fun PreviewServerError() = PreviewAppTheme {
    Column {
        ServerStatusBanner(ServerStatus(Status.ERROR))
    }
}

@PreviewThemes
@Composable
fun PreviewServerUnreachable() = PreviewAppTheme {
    Column {
        ServerStatusBanner(ServerStatus(Status.UNREACHABLE))
    }
}
