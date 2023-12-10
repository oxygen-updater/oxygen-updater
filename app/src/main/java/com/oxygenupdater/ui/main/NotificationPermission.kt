package com.oxygenupdater.ui.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.dialogs.NotificationPermissionSheet

/** This is required only for Android 13+ (API >= 33) */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermission(
    canShow: Boolean,
    hide: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var invokeTime = 0L
    var showSheet by rememberSaveableState("showNotificationPermissionSheet", false)
    val state = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) {
        showSheet = !it
        // If we returned here with a denied status within 250ms of invoking launchPermissionRequest,
        // it's likely that the system didn't show the dialog. However, because this is a user-click,
        // we're opening the APP_NOTIFICATION_SETTINGS screen so that they can grant this permission.
        if (!it && invokeTime != 0L && System.currentTimeMillis() - invokeTime <= 250) context.run {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }

    val status = state.status
    LaunchedEffect(status) {
        when (status) {
            is PermissionStatus.Denied -> if (status.shouldShowRationale) showSheet = true else {
                // Must be called inside an effect to avoid IllegalStateException: ActivityResultLauncher cannot be null
                state.launchPermissionRequest()
            }

            PermissionStatus.Granted -> showSheet = false
        }
    }

    // Respect user's explicit choice to not show the sheet again
    if (canShow && showSheet) ModalBottomSheet({ showSheet = false }) {
        NotificationPermissionSheet(
            hide = {
                hide(it)
                showSheet = false
            },
            launchPermissionRequest = {
                state.launchPermissionRequest()
                invokeTime = System.currentTimeMillis()
            },
        )
    }
}
