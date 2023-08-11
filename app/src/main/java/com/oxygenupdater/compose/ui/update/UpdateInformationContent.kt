package com.oxygenupdater.compose.ui.update

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.Data
import androidx.work.WorkInfo
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.oxygenupdater.R
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.Info
import com.oxygenupdater.compose.ui.common.IconText
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.OutlinedIconButton
import com.oxygenupdater.compose.ui.common.RichText
import com.oxygenupdater.compose.ui.common.RichTextType
import com.oxygenupdater.compose.ui.common.animatedClickable
import com.oxygenupdater.compose.ui.common.rememberCallback
import com.oxygenupdater.compose.ui.common.withPlaceholder
import com.oxygenupdater.compose.ui.device.DeviceSoftwareInfo
import com.oxygenupdater.compose.ui.device.defaultDeviceName
import com.oxygenupdater.compose.ui.dialogs.AlreadyDownloadedSheet
import com.oxygenupdater.compose.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.compose.ui.dialogs.SheetType
import com.oxygenupdater.compose.ui.dialogs.defaultModalBottomSheetState
import com.oxygenupdater.compose.ui.onboarding.NOT_SET
import com.oxygenupdater.compose.ui.onboarding.NOT_SET_L
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.compose.ui.theme.backgroundVariant
import com.oxygenupdater.compose.ui.theme.positive
import com.oxygenupdater.compose.ui.update.DownloadStatus.DOWNLOADING
import com.oxygenupdater.compose.ui.update.DownloadStatus.DOWNLOAD_COMPLETED
import com.oxygenupdater.compose.ui.update.DownloadStatus.DOWNLOAD_FAILED
import com.oxygenupdater.compose.ui.update.DownloadStatus.DOWNLOAD_PAUSED
import com.oxygenupdater.compose.ui.update.DownloadStatus.DOWNLOAD_QUEUED
import com.oxygenupdater.compose.ui.update.DownloadStatus.NOT_DOWNLOADING
import com.oxygenupdater.compose.ui.update.DownloadStatus.VERIFICATION_COMPLETED
import com.oxygenupdater.compose.ui.update.DownloadStatus.VERIFICATION_FAILED
import com.oxygenupdater.compose.ui.update.DownloadStatus.VERIFYING
import com.oxygenupdater.extensions.formatFileSize
import com.oxygenupdater.extensions.startInstallActivity
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.utils.LocalNotifications
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logInfo
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.UpdateDataVersionFormatter
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_BYTES_DONE
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_ETA
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_FAILURE_TYPE
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_PROGRESS
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_TOTAL_BYTES
import java.io.IOException
import java.util.UUID

/** Used for maintaining progress text across DOWNLOADING -> PAUSED */
private var previousProgressText: String? = null
private var previousProgress: Float? = null

@OptIn(ExperimentalAnimationGraphicsApi::class, ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailable(
    refreshing: Boolean,
    updateData: UpdateData,
    workInfo: WorkInfo?,
    downloadStatus: DownloadStatus,
    forceDownloadErrorDialog: Boolean,
    downloadAction: (DownloadAction) -> Unit,
    logDownloadError: (Data) -> Unit,
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

        val caption = MaterialTheme.typography.bodySmall
        if (updateData.systemIsUpToDate) {
            // Remind user why they're seeing this update
            val updateMethod = PrefManager.getString(PrefManager.PROPERTY_UPDATE_METHOD, "<UNKNOWN>") ?: "<UNKNOWN>"
            Text(
                stringResource(R.string.update_information_header_advanced_mode_helper, updateMethod),
                Modifier.padding(horizontal = 16.dp),
                MaterialTheme.colorScheme.onSurfaceVariant,
                style = caption
            )

            ItemDivider(Modifier.padding(vertical = 16.dp))
        }

        SelectionContainer(Modifier.padding(start = 16.dp, end = 8.dp)) {
            Text(
                stringResource(R.string.update_information_file_name, updateData.filename ?: Build.UNKNOWN),
                Modifier.withPlaceholder(refreshing),
                MaterialTheme.colorScheme.onSurfaceVariant,
                style = caption
            )
        }

        // Needs to be a separate container to avoid losing Column context
        SelectionContainer(Modifier.padding(start = 16.dp, end = 8.dp)) {
            Text(
                stringResource(R.string.update_information_md5, updateData.mD5Sum ?: Build.UNKNOWN),
                Modifier.withPlaceholder(refreshing),
                MaterialTheme.colorScheme.onSurfaceVariant,
                style = caption
            )
        }

        val outputData = workInfo?.outputData
        if (downloadStatus == VERIFICATION_FAILED
            || (downloadStatus == DOWNLOAD_FAILED && outputData?.getInt(WORK_DATA_DOWNLOAD_FAILURE_TYPE, NOT_SET).let {
                // Note: show download link only if failure is any of these:
                // - NULL_UPDATE_DATA_OR_DOWNLOAD_URL
                // - DOWNLOAD_URL_INVALID_SCHEME
                // - SERVER_ERROR
                // - CONNECTION_ERROR
                // - UNSUCCESSFUL_RESPONSE
                // - UNKNOWN
                // For simplicity, we're negating boolean logic by checking for the only other possible values:
                // null and COULD_NOT_MOVE_TEMP_FILE. This will need to be adjusted later if new failures are added.
                it != null && it != NOT_SET && it != DownloadFailure.CouldNotMoveTempFile.value
            })
        ) {
            val url = updateData.downloadUrl ?: "null"
            val text = stringResource(R.string.update_information_download_link, url)
            val length = text.length
            @OptIn(ExperimentalTextApi::class)
            @Suppress("NAME_SHADOWING")
            RichText(
                text,
                Modifier
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                type = RichTextType.Custom
            ) { text, contentColor, urlColor ->
                AnnotatedString.Builder(length).apply {
                    // Global
                    append(text)
                    addStyle(caption.toSpanStyle().copy(color = contentColor), 0, length)
                    addStyle(caption.toParagraphStyle(), 0, length)

                    // For download URL
                    val start = text.indexOf('\n') + 1
                    addUrlAnnotation(UrlAnnotation(url), start, length)
                    addStyle(SpanStyle(color = urlColor, textDecoration = TextDecoration.Underline), start, length)
                }.toAnnotatedString()
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val runningInPreview = LocalInspectionMode.current

    @Suppress("IMPLICIT_CAST_TO_ANY")
    val downloadPermissionState = if (runningInPreview) Unit
    else if (SDK_INT >= VERSION_CODES.R) rememberAllFilesPermissionState(context) else rememberMultiplePermissionsState(
        listOf(VERIFY_FILE_PERMISSION, DOWNLOAD_FILE_PERMISSION)
    )

    val hasDownloadPermissions = if (SDK_INT >= VERSION_CODES.R && downloadPermissionState is AllFilesPermission) {
        downloadPermissionState.status.isGranted
    } else if (downloadPermissionState is MultiplePermissionsState) downloadPermissionState.allPermissionsGranted else {
        // If state is somehow something else, we can't check if permissions are granted.
        // So, make an educated guess: on API < 23 both READ & WRITE permissions
        // are granted automatically, while on API >= 23 only READ is granted
        // automatically. A simple SDK version check should suffice.
        SDK_INT <= VERSION_CODES.M
    }

    val requestDownloadPermissions = remember(downloadPermissionState) {
        {
            if (SDK_INT >= VERSION_CODES.R && downloadPermissionState is AllFilesPermission) {
                downloadPermissionState.launchPermissionRequest()
            } else if (downloadPermissionState is MultiplePermissionsState) {
                downloadPermissionState.launchMultiplePermissionRequest()
            }
        }
    }

    val sheetState = defaultModalBottomSheetState()
    var sheetType by remember { mutableStateOf(SheetType.None) }
    val hide = rememberCallback { sheetType = SheetType.None }
    BackHandler(sheetState.isVisible, hide)

    if (sheetType != SheetType.None) ModalBottomSheet(hide, sheetState) {
        when (sheetType) {
            SheetType.AlreadyDownloaded -> AlreadyDownloadedSheet(hide) {
                if (it) context.startInstallActivity()
                else if (hasDownloadPermissions) downloadAction(DownloadAction.Delete)
                else requestDownloadPermissions()
            }

            else -> {}
        }
    }

    // Since params are set automatically (i.e. without user action), we need an extra flag to control
    // when dialog can be shown, to avoid infinitely re-showing it on every state change.
    var canShowDownloadErrorDialog by remember { mutableStateOf(forceDownloadErrorDialog) }
    val (downloadErrorDialogParams, setDownloadErrorParams) = remember {
        mutableStateOf<DownloadErrorParams?>(null)
    }

    // TODO(compose/update): make it a BottomSheet
    if (canShowDownloadErrorDialog && downloadErrorDialogParams != null) DownloadErrorDialog({
        canShowDownloadErrorDialog = false
        setDownloadErrorParams(null)
    }, downloadErrorDialogParams)

    val (manageStorageDialogData, setManageStorageData) = remember {
        mutableStateOf<Pair<UUID, Long>?>(null)
    }

    // TODO(compose/update): make it a BottomSheet
    if (SDK_INT >= VERSION_CODES.O && manageStorageDialogData != null) ManageStorageDialog({
        setManageStorageData(null)
    }, manageStorageDialogData, downloadAction, onCancel = {
        setDownloadErrorParams(DownloadErrorParams(context.getString(R.string.download_error_storage)))
    })

    val titleResId: Int
    val details: String?
    val sizeOrProgressText: String
    val progress: Float? // null => hidden, 0 => indeterminate
    val actionButtonConfig: Triple<Boolean, ImageVector, Color>?
    val onDownloadClick: (() -> Unit)?
    val downloadSize = updateData.downloadSize
    val enqueueIfSpaceAvailable = if (SDK_INT >= VERSION_CODES.O) remember(context, downloadSize) {
        label@{
            val externalFilesDir = context.getExternalFilesDir(null) ?: return@label
            val storageManager = context.getSystemService<StorageManager>() ?: return@label
            val appSpecificExternalDirUuid = storageManager.getUuidForPath(externalFilesDir)
            canShowDownloadErrorDialog = true

            // Get max bytes that can be allocated by the system to the app
            // This value is usually larger than [File.usableSpace], because deletable cached files are also considered
            val allocatableBytes = storageManager.getAllocatableBytes(appSpecificExternalDirUuid) - SAFE_MARGIN
            if (downloadSize <= allocatableBytes) try {
                // Allocate bytes: the system will delete cached files if necessary to fulfil this request,
                // or throw an IOException if it fails for whatever reason.
                storageManager.allocateBytes(appSpecificExternalDirUuid, downloadSize)

                // Enqueue download work since the required space has been freed up
                downloadAction(DownloadAction.Enqueue)
            } catch (e: IOException) {
                // Request the user to free up space manually because the system couldn't do it automatically
                setManageStorageData(appSpecificExternalDirUuid to downloadSize + SAFE_MARGIN)
            } else setManageStorageData(appSpecificExternalDirUuid to downloadSize - allocatableBytes)
        }
    } else remember(context, downloadSize) {
        label@{
            val externalFilesDir = context.getExternalFilesDir(null) ?: return@label
            canShowDownloadErrorDialog = true

            // Check if there is enough free storage space before downloading
            val usableBytes = externalFilesDir.usableSpace - SAFE_MARGIN
            if (downloadSize <= usableBytes) downloadAction(DownloadAction.Enqueue)
            else {
                // Don't have enough space to complete the download. Display a notification and an error dialog to the user.
                LocalNotifications.showDownloadFailedNotification(
                    context,
                    false,
                    R.string.download_error_storage,
                    R.string.download_notification_error_storage_full
                )

                setDownloadErrorParams(DownloadErrorParams(context.getString(R.string.download_error_storage)))
            }
        }
    }

    when (downloadStatus) {
        NOT_DOWNLOADING -> {
            val outputData = workInfo?.outputData
            val failureType = outputData?.getInt(WORK_DATA_DOWNLOAD_FAILURE_TYPE, NOT_SET)
            if (failureType != null && failureType != NOT_SET) when (failureType) {
                DownloadFailure.ServerError.value,
                DownloadFailure.ConnectionError.value,
                -> setDownloadErrorParams(DownloadErrorParams(
                    stringResource(R.string.download_error_server)
                ) { resumable ->
                    if (!resumable) downloadAction(DownloadAction.Delete)
                    enqueueIfSpaceAvailable()
                })

                DownloadFailure.UnsuccessfulResponse.value -> setDownloadErrorParams(
                    DownloadErrorParams(stringResource(R.string.download_error_unsuccessful_response), RichTextType.Html)
                ).also { logDownloadError(outputData) }

                DownloadFailure.NullUpdateDataOrDownloadUrl.value,
                DownloadFailure.DownloadUrlInvalidScheme.value,
                DownloadFailure.Unknown.value,
                -> setDownloadErrorParams(DownloadErrorParams(
                    stringResource(R.string.download_error_internal)
                ) { resumable ->
                    if (!resumable) downloadAction(DownloadAction.Delete)
                    enqueueIfSpaceAvailable()
                })

                DownloadFailure.CouldNotMoveTempFile.value -> setDownloadErrorParams(
                    DownloadErrorParams(
                        stringResource(
                            R.string.download_error_could_not_move_temp_file,
                            "Android/data/${context.packageName}/files"
                        )
                    )
                )

                else -> {}
            }

            titleResId = R.string.download
            details = null
            sizeOrProgressText = context.formatFileSize(downloadSize)
            progress = null
            actionButtonConfig = null
            onDownloadClick = if (hasDownloadPermissions) if (updateData.downloadUrl?.startsWith("http") == true) ({
                enqueueIfSpaceAvailable()
            }) else null else requestDownloadPermissions().let { null }
        }

        DOWNLOAD_FAILED -> {
            titleResId = R.string.download
            details = stringResource(R.string.download_failed)
            sizeOrProgressText = context.formatFileSize(downloadSize)
            progress = null
            actionButtonConfig = null
            onDownloadClick = null
        }

        DOWNLOAD_QUEUED, DOWNLOADING -> {
            titleResId = R.string.downloading

            val bytesDone: Long
            val totalBytes: Long
            val currentProgress: Int
            val workProgress = workInfo?.progress
            details = if (workProgress != null) workProgress.run {
                bytesDone = getLong(WORK_DATA_DOWNLOAD_BYTES_DONE, NOT_SET_L)
                totalBytes = getLong(WORK_DATA_DOWNLOAD_TOTAL_BYTES, NOT_SET_L)
                currentProgress = getInt(WORK_DATA_DOWNLOAD_PROGRESS, 0)
                getString(WORK_DATA_DOWNLOAD_ETA) ?: stringResource(R.string.summary_please_wait)
            } else {
                bytesDone = NOT_SET_L
                totalBytes = NOT_SET_L
                currentProgress = 0
                stringResource(R.string.summary_please_wait)
            }

            sizeOrProgressText = if (bytesDone != NOT_SET_L && totalBytes != NOT_SET_L) {
                val bytesDoneStr = context.formatFileSize(bytesDone)
                val totalBytesStr = context.formatFileSize(totalBytes)
                "$bytesDoneStr / $totalBytesStr ($currentProgress%)"
            } else previousProgressText ?: context.formatFileSize(downloadSize)
            previousProgressText = sizeOrProgressText

            progress = if (currentProgress == 0) 0f else currentProgress / 100f
            previousProgress = progress

            actionButtonConfig = Triple(true, Icons.Rounded.Close, colorScheme.error)
            onDownloadClick = { downloadAction(DownloadAction.Pause) }
        }

        DOWNLOAD_PAUSED -> {
            titleResId = R.string.paused
            details = stringResource(R.string.download_progress_text_paused)
            sizeOrProgressText = previousProgressText ?: context.formatFileSize(downloadSize)
            progress = previousProgress
            actionButtonConfig = Triple(true, Icons.Rounded.Close, colorScheme.error)
            onDownloadClick = if (hasDownloadPermissions) ({
                enqueueIfSpaceAvailable()
            }) else requestDownloadPermissions().let { null }
        }

        DOWNLOAD_COMPLETED, VERIFICATION_COMPLETED -> {
            titleResId = R.string.downloaded
            details = null
            sizeOrProgressText = context.formatFileSize(downloadSize)
            progress = null
            actionButtonConfig = Triple(false, Icons.Rounded.Launch, colorScheme.primary)
            onDownloadClick = { sheetType = SheetType.AlreadyDownloaded }

            // Open install guide automatically, but only after the normal download flow completes
            LaunchedEffect(Unit) {
                if (previousProgress != null) context.startInstallActivity()
            }
        }

        VERIFYING -> {
            titleResId = R.string.download_verifying
            details = stringResource(R.string.download_progress_text_verifying)
            sizeOrProgressText = context.formatFileSize(downloadSize)
            progress = 0f
            actionButtonConfig = null
            onDownloadClick = null
        }

        VERIFICATION_FAILED -> {
            titleResId = R.string.download_verifying_error
            details = stringResource(R.string.download_notification_error_corrupt)
            sizeOrProgressText = context.formatFileSize(downloadSize)
            progress = null
            actionButtonConfig = null
            onDownloadClick = if (hasDownloadPermissions) ({
                enqueueIfSpaceAvailable()
            }) else requestDownloadPermissions().let { null }

            setDownloadErrorParams(DownloadErrorParams(context.getString(R.string.download_error_corrupt)) {
                canShowDownloadErrorDialog = true
                onDownloadClick?.invoke()
            })
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .background(colorScheme.backgroundVariant)
            .animatedClickable(onDownloadClick != null, onDownloadClick)
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
                // Title
                Text(stringResource(titleResId), style = MaterialTheme.typography.bodyMedium)
                // Size + progress%
                Text(
                    sizeOrProgressText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (details != null) Text(details, style = MaterialTheme.typography.bodyMedium)
            }

            actionButtonConfig?.let { (forCancel, icon, tint) ->
                IconButton({
                    if (forCancel) downloadAction(DownloadAction.Cancel)
                    else context.startInstallActivity()
                }, Modifier.requiredSize(56.dp)) {
                    Icon(icon, iconContentDescription, tint = tint)
                }
            }
        }

        if (titleResId != R.string.paused) progress?.let {
            if (it == 0f) LinearProgressIndicator(Modifier.fillMaxWidth()) else {
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

@Composable
fun UpToDate(
    refreshing: Boolean,
    updateData: UpdateData,
) = Column(Modifier.verticalScroll(rememberScrollState())) {
    val currentOtaVersion = SystemVersionProperties.oxygenOSOTAVersion
    val method = PrefManager.getString(PrefManager.PROPERTY_UPDATE_METHOD, "") ?: ""

    val incomingOtaVersion = updateData.otaVersionNumber
    val isDifferentVersion = incomingOtaVersion != currentOtaVersion
    val showAdvancedModeTip = isDifferentVersion // show advanced mode hint only if OTA versions don't match…
            // …and incoming is newer (older builds can't be installed due to standard Android security measures)
            && UpdateData.getBuildDate(incomingOtaVersion) >= UpdateData.getBuildDate(currentOtaVersion)
            // …and incoming is likely a full ZIP, or the selected update method is "full"
            // (incrementals are only for specific source/target version combos)
            && (updateData.downloadSize >= FULL_ZIP_LIKELY_MIN_SIZE || method.endsWith("(full)") || method.endsWith("(volledig)"))
    if (showAdvancedModeTip) {
        IconText(
            Modifier.padding(16.dp),
            icon = CustomIcons.Info,
            text = stringResource(R.string.update_information_banner_advanced_mode_tip)
        )
        ItemDivider()
    }

    Box(Modifier.fillMaxWidth()) {
        val positive = MaterialTheme.colorScheme.positive
        IconText(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            icon = Icons.Rounded.CheckCircleOutline,
            text = stringResource(R.string.update_information_system_is_up_to_date),
            iconTint = positive,
            style = MaterialTheme.typography.titleMedium.copy(color = positive)
        )

        Icon(
            Icons.Rounded.DoneAll, stringResource(R.string.icon),
            Modifier
                .graphicsLayer(scaleX = 2f, scaleY = 2f, alpha = .1f)
                .align(Alignment.CenterEnd)
                .requiredSize(64.dp)
        )
    }

    ItemDivider(Modifier.padding(top = 2.dp))
    DeviceSoftwareInfo(false)
    ItemDivider(Modifier.padding(top = 16.dp))

    val runningInPreview = LocalInspectionMode.current
    val expandEnabled = updateData.isUpdateInformationAvailable
    var expanded by remember { mutableStateOf(runningInPreview) }
    IconText(
        Modifier
            .fillMaxWidth()
            .animatedClickable(expandEnabled) { expanded = !expanded }
            .padding(16.dp) // must be after `clickable`
            .withPlaceholder(refreshing),
        icon = if (!expandEnabled) Icons.Rounded.ErrorOutline else if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
        text = stringResource(
            if (!expandEnabled) R.string.update_information_no_update_data_available
            else R.string.update_information_view_update_information
        ),
        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary)
    )

    AnimatedVisibility(
        expanded,
        enter = remember {
            expandVertically(
                spring(visibilityThreshold = IntSize.VisibilityThreshold)
            ) + fadeIn(initialAlpha = .3f)
        },
        exit = remember {
            shrinkVertically(
                spring(visibilityThreshold = IntSize.VisibilityThreshold)
            ) + fadeOut()
        },
    ) {
        val changelogModifier = Modifier
            .padding(start = 20.dp, end = 16.dp, bottom = 16.dp)
            .withPlaceholder(refreshing)
        if (isDifferentVersion) Column {
            Text(
                stringResource(
                    R.string.update_information_different_version_changelog_notice_base,
                    UpdateDataVersionFormatter.getFormattedVersionNumber(updateData),
                    PrefManager.getString(PrefManager.PROPERTY_UPDATE_METHOD, "<UNKNOWN>") ?: "<UNKNOWN>"
                ) + if (showAdvancedModeTip) stringResource(R.string.update_information_different_version_changelog_notice_advanced) else "",
                Modifier
                    .padding(start = 20.dp, end = 16.dp, bottom = 8.dp)
                    .withPlaceholder(refreshing),
                MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            updateData.Changelog(changelogModifier)
        } else updateData.Changelog(changelogModifier)
    }

    ItemDivider()
}

@Composable
private fun UpdateData.Changelog(
    modifier: Modifier = Modifier,
    extraTextPadding: Boolean = false,
) = if (!changelog.isNullOrBlank() && !description.isNullOrBlank()) RichText(
    description, modifier, type = RichTextType.Markdown
) else Text(
    stringResource(R.string.update_information_description_not_available),
    if (extraTextPadding) modifier.padding(top = 16.dp) else modifier,
    style = MaterialTheme.typography.bodyMedium
)

@Immutable
data class DownloadErrorParams(
    val text: String,
    val type: RichTextType? = null,
    val resumable: Boolean = false,
    val callback: ((Boolean) -> Unit)? = null,
)

@Composable
fun DownloadErrorDialog(
    hide: () -> Unit,
    params: DownloadErrorParams,
) {
    val (text, type, resumable, callback) = params
    AlertDialog(hide, confirmButton = {
        if (callback == null) return@AlertDialog
        val icon = if (resumable) Icons.Rounded.Download else Icons.Rounded.Autorenew
        val resId = if (resumable) R.string.download_error_resume else R.string.download_error_retry
        OutlinedIconButton({
            LocalNotifications.hideDownloadCompleteNotification()
            hide()
            callback(resumable) // must be after `hide` so that the extra flag works correctly
        }, icon, resId)
    }, dismissButton = {
        TextButton({
            LocalNotifications.hideDownloadCompleteNotification()
            hide()
        }, colors = textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text(stringResource(R.string.download_error_close))
        }
    }, title = {
        Text(stringResource(R.string.download_error))
    }, text = {
        if (type == null) Text(text) else RichText(text, type = type)
    })
}

/**
 * Shows a dialog to the user asking them to free up some space, so that the app can download the OTA ZIP successfully.
 * If the user confirms this request, [StorageManager.ACTION_MANAGE_STORAGE] is launched which shows a system-supplied
 * "Remove items" UI (API 26+) that guides the user on clearing up the required storage space.
 */
@RequiresApi(VERSION_CODES.O)
@Composable
private fun ManageStorageDialog(
    hide: () -> Unit,
    pair: Pair<UUID, Long>?,
    downloadAction: (DownloadAction) -> Unit,
    onCancel: () -> Unit,
) = AlertDialog(hide, confirmButton = {
    val launcher = rememberLauncherForActivityResult(StartActivityForResult()) {
        when (it.resultCode) {
            // Enqueue if required space has been freed up
            Activity.RESULT_OK -> downloadAction(DownloadAction.Enqueue).also {
                logInfo(TAG, "User freed-up space")
            }

            Activity.RESULT_CANCELED -> onCancel().also {
                logInfo(TAG, "User didn't free-up space")
            }

            else -> logWarning(TAG, "Unhandled resultCode: ${it.resultCode}")
        }
    }
    OutlinedIconButton({
        val (uuid, bytes) = pair ?: return@OutlinedIconButton
        val intent = Intent(StorageManager.ACTION_MANAGE_STORAGE)
            .putExtra(StorageManager.EXTRA_UUID, uuid)
            .putExtra(StorageManager.EXTRA_REQUESTED_BYTES, bytes)
        launcher.launch(intent)
        hide()
    }, Icons.Rounded.Launch, android.R.string.ok)
}, dismissButton = {
    TextButton(hide, colors = textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
        Text(stringResource(R.string.download_error_close))
    }
}, title = {
    Text(stringResource(R.string.download_notification_error_storage_full))
}, text = {
    Text(stringResource(R.string.download_error_storage))
})

@RequiresApi(VERSION_CODES.R)
@Composable
private fun rememberAllFilesPermissionState(context: Context) = remember(context) {
    AllFilesPermission(context)
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
    permissionState: AllFilesPermission,
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
private class AllFilesPermission(private val context: Context) : PermissionState {

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
        Toast.makeText(context, R.string.grant_all_files_access, Toast.LENGTH_LONG).show()
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
fun PreviewUpToDate() = PreviewAppTheme {
    UpToDate(
        refreshing = false,
        updateData = """##Personalization
• Expands Omoji's functionality and library

##Health
• Adds a new TalkBack feature that recognizes and announces images in apps and Photos
• Adds the new Zen Space app, with two modes, Deep Zen and Light Zen, to help you focus on the present
• Improves Simple mode with a new helper widget and quick tutorials on the Home screen

##Gaming experience
• Adds the Championship mode to Game Assistant. This mode improves performance while also disabling notifications, calls, and other messages to give you a more immersive gaming experience
• Adds a music playback control to Game Assistant, so you can listen to and control music easily while gaming""".let { changelog ->
            UpdateData(
                id = 1,
                versionNumber = "KB2001_11_F.66",
                otaVersionNumber = "KB2001_11.F.66_2660_202305041648",
                changelog = changelog,
                description = """#KB2001_13.1.0.513(EX01)
##2023-06-10

A system update is available. The OxygenOS 13.1 update brings new Zen Space features, a new TalkBack feature to describe images, and better gaming performance and experience.

""" + changelog,
                downloadUrl = "https://gauss-componentotacostmanual-in.allawnofs.com/remove-a7779e2dc9b4b40458be6db38b226089/component-ota/23/03/15/4b70c7244ce7411994c97313e8ceb82d.zip",
                downloadSize = 4777312256,
                filename = "4b70c7244ce7411994c97313e8ceb82d.zip",
                mD5Sum = "0dc48e34ca895ae5653a32ef4daf2933",
                updateInformationAvailable = false,
                systemIsUpToDate = true,
            )
        },
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
                mD5Sum = "0dc48e34ca895ae5653a32ef4daf2933",
                updateInformationAvailable = true,
                systemIsUpToDate = false,
            )
        },
        workInfo = null,
        downloadStatus = NOT_DOWNLOADING,
        forceDownloadErrorDialog = false,
        downloadAction = {},
        logDownloadError = {},
    )
}

private const val TAG = "UpdateInformationContent"

/** Amount of free storage space to reserve when downloading an update */
private const val SAFE_MARGIN = 1048576 * 25L // 25 MB

private const val FULL_ZIP_LIKELY_MIN_SIZE = 1048576 * 2000L // 2 GB
private const val DOWNLOAD_FILE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
private const val VERIFY_FILE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
