package com.oxygenupdater.compose.ui.main

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import com.oxygenupdater.R

@Composable
fun MainSnackbar(
    snackbarHostState: SnackbarHostState,
    snackbarText: MutableState<Pair<Int, Int>?>,
    openPlayStorePage: () -> Unit,
    completeAppUpdate: () -> Unit,
) {
    val data = snackbarText.value
    if (data == null) {
        snackbarHostState.currentSnackbarData?.dismiss()
        return
    }

    val actionResId = data.second
    val message = stringResource(data.first)
    val action = stringResource(actionResId)
    LaunchedEffect(data) {
        val result = snackbarHostState.showSnackbar(
            message, action, false, SnackbarDuration.Indefinite
        )

        if (result != SnackbarResult.ActionPerformed) return@LaunchedEffect
        when (actionResId) {
            AppUpdateFailedSnackbarData.second -> openPlayStorePage()
            AppUpdateDownloadedSnackbarData.second -> completeAppUpdate()
        }
    }
}

val NoConnectionSnackbarData = Pair(
    R.string.error_no_internet_connection, android.R.string.ok
)

val AppUpdateDownloadedSnackbarData = Pair(
    R.string.new_app_version_inapp_downloaded, R.string.error_reload
)

val AppUpdateFailedSnackbarData = Pair(
    R.string.new_app_version_inapp_failed, R.string.error_google_play_button_text
)