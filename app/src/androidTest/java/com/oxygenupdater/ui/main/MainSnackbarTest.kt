package com.oxygenupdater.ui.main

import androidx.collection.IntIntPair
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onChildren
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.assertAndPerformClick
import org.junit.Test

class MainSnackbarTest : ComposeBaseTest() {

    private var snackbarText by mutableStateOf<IntIntPair?>(null)

    @Test
    fun mainSnackbar() {
        setContent {
            MainSnackbar(
                snackbarText = snackbarText,
                openPlayStorePage = { trackCallback("openPlayStorePage") },
                completeAppUpdate = { trackCallback("completeAppUpdate") },
            )
        }

        // Then for other non-null values
        snackbarText = NoConnectionSnackbarData
        validateNodes(null)

        snackbarText = AppUpdateFailedSnackbarData
        validateNodes("openPlayStorePage")

        snackbarText = AppUpdateDownloadedSnackbarData
        validateNodes("completeAppUpdate")
    }

    /**
     * @param callbackToCheck [ensureNoCallbacksWereInvoked] if null, otherwise
     *   [ensureCallbackInvokedExactlyOnce]
     */
    private fun validateNodes(callbackToCheck: String? = null) {
        val (messageResId, actionResId) = snackbarText!!
        val children = rule.onAllNodes(
            // Snackbar is wrapped in a Surface (implementation detail), which
            // has `isContainer = true` even though it's deprecated. This is
            // how we filter for the snackbar component.
            @Suppress("DEPRECATION")
            SemanticsMatcher.keyIsDefined(SemanticsProperties.IsContainer)
        )[1].onChildren()
        // Note: [1] because [0] would be Surface, which PreviewAppTheme wraps our content in

        // Message
        children[0].assertHasTextExactly(messageResId)

        // Action
        children[1].run {
            assertHasTextExactly(actionResId)
            assertAndPerformClick() // maybe triggers callbacks, check below
        }

        if (callbackToCheck == null) ensureNoCallbacksWereInvoked()
        else ensureCallbackInvokedExactlyOnce(callbackToCheck)
    }
}
