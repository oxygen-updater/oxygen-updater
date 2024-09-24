package com.oxygenupdater.ui.update

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.annotation.StringRes
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onLast
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.assertSizeIsEqualTo
import com.oxygenupdater.extensions.formatFileSize
import com.oxygenupdater.get
import com.oxygenupdater.internal.NotSet
import com.oxygenupdater.ui.common.OutlinedIconButtonTestTag
import com.oxygenupdater.ui.common.RichText_ContainerTestTag
import com.oxygenupdater.ui.device.DefaultDeviceName
import com.oxygenupdater.ui.dialogs.BottomSheet_ContentTestTag
import com.oxygenupdater.ui.dialogs.BottomSheet_DismissButtonTestTag
import com.oxygenupdater.ui.dialogs.BottomSheet_HeaderTestTag
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.update.DownloadFailure.Companion.ConnectionError
import com.oxygenupdater.ui.update.DownloadFailure.Companion.CouldNotMoveTempFile
import com.oxygenupdater.ui.update.DownloadFailure.Companion.DownloadUrlInvalidScheme
import com.oxygenupdater.ui.update.DownloadFailure.Companion.NullUpdateDataOrDownloadUrl
import com.oxygenupdater.ui.update.DownloadFailure.Companion.ServerError
import com.oxygenupdater.ui.update.DownloadFailure.Companion.Unknown
import com.oxygenupdater.ui.update.DownloadFailure.Companion.UnsuccessfulResponse
import com.oxygenupdater.ui.update.DownloadStatus.Companion.DownloadCompleted
import com.oxygenupdater.ui.update.DownloadStatus.Companion.DownloadFailed
import com.oxygenupdater.ui.update.DownloadStatus.Companion.DownloadPaused
import com.oxygenupdater.ui.update.DownloadStatus.Companion.DownloadQueued
import com.oxygenupdater.ui.update.DownloadStatus.Companion.Downloading
import com.oxygenupdater.ui.update.DownloadStatus.Companion.NotDownloading
import com.oxygenupdater.ui.update.DownloadStatus.Companion.VerificationCompleted
import com.oxygenupdater.ui.update.DownloadStatus.Companion.VerificationFailed
import com.oxygenupdater.ui.update.DownloadStatus.Companion.Verifying
import com.oxygenupdater.utils.UpdateDataVersionFormatter
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UpdateAvailableContentTest : ComposeBaseTest() {

    private var updateData by mutableStateOf(
        // 1TB; much more than the free space any device may have
        PreviewUpdateData.copy(downloadSize = 1000000000000)
    )

    private var downloadStatus by mutableStateOf(NotDownloading)
    private var failureType by mutableStateOf<Int>(NotSet)
    private var workProgress by mutableStateOf<WorkProgress?>(null)

    @get:Rule
    val downloadPermissionRule = GrantDownloadPermissionRule()

    /**
     * This is required only for [Android 11/R][VERSION_CODES.R]+, because requesting
     * [MANAGE_EXTERNAL_STORAGE] via [downloadPermissionRule] always fails with:
     * ```
     * junit.framework.AssertionFailedError: Failed to grant permissions, see logcat for details
     * ```
     * These are the only relevant lines in logcat:
     * ```
     * UiAutomationShellCmd: Requesting permission: pm grant <packageName> android.permission.MANAGE_EXTERNAL_STORAGE
     * UiAutomation: UiAutomation.grantRuntimePermission() is more robust and should be used instead of 'pm grant'
     * GrantPermissionCallable: Permission: android.permission.MANAGE_EXTERNAL_STORAGE cannot be granted!
     * ```
     */
    @Before
    fun setup() {
        if (SDK_INT < VERSION_CODES.R) return
        InstrumentationRegistry.getInstrumentation().uiAutomation.adoptShellPermissionIdentity(MANAGE_EXTERNAL_STORAGE)
    }

    @Test
    fun updateAvailableContent_expanded() = common(WindowWidthSizeClass.Expanded)

    @Test
    fun updateAvailableContent_compact() = common(WindowWidthSizeClass.Compact)

    private fun common(windowWidthSize: WindowWidthSizeClass) {
        setContent {
            UpdateAvailable(
                navType = NavType.BottomBar,
                windowWidthSize = windowWidthSize,
                refreshing = false,
                updateData = updateData,
                downloadStatus = downloadStatus,
                failureType = failureType,
                httpFailureCodeAndMessage = { NotSet to "" },
                workProgress = workProgress,
                forceDownloadErrorDialog = false,
                getPrefStr = GetPrefStrForUpdateMethod,
                downloadAction = { trackCallback("downloadAction: $it") },
                openInstallGuide = { trackCallback("openInstallGuide") },
                logDownloadError = { trackCallback("logDownloadError") },
                hideDownloadCompleteNotification = { trackCallback("hideDownloadCompleteNotification") },
                showDownloadFailedNotification = { trackCallback("showDownloadFailedNotification") },
            )
        }

        val expanded = windowWidthSize == WindowWidthSizeClass.Expanded
        val zeroIfExpanded = if (expanded) 0 else 1

        val columns = rule[UpdateAvailableContentTestTag].run {
            assertExists()
            onChildren()
        }.also {
            it.assertCountEquals(2)
            if (expanded) it.assertAll(hasScrollAction()) else it[0].assertHasScrollAction()
        }

        columns[0].onChildren().run {
            // Version
            get(0).onChild().assertHasTextExactly(
                UpdateDataVersionFormatter.getFormattedVersionNumber(updateData, "").takeIf {
                    it.isNotBlank() && it != "-" && it != "null"
                } ?: activity.getString(
                    R.string.update_information_unknown_update_name, DefaultDeviceName,
                ),
            )

            // Changelog
            get(1).assert(hasTestTag(RichText_ContainerTestTag))
        }

        val children = columns[1 - zeroIfExpanded].onChildren().run {
            assertCountEquals(zeroIfExpanded + if (updateData.systemIsUpToDate) 3 else 2)
        }

        // Advanced mode reminder
        if (updateData.systemIsUpToDate) children[zeroIfExpanded + 1].assertHasTextExactly(
            activity.getString(R.string.update_information_header_advanced_mode_helper, UpdateMethod),
        )

        val lastChild = children.onLast()

        /** Must be done before the [VerificationFailed] test */
        lastChild.onChildren().run {
            // Filename
            get(0).assertHasTextExactly(
                activity.getString(R.string.update_information_file_name, updateData.filename),
            )
            // MD5
            get(1).assertHasTextExactly(
                activity.getString(R.string.update_information_md5, updateData.md5sum),
            )
        }

        // Download button
        (if (expanded) children[0] else columns[1]).run {
            // Warning: some validations are order-dependent, so adjust if changed
            downloadStatus = NotDownloading; validateButtonForNotDownloading()
            downloadStatus = DownloadQueued; validateButtonForQueuedOrDownloading()
            downloadStatus = Downloading; validateButtonForQueuedOrDownloading()
            downloadStatus = DownloadPaused; validateButtonForPaused()
            downloadStatus = DownloadCompleted; validateButtonForCompleted(forDownload = true)
            downloadStatus = Verifying; validateButtonForVerifying()
            downloadStatus = VerificationCompleted; validateButtonForCompleted(forDownload = false)
            downloadStatus = DownloadFailed; validateButtonForFailed(forDownload = true)
            downloadStatus = VerificationFailed; validateButtonForFailed(forDownload = false)
        }

        /** After the [VerificationFailed] test, the last child will be [DownloadLink] */
        lastChild.run {
            downloadStatus = VerificationFailed; ensureDownloadLinkShown()

            /** Now we also test for [DownloadFailed] for all [failureType]s */
            downloadStatus = DownloadFailed
            // First for values where link should not be shown
            failureType = NotSet; ensureDownloadLinkNotShown()
            failureType = CouldNotMoveTempFile.value; ensureDownloadLinkNotShown()

            // Then for valid values
            failureType = NullUpdateDataOrDownloadUrl.value; ensureDownloadLinkShown()
            failureType = DownloadUrlInvalidScheme.value; ensureDownloadLinkShown()
            failureType = ServerError.value; ensureDownloadLinkShown()
            failureType = ConnectionError.value; ensureDownloadLinkShown()
            failureType = UnsuccessfulResponse.value; ensureDownloadLinkShown()
            failureType = Unknown.value; ensureDownloadLinkShown()
        }
    }

    private fun SemanticsNodeInteraction.validateButtonForNotDownloading() {
        assertHasTextExactly(
            R.string.download,
            updateData.downloadSize.formattedSize(),
        )

        assertPreviousProgress(null)
        assertPreviousProgressText(null)

        // Initial downloadSize should be much larger than the available space on the emulator,
        // so it clicking should open the insufficient space dialog.
        assertAndPerformClick()

        if (SDK_INT >= VERSION_CODES.O) ensureNoCallbacksWereInvoked()
        // This notification is shown only below Android 8/Oreo
        else ensureCallbackInvokedExactlyOnce("showDownloadFailedNotification")

        validateBottomSheet(
            headerResId = if (SDK_INT >= VERSION_CODES.O) {
                /** [com.oxygenupdater.ui.dialogs.ManageStorageSheet] */
                R.string.download_notification_error_storage_full
            } else {
                /** [com.oxygenupdater.ui.dialogs.DownloadErrorSheet] */
                R.string.download_error
            },
            contentResId = R.string.download_error_storage,
            confirmResId = if (SDK_INT >= VERSION_CODES.O) android.R.string.ok else null,
        )

        // Now, we check for the case where there is enough space
        updateData = PreviewUpdateData.copy(downloadSize = 1)
        assertAndPerformClick()

        if (SDK_INT >= VERSION_CODES.O) ensureCallbackInvokedExactlyOnce(
            "downloadAction: ${DownloadAction.Enqueue}",
        ) else ensureCallbackInvokedExactlyOnce(
            "downloadAction: ${DownloadAction.Enqueue}",
            // Notification was shown only below Android 8/Oreo
            "hideDownloadCompleteNotification", // because sheet was dismissed
        )
        // Don't reset `updateData` so that subsequent tests don't need to check sheet again

        rule[UpdateAvailableContent_ProgressIndicatorTestTag].assertDoesNotExist()
        rule[UpdateAvailableContent_ActionButtonTestTag].assertDoesNotExist()
    }

    /**
     * Note: after calling this function, we need to use [advanceManually]
     * in all subsequent validation functions.
     */
    private fun SemanticsNodeInteraction.validateButtonForQueuedOrDownloading() = advanceManually { ensureDeterminateProgressIndicator ->
        // First we test for the initial value of `workProgress = null`
        assertHasTextExactly(
            R.string.downloading,
            previousProgressText ?: updateData.downloadSize.formattedSize(),
            R.string.summary_please_wait,
        )

        assertAndPerformClick()
        ensureCallbackInvokedExactlyOnce("downloadAction: ${DownloadAction.Pause}")

        // Then for non-null
        workProgress = WorkProgress(
            bytesDone = 1L,
            totalBytes = 2L,
            currentProgress = 50,
            downloadEta = "downloadEta",
        ); advanceFrame()

        workProgress!!.let {
            val progress = it.currentProgress / 100f
            val progressText = it.getProgressText()
            assertPreviousProgress(progress)
            assertPreviousProgressText(progressText)

            assertHasTextExactly(R.string.downloading, progressText, it.downloadEta)
            ensureDeterminateProgressIndicator()
        }

        rule[UpdateAvailableContent_ActionButtonTestTag].run {
            assertSizeIsEqualTo(56.dp)
            assertAndPerformClick()
        }; ensureCallbackInvokedExactlyOnce("downloadAction: ${DownloadAction.Cancel}")

        // Reset for subsequent tests
        workProgress = null
    }

    private fun SemanticsNodeInteraction.validateButtonForPaused() = advanceManually { ensureDeterminateProgressIndicator ->
        assertHasTextExactly(
            R.string.paused,
            previousProgressText ?: updateData.downloadSize.formattedSize(),
            R.string.download_progress_text_paused,
        )

        /** Only if called after [validateButtonForQueuedOrDownloading] */
        ensureDeterminateProgressIndicator()

        rule[UpdateAvailableContent_ActionButtonTestTag].run {
            assertSizeIsEqualTo(56.dp)
            assertAndPerformClick()
        }; ensureCallbackInvokedExactlyOnce("downloadAction: ${DownloadAction.Cancel}")
    }

    private fun SemanticsNodeInteraction.validateButtonForCompleted(forDownload: Boolean) = advanceManually {
        assertHasTextExactly(
            R.string.downloaded,
            updateData.downloadSize.formattedSize(),
        )

        assertAndPerformClick(); advanceFrame()
        validateBottomSheet(
            /** [com.oxygenupdater.ui.dialogs.AlreadyDownloadedSheet] */
            headerResId = R.string.delete_message_title,
            contentResId = R.string.delete_message_contents,
            dismissResId = R.string.delete_message_delete_button,
            confirmResId = R.string.install,
        )

        if (forDownload) ensureCallbackInvokedExactlyOnce(
            "downloadAction: ${DownloadAction.Delete}", // because sheet was dismissed
        ) else {
            /** Only if called after [validateButtonForQueuedOrDownloading] */
            ensureCallbackInvokedExactlyOnce(
                "openInstallGuide",
                "downloadAction: ${DownloadAction.Delete}", // because sheet was dismissed
            )
            assertPreviousProgress(null)
            assertPreviousProgressText(null)
        }

        rule[UpdateAvailableContent_ProgressIndicatorTestTag].assertDoesNotExist()
        rule[UpdateAvailableContent_ActionButtonTestTag].run {
            assertSizeIsEqualTo(56.dp)
            assertAndPerformClick()
        }; ensureCallbackInvokedExactlyOnce("openInstallGuide")
    }

    /**
     * Compared to others, this validation differs in its implementation in that
     * it does not use [SemanticsNodeInteraction] as the receiver, because for
     * some reason Compose does not show the download button in the hierarchy
     * at all. Maybe it's caused by the fact that in this state there's no
     * click action on the download button.
     *
     * So, we use its test tag and check each child individually.
     */
    private fun validateButtonForVerifying() = advanceManually {
        val children = rule[UpdateAvailableContent_DownloadButtonTestTag].run {
            assertHasNoClickAction()
            onChildren().assertCountEquals(5)
        }

        children[1].assertHasTextExactly(R.string.download_verifying)
        children[2].assertHasTextExactly(updateData.downloadSize.formattedSize())
        children[3].assertHasTextExactly(R.string.download_progress_text_verifying)

        children[4].run {
            assert(hasTestTag(UpdateAvailableContent_ProgressIndicatorTestTag))
            assert(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        }

        rule[UpdateAvailableContent_ActionButtonTestTag].assertDoesNotExist()
    }

    private fun SemanticsNodeInteraction.validateButtonForFailed(forDownload: Boolean) = advanceManually {
        if (forDownload) assertHasTextExactly(
            R.string.download,
            updateData.downloadSize.formattedSize(),
            R.string.download_failed,
        ) else {
            assertHasTextExactly(
                R.string.download_verifying_error,
                updateData.downloadSize.formattedSize(),
                R.string.download_notification_error_corrupt,
            )

            advanceFrame(); validateBottomSheet(
                /** [com.oxygenupdater.ui.dialogs.DownloadErrorSheet] */
                headerResId = R.string.download_error,
                contentResId = R.string.download_error_corrupt,
                confirmResId = R.string.download_error_retry,
            )
        }

        assertAndPerformClick()
        if (forDownload) ensureCallbackInvokedExactlyOnce(
            "downloadAction: ${DownloadAction.Enqueue}",
        ) else ensureCallbackInvokedExactlyOnce(
            "downloadAction: ${DownloadAction.Enqueue}",
            "hideDownloadCompleteNotification", // because sheet was dismissed
        )
        assertPreviousProgress(null)
        assertPreviousProgressText(null)

        rule[UpdateAvailableContent_ProgressIndicatorTestTag].assertDoesNotExist()
        rule[UpdateAvailableContent_ActionButtonTestTag].assertDoesNotExist()
    }

    /**
     * BottomSheet is shown in several cases, e.g. on download/verification failure,
     * to request user to free up storage space, etc.
     *
     * @see [downloadButtonConfig]
     * @see [DownloadButtonContainer]
     */
    private fun validateBottomSheet(
        @StringRes headerResId: Int,
        @StringRes contentResId: Int,
        @StringRes dismissResId: Int = R.string.download_error_close,
        @StringRes confirmResId: Int? = null,
    ) {
        rule[BottomSheet_HeaderTestTag].assertHasTextExactly(headerResId)
        rule[BottomSheet_ContentTestTag].assertHasTextExactly(contentResId)

        if (confirmResId != null) rule[OutlinedIconButtonTestTag].run {
            assertHasClickAction()
            assertHasTextExactly(confirmResId)
        } else rule[OutlinedIconButtonTestTag].assertDoesNotExist()

        rule[BottomSheet_DismissButtonTestTag].run {
            assertHasTextExactly(dismissResId)
            assertAndPerformClick()
        }
    }

    private fun SemanticsNodeInteraction.ensureDownloadLinkShown() = advanceManually {
        assert(hasTestTag(RichText_ContainerTestTag))
        onChild().assertHasTextExactly(activity.getString(R.string.update_information_download_link, updateData.downloadUrl))
    }

    private fun SemanticsNodeInteraction.ensureDownloadLinkNotShown() = advanceManually {
        assert(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, RichText_ContainerTestTag).not())
    }

    private fun assertPreviousProgress(progress: Float?) = assert(previousProgress == progress) {
        "previousProgress did not match. Expected $progress, actual: $previousProgress."
    }

    private fun assertPreviousProgressText(progressText: String?) = assert(previousProgressText == progressText) {
        "previousProgressText did not match. Expected $progressText, actual: $previousProgressText."
    }

    /**
     * Compose awaits idleness when using [SemanticsNodeInteraction.fetchSemanticsNode]
     * or similar functions internally, so that it acts on a "stable" UI state.
     *
     * However, when there are nodes in the tree that animate, Compose will never
     * be idle because animations count as pending work (recomposition).
     *
     * For example, in the [DownloadStatus.inProgress] state, we're displaying a
     * [androidx.compose.animation.graphics.vector.AnimatedImageVector]. Also, in other
     * states, we show [androidx.compose.material3.LinearProgressIndicator].
     *
     * So we have to programmatically advance frame-by-frame to test our UI.
     *
     * @param block UI tests that must be done after `autoAdvance = false`
     *
     * Callers can invoke [advanceFrame] within [block] if needed.
     */
    private inline fun advanceManually(block: (ensureDeterminateProgressIndicator: () -> Unit) -> Unit) {
        rule.mainClock.autoAdvance = false // turn off
        advanceFrame() // let UI update if states were updated

        block {
            rule[UpdateAvailableContent_ProgressIndicatorTestTag].assert(
                // `hasProgressBarRangeInfo(ProgressBarRangeInfo(progress, 0f..1f))` can't
                // be used because it's animated, and we've opted out of `autoAdvance` in
                // this block. Instead of waiting for an unknown amount of time for the
                // indicator to stabilize, we simply check if it's not indeterminate.
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo).and(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate
                    ).not()
                )
            )
        }

        rule.mainClock.autoAdvance = true // reset
    }

    private fun Long.formattedSize() = activity.formatFileSize(this)
    private fun WorkProgress.getProgressText() = "${bytesDone.formattedSize()} / ${totalBytes.formattedSize()} ($currentProgress%)"
}
