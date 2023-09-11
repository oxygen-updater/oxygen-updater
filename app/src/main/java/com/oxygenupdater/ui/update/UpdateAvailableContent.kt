package com.oxygenupdater.ui.update

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.oxygenupdater.R
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.extensions.startInstallActivity
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.RichTextType
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.rememberCallback
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.ui.device.defaultDeviceName
import com.oxygenupdater.ui.dialogs.AlreadyDownloadedSheet
import com.oxygenupdater.ui.dialogs.DownloadErrorSheet
import com.oxygenupdater.ui.dialogs.ManageStorageSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.onboarding.NOT_SET
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.backgroundVariant
import com.oxygenupdater.ui.theme.positive
import com.oxygenupdater.ui.update.DownloadStatus.DOWNLOAD_FAILED
import com.oxygenupdater.ui.update.DownloadStatus.NOT_DOWNLOADING
import com.oxygenupdater.ui.update.DownloadStatus.VERIFICATION_FAILED
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.UpdateDataVersionFormatter
import java.util.UUID

@Composable
fun UpdateAvailable(
    refreshing: Boolean,
    updateData: UpdateData,
    downloadStatus: DownloadStatus,
    failureType: Int?,
    workProgress: WorkProgress?,
    forceDownloadErrorDialog: Boolean,
    downloadAction: (DownloadAction) -> Unit,
    logDownloadError: () -> Unit,
) = Column {
    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp) // must be after `verticalScroll`
    ) {
        SelectionContainer(Modifier.padding(horizontal = 16.dp)) {
            Text(
                UpdateDataVersionFormatter.getFormattedVersionNumber(updateData, "").takeIf {
                    it.isNotBlank() && it != "-" && it != "null"
                } ?: stringResource(
                    R.string.update_information_unknown_update_name,
                    defaultDeviceName().let { PrefManager.getString(PrefManager.PROPERTY_DEVICE, it) ?: it }
                ),
                Modifier.withPlaceholder(refreshing),
                style = MaterialTheme.typography.titleLarge
            )
        }

        updateData.Changelog(
            Modifier
                .padding(start = 16.dp, top = 4.dp, end = 16.dp)
                .withPlaceholder(refreshing),
            true
        )

        Spacer(Modifier.weight(1f))
        ItemDivider(Modifier.padding(vertical = 16.dp))

        val bodySmall = MaterialTheme.typography.bodySmall
        if (updateData.systemIsUpToDate) {
            // Remind user why they're seeing this update
            val updateMethod = PrefManager.getString(PrefManager.PROPERTY_UPDATE_METHOD, "<UNKNOWN>") ?: "<UNKNOWN>"
            Text(
                stringResource(R.string.update_information_header_advanced_mode_helper, updateMethod),
                Modifier.padding(horizontal = 16.dp),
                MaterialTheme.colorScheme.onSurfaceVariant,
                style = bodySmall
            )

            ItemDivider(Modifier.padding(vertical = 16.dp))
        }

        SelectionContainer(Modifier.padding(start = 16.dp, end = 8.dp)) {
            Text(
                stringResource(R.string.update_information_file_name, updateData.filename ?: Build.UNKNOWN),
                Modifier.withPlaceholder(refreshing),
                MaterialTheme.colorScheme.onSurfaceVariant,
                style = bodySmall
            )
        }

        // Needs to be a separate container to avoid losing Column context
        SelectionContainer(Modifier.padding(start = 16.dp, end = 8.dp)) {
            Text(
                stringResource(R.string.update_information_md5, updateData.md5sum ?: Build.UNKNOWN),
                Modifier.withPlaceholder(refreshing),
                MaterialTheme.colorScheme.onSurfaceVariant,
                style = bodySmall
            )
        }

        if (downloadStatus == VERIFICATION_FAILED || (downloadStatus == DOWNLOAD_FAILED && failureType.let {
                // Note: show download link only if failure is any of these:
                // - NullUpdateDataOrDownloadUrl
                // - DownloadUrlInvalidScheme
                // - ServerError
                // - ConnectionError
                // - UnsuccessfulResponse
                // - Unknown
                // For simplicity, we're negating boolean logic by checking for the only other possible values:
                // null and CouldNotMoveTempFile. This will need to be adjusted later if new failures are added.
                it != null && it != NOT_SET && it != DownloadFailure.CouldNotMoveTempFile.value
            })
        ) {
            DownloadLink(updateData.downloadUrl ?: "null")
        }
    }

    DownloadButton(
        refreshing,
        updateData.downloadSize,
        failureType,
        workProgress,
        downloadStatus,
        forceDownloadErrorDialog,
        downloadAction,
        logDownloadError,
    )
}

@Composable
private fun DownloadLink(url: String) {
    val bodySmall = MaterialTheme.typography.bodySmall

    val text = stringResource(R.string.update_information_download_link, url)
    val length = text.length

    @OptIn(ExperimentalTextApi::class)
    @Suppress("NAME_SHADOWING")
    RichText(
        text,
        Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        type = RichTextType.Custom
    ) { text, contentColor, urlColor ->
        AnnotatedString.Builder(length).apply {
            // Global
            append(text)
            addStyle(bodySmall.toSpanStyle().copy(color = contentColor), 0, length)
            addStyle(bodySmall.toParagraphStyle(), 0, length)

            // For download URL
            val start = text.indexOf('\n') + 1
            addUrlAnnotation(UrlAnnotation(url), start, length)
            addStyle(SpanStyle(color = urlColor, textDecoration = TextDecoration.Underline), start, length)
        }.toAnnotatedString()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun DownloadButton(
    refreshing: Boolean,
    downloadSize: Long,
    failureType: Int?,
    workProgress: WorkProgress?,
    downloadStatus: DownloadStatus,
    forceDownloadErrorDialog: Boolean,
    downloadAction: (DownloadAction) -> Unit,
    logDownloadError: () -> Unit,
) {
    val context = LocalContext.current

    @Suppress("IMPLICIT_CAST_TO_ANY")
    val downloadPermissionState = if (LocalInspectionMode.current) Unit
    else if (SDK_INT >= VERSION_CODES.R) rememberAllFilesPermissionState(context) else rememberMultiplePermissionsState(
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    )

    // Deferred read
    val hasDownloadPermissions = rememberCallback(downloadPermissionState) {
        if (SDK_INT >= VERSION_CODES.R && downloadPermissionState is AllFilesPermissionState) {
            downloadPermissionState.status.isGranted
        } else if (downloadPermissionState is MultiplePermissionsState) downloadPermissionState.allPermissionsGranted else {
            // If state is somehow something else, we can't check if permissions are granted.
            // So, make an educated guess: on API < 23 both READ & WRITE permissions
            // are granted automatically, while on API >= 23 only READ is granted
            // automatically. A simple SDK version check should suffice.
            SDK_INT <= VERSION_CODES.M
        }
    }

    val requestDownloadPermissions = rememberCallback(downloadPermissionState) {
        if (SDK_INT >= VERSION_CODES.R && downloadPermissionState is AllFilesPermissionState) {
            downloadPermissionState.launchPermissionRequest()
        } else if (downloadPermissionState is MultiplePermissionsState) {
            downloadPermissionState.launchMultiplePermissionRequest()
        }
    }

    var showAlreadyDownloadedSheet by rememberSaveableState("showAlreadyDownloadedSheet", false)
    if (showAlreadyDownloadedSheet) {
        val hide = rememberCallback { showAlreadyDownloadedSheet = false }
        ModalBottomSheet(hide) {
            AlreadyDownloadedSheet(hide) {
                if (it) context.startInstallActivity(false)
                else if (hasDownloadPermissions()) downloadAction(DownloadAction.Delete)
                else requestDownloadPermissions()
            }
        }
    }

    // Since params are set automatically (i.e. without user action), we need an extra flag to control
    // when dialog can be shown, to avoid infinitely re-showing it on every state change.
    var canShowDownloadErrorDialog by remember { mutableStateOf(forceDownloadErrorDialog) }
    var downloadErrorDialogParams by remember { mutableStateOf<DownloadErrorParams?>(null) }
    if (canShowDownloadErrorDialog) downloadErrorDialogParams?.let {
        val hide = rememberCallback {
            canShowDownloadErrorDialog = false
            downloadErrorDialogParams = null
        }
        ModalBottomSheet(hide) { DownloadErrorSheet(hide, it) }
    }

    var manageStorageDialogData by remember { mutableStateOf<Pair<UUID, Long>?>(null) }
    if (SDK_INT >= VERSION_CODES.O) manageStorageDialogData?.let {
        val hide = rememberCallback { manageStorageDialogData = null }
        val text = stringResource(R.string.download_error_storage)
        ModalBottomSheet(hide) {
            ManageStorageSheet(hide, it, downloadAction) {
                downloadErrorDialogParams = DownloadErrorParams(text)
            }
        }
    }

    DownloadButton(
        refreshing,
        downloadStatus,
        downloadAction,
        downloadButtonConfig(
            downloadSize,
            failureType,
            workProgress,
            downloadStatus,
            downloadAction,
            logDownloadError,
            hasDownloadPermissions,
            requestDownloadPermissions = requestDownloadPermissions,
            showAlreadyDownloadedSheet = { showAlreadyDownloadedSheet = true },
            setCanShowDownloadErrorDialog = { canShowDownloadErrorDialog = true },
            setDownloadErrorDialogParams = { downloadErrorDialogParams = it },
            setManageStorageDialogData = { manageStorageDialogData = it },
        ),
    )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun DownloadButton(
    refreshing: Boolean,
    downloadStatus: DownloadStatus,
    downloadAction: (DownloadAction) -> Unit,
    buttonConfig: DownloadButtonConfig,
) {
    val (titleResId, details, sizeOrProgressText, progress, actionButtonConfig, onDownloadClick) = buttonConfig
    val colorScheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .alpha(if (refreshing) 0.38f else 1f)
            .background(colorScheme.backgroundVariant)
            .animatedClickable(!refreshing && onDownloadClick != null, onDownloadClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val iconContentDescription = stringResource(R.string.icon)
            val inProgress = downloadStatus.inProgress
            val successful = downloadStatus.successful
            if (inProgress) {
                var atEnd by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { atEnd = true /* start immediately */ } // run only on init

                Icon(
                    rememberAnimatedVectorPainter(
                        AnimatedImageVector.animatedVectorResource(R.drawable.download_anim),
                        atEnd
                    ),
                    iconContentDescription,
                    Modifier.requiredWidth(56.dp),
                    tint = colorScheme.positive
                )
            } else Icon(
                if (successful) Icons.Rounded.CheckCircleOutline else Icons.Rounded.Download,
                iconContentDescription,
                Modifier.requiredWidth(56.dp),
                tint = if (successful) colorScheme.positive else colorScheme.primary
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
            ) {
                val typography = MaterialTheme.typography
                // Title
                Text(stringResource(titleResId), style = typography.bodyMedium)
                // Size + progress%
                Text(sizeOrProgressText, color = colorScheme.onSurfaceVariant, style = typography.bodyMedium)
                if (details != null) Text(details, style = typography.bodyMedium)
            }

            actionButtonConfig?.let { (forCancel, icon, tint) ->
                val context = LocalContext.current
                IconButton(
                    if (forCancel) ({ downloadAction(DownloadAction.Cancel) }) else rememberCallback(context) {
                        context.startInstallActivity(false)
                    },
                    Modifier.requiredSize(56.dp)
                ) {
                    Icon(icon, iconContentDescription, tint = tint)
                }
            }
        }

        progress?.let {
            if (it == -1f) LinearProgressIndicator(Modifier.fillMaxWidth()) else {
                val animatedProgress by animateFloatAsState(
                    it, ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "DownloadProgressAnimation"
                )
                LinearProgressIndicator(animatedProgress, Modifier.fillMaxWidth())
            }
        }

        HorizontalDivider(Modifier.align(Alignment.BottomStart))
    }
}

@RequiresApi(VERSION_CODES.R)
@Composable
private fun rememberAllFilesPermissionState(context: Context) = remember(context) {
    AllFilesPermissionState(context)
}.also {
    // Refresh the permission status when the lifecycle is resumed
    PermissionLifecycleCheckerEffect(it)
}

/**
 * @see [com.google.accompanist.permissions.PermissionLifecycleCheckerEffect]
 */
@OptIn(ExperimentalPermissionsApi::class)
@RequiresApi(VERSION_CODES.R)
@Composable
private fun PermissionLifecycleCheckerEffect(
    permissionState: AllFilesPermissionState,
    lifecycleEvent: Lifecycle.Event = Lifecycle.Event.ON_RESUME,
) {
    // Check if the permission was granted when the lifecycle is resumed.
    // The user might've gone to the Settings screen and granted the permission.
    val observer = remember(permissionState) {
        LifecycleEventObserver { _, event ->
            if (event != lifecycleEvent) return@LifecycleEventObserver

            // If the permission is revoked, check again.
            // We don't check if the permission was denied as that triggers a process restart.
            if (permissionState.status != PermissionStatus.Granted) permissionState.refreshPermissionStatus()
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, observer) {
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@RequiresApi(VERSION_CODES.R)
private class AllFilesPermissionState(private val context: Context) : PermissionState {

    override val permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE
    override var status by mutableStateOf(permissionStatus)

    fun refreshPermissionStatus() {
        status = permissionStatus
    }

    override fun launchPermissionRequest() = try {
        // This opens up a screen where the user can grant "All files access" to the app.
        // Since there's no result/output, there's no callback to receive it.
        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))

        // Show a toast asking users what to do
        context.showToast(R.string.grant_all_files_access)
    } catch (e: ActivityNotFoundException) {
        logError(TAG, "Couldn't open MANAGE_ALL_FILES_ACCESS settings screen", e)
    }

    private val permissionStatus
        get() = if (Environment.isExternalStorageManager()) PermissionStatus.Granted else PermissionStatus.Denied(
            true // ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        )
}

@PreviewThemes
@Composable
fun PreviewUpdateAvailable() = PreviewAppTheme {
    UpdateAvailable(
        refreshing = false,
        updateData = """##Personalization
• Expands Omoji's functionality and library.

##Health
• Adds a new TalkBack feature that recognizes and announces images in apps and Photos.
• Adds the new Zen Space app, with two modes, Deep Zen and Light Zen, to help you focus on the present.
• Improves Simple mode with a new helper widget and quick tutorials on the Home screen.

##Gaming experience
• Adds the Championship mode to Game Assistant. This mode improves performance while also disabling notifications, calls, and other messages to give you a more immersive gaming experience.
• Adds a music playback control to Game Assistant, so you can listen to and control music easily while gaming.""".let { changelog ->
            UpdateData(
                id = 1,
                versionNumber = "KB2001_11_F.66",
                otaVersionNumber = "KB2001_11.F.66_2660_202305041648",
                changelog = changelog,
                description = """#KB2001_13.1.0.513(EX01)
##2023-05-15

A system update is available. The OxygenOS 13.1 update brings new Zen Space features, a new TalkBack feature to describe images, and better gaming performance and experience.

""" + changelog,
                downloadUrl = "https://gauss-componentotacostmanual-in.allawnofs.com/remove-a7779e2dc9b4b40458be6db38b226089/component-ota/23/03/15/4b70c7244ce7411994c97313e8ceb82d.zip",
                downloadSize = 4777312256,
                filename = "4b70c7244ce7411994c97313e8ceb82d.zip",
                md5sum = "0dc48e34ca895ae5653a32ef4daf2933",
                updateInformationAvailable = true,
                systemIsUpToDate = false,
            )
        },
        downloadStatus = NOT_DOWNLOADING,
        failureType = null,
        workProgress = null,
        forceDownloadErrorDialog = false,
        downloadAction = {},
    ) {}
}

private const val TAG = "UpdateInformationContent"
