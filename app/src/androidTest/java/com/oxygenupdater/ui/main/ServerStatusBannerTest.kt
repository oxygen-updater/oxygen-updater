package com.oxygenupdater.ui.main

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onChildAt
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.get
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.models.ServerStatus.Status
import com.oxygenupdater.ui.common.IconTextTestTag
import org.junit.Test

class ServerStatusBannerTest : ComposeBaseTest() {

    private val root = rule[IconTextTestTag]

    private var serverStatus by mutableStateOf(ServerStatus(Status.NORMAL))

    @Test
    fun serverStatusBanner() {
        setContent {
            ServerStatusBanner(
                serverStatus = serverStatus,
                openPlayStorePage = { trackCallback("openPlayStorePage") },
            )
        }

        serverStatus = ServerStatus(Status.NORMAL)
        root.assertDoesNotExist()
        serverStatus = ServerStatus(Status.MAINTENANCE)
        root.assertDoesNotExist()

        serverStatus = ServerStatus(Status.WARNING)
        validateForRecoverableErrors(R.string.server_status_warning)

        serverStatus = ServerStatus(Status.ERROR)
        validateForRecoverableErrors(R.string.server_status_error)

        serverStatus = ServerStatus(Status.UNREACHABLE)
        validateForRecoverableErrors(R.string.server_status_unreachable)

        serverStatus = ServerStatus(Status.OUTDATED, latestAppVersion = "88.88.88")
        validateForOutdated()
        serverStatus = ServerStatus(latestAppVersion = "88.88.88")
        validateForOutdated()
    }

    private fun validateForRecoverableErrors(@StringRes resId: Int) {
        root.onChildAt(1).assertHasTextExactly(resId)
        ensureNoCallbacksWereInvoked()
    }

    private fun validateForOutdated() = root.run {
        assertHasTextExactly(activity.getString(R.string.new_app_version, serverStatus.latestAppVersion!!))
        assertAndPerformClick()
        ensureCallbackInvokedExactlyOnce("openPlayStorePage")
    }
}
