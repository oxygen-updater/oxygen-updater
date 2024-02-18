package com.oxygenupdater.ui.dialogs

import android.content.ActivityNotFoundException
import android.os.Build.VERSION_CODES
import android.os.storage.StorageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.isNotFocused
import androidx.core.content.getSystemService
import androidx.test.filters.SdkSuppress
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.ui.common.OutlinedIconButtonTestTag
import org.junit.Test
import java.util.UUID

@SdkSuppress(
    // This sheet is supposed to be shown only on Android 8/O+
    minSdkVersion = VERSION_CODES.O,
)
class ManageStorageSheetTest : ModalBottomSheetTest() {

    @Test
    fun manageStorageSheet() {
        var hidden = false
        var pair by mutableStateOf<Pair<UUID, Long>?>(null)
        setContent {
            Column {
                ManageStorageSheet(
                    hide = { hidden = true },
                    pair = pair,
                    downloadAction = {},
                    onCancel = {},
                )
            }
        }

        validateHeaderContentCaption(
            headerResIdOrString = R.string.download_notification_error_storage_full,
            contentResIdOrString = R.string.download_error_storage,
        )

        // First we test for the initial null value of pair
        val dismissButton = validateDismissButton(
            dismissResId = R.string.download_error_close,
            hidden = { hidden },
            result = { false }, // we don't really have a conventional result here
        )

        // Then for non-null
        pair = storageUuidToSizePair()
        hidden = false // reset

        try {
            validateConfirmButton(
                dismissButton = dismissButton,
                confirmResId = android.R.string.ok,
                hidden = { hidden },
                result = ::trueIfConfirmButtonNotFocused,
                resultFailureMessage = { "Intent ${StorageManager.ACTION_MANAGE_STORAGE} did not launch" },
            )
        } catch (e: ActivityNotFoundException) {
            // Should happen only after clicking the button if running on an ATD
            // image, because it doesn't have StorageManager:
            // https://developer.android.com/studio/test/gradle-managed-devices#atd-optimizations.
        }
    }

    /**
     * Mimics actual argument passed to [ManageStorageSheet] via
     * [com.oxygenupdater.ui.update.enqueueIfSpaceAvailable].
     */
    private fun storageUuidToSizePair(): Pair<UUID, Long> {
        val externalFilesDir = activity.getExternalFilesDir(null) ?: throw AssertionError(
            "external files dir = null"
        )

        val storageManager = activity.getSystemService<StorageManager>() ?: throw AssertionError(
            "StorageManager = null"
        )

        return storageManager.getUuidForPath(externalFilesDir) to 1L
    }

    private fun trueIfConfirmButtonNotFocused() = try {
        isNotFocused().matches(
            rule[OutlinedIconButtonTestTag].fetchSemanticsNode()
        )
    } catch (e: IllegalStateException) {
        // This is likely "No compose hierarchies found in the app", which is fine
        // because that's what we expect (we're in the system's manage storage screen now).
        true
    }
}
