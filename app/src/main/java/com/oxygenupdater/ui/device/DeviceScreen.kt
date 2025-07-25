package com.oxygenupdater.ui.device

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Build.UNKNOWN
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Aod
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.DeviceUnknown
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PermDeviceInformation
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SecurityUpdateGood
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Incremental
import com.oxygenupdater.internal.CpuFrequencies
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStart
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.common.modifierDefaultPaddingTop
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberState
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize
import com.oxygenupdater.utils.UpdateDataVersionFormatter
import com.oxygenupdater.utils.logInfo
import java.text.NumberFormat
import kotlin.math.roundToLong

val DefaultDeviceName = "${Build.MANUFACTURER} ${Build.DEVICE}"

@Composable
fun DeviceScreen(
    navType: NavType,
    windowWidthSize: WindowWidthSizeClass,
    deviceName: String,
    deviceOsSpec: DeviceOsSpec?,
    deviceMismatchStatus: Triple<Boolean, String, String>?,
) = if (windowWidthSize == WindowWidthSizeClass.Expanded) Row(modifierMaxWidth) {
    DeviceHeaderExpanded(
        navType = navType,
        deviceName = deviceName,
        deviceOsSpec = deviceOsSpec,
    )
    // TODO(compose/screens): movableContentOf, see https://github.com/android/user-interface-samples/blob/main/CanonicalLayouts/list-detail-compose/app/src/main/java/com/example/listdetailcompose/ui/ListDetail.kt

    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .testTag(DeviceScreen_ScrollableColumnTestTag)
    ) {
        DeviceMismatchStatus(status = deviceMismatchStatus)

        DeviceSoftwareInfo()
        HorizontalDivider(modifierDefaultPaddingTop)
        DeviceHardwareInfo()

        ConditionalNavBarPadding(navType)
    }
} else Column(
    Modifier
        .verticalScroll(rememberScrollState())
        .testTag(DeviceScreen_ScrollableColumnTestTag)
) {
    DeviceHeaderCompact(
        deviceName = deviceName,
        deviceOsSpec = deviceOsSpec,
        deviceMismatchStatus = deviceMismatchStatus,
    )

    DeviceSoftwareInfo()
    HorizontalDivider(modifierDefaultPaddingTop)
    DeviceHardwareInfo()

    ConditionalNavBarPadding(navType)
}

@Composable
private fun DeviceHeaderCompact(
    deviceName: String,
    deviceOsSpec: DeviceOsSpec?,
    deviceMismatchStatus: Triple<Boolean, String, String>?,
) {
    Row(modifierDefaultPaddingStartTopEnd) {
        DeviceImage(
            deviceName = deviceName,
            deviceOsSpec = deviceOsSpec,
            size = 128.dp,
        )

        Column(modifierDefaultPaddingStart.height(128.dp) /* same size as image */) {
            DeviceNameWithSpec(
                deviceName = deviceName,
                deviceOsSpec = deviceOsSpec,
            )
        }
    }

    HorizontalDivider()
    DeviceMismatchStatus(status = deviceMismatchStatus)
}

@Composable
private fun DeviceHeaderExpanded(
    navType: NavType,
    deviceName: String,
    deviceOsSpec: DeviceOsSpec?,
) = Column(Modifier.width(IntrinsicSize.Min) then modifierDefaultPaddingStartTopEnd) {
    DeviceImage(
        deviceName = deviceName,
        deviceOsSpec = deviceOsSpec,
        size = 192.dp,
    )

    Spacer(modifierDefaultPaddingTop)

    DeviceNameWithSpec(
        deviceName = deviceName,
        deviceOsSpec = deviceOsSpec,
    )

    ConditionalNavBarPadding(navType)
}

@Composable
private fun ColumnScope.DeviceNameWithSpec(
    deviceName: String,
    deviceOsSpec: DeviceOsSpec?,
) {
    Text(
        text = deviceName,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.testTag(DeviceScreen_NameTestTag)
    )

    SelectionContainer(Modifier.weight(1f)) {
        Text(
            text = Build.MODEL,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(DeviceScreen_ModelTestTag)
        )
    }

    Text(
        text = stringResource(remember(deviceOsSpec) {
            when (deviceOsSpec) {
                DeviceOsSpec.SupportedDeviceAndOs -> R.string.device_information_supported_oxygen_os
                DeviceOsSpec.CarrierExclusiveOxygenOs -> R.string.device_information_carrier_exclusive_oxygen_os
                DeviceOsSpec.UnsupportedDevice -> R.string.device_information_unsupported_oxygen_os
                DeviceOsSpec.UnsupportedDeviceAndOs -> R.string.device_information_unsupported_os
                else -> R.string.device_information_unsupported_os
            }
        }),
        color = if (deviceOsSpec == DeviceOsSpec.SupportedDeviceAndOs) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else MaterialTheme.colorScheme.error,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .testTag(DeviceScreen_SupportStatusTestTag)
    )
}

@Composable
fun DeviceMismatchStatus(status: Triple<Boolean, String, String>?) {
    if (status?.first != true) return

    Text(
        text = stringResource(
            R.string.incorrect_device_warning_message,
            status.second,
            status.third,
        ),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifierDefaultPadding.testTag(DeviceScreen_MismatchTextTestTag)
    )

    HorizontalDivider()
}

@Composable
private fun DeviceImage(deviceName: String, deviceOsSpec: DeviceOsSpec?, size: Dp) {
    val notSupported = deviceOsSpec != DeviceOsSpec.SupportedDeviceAndOs
    var showUnsupportedDialog by rememberState(false)
    if (notSupported) deviceOsSpec?.let {
        UnsupportedDeviceOsSpecDialog(showUnsupportedDialog, { showUnsupportedDialog = false }, it)
    }

    Box(Modifier.animatedClickable(notSupported) { showUnsupportedDialog = true }) {
        val requiredSizeModifier = Modifier.requiredSize(size)

        val context = LocalContext.current
        val defaultImage = rememberVectorPainter(
            when (deviceOsSpec) {
                DeviceOsSpec.SupportedDeviceAndOs -> Icons.Rounded.SecurityUpdateGood // device frame with tick mark
                DeviceOsSpec.CarrierExclusiveOxygenOs -> Icons.Rounded.ScreenLockPortrait // device frame with locked icon
                DeviceOsSpec.UnsupportedDevice -> Icons.Rounded.Aod // device frame with two horizontal lines
                // Covers UnsupportedDeviceAndOs
                else -> Icons.Rounded.DeviceUnknown // device frame with question mark
            }
        )
        AsyncImage(
            model = deviceName.let {
                val sizePx = LocalDensity.current.run { size.roundToPx() }
                remember(it, sizePx) {
                    ImageRequest.Builder(context)
                        .data(Device.constructImageUrl(it))
                        .size(sizePx)
                        .build()
                }
            },
            contentDescription = stringResource(R.string.device_information_image_description),
            placeholder = defaultImage,
            error = defaultImage,
            modifier = requiredSizeModifier.testTag(DeviceScreen_ImageTestTag)
        )

        if (notSupported) {
            Box(requiredSizeModifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .75f)))
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = stringResource(R.string.icon),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag(DeviceScreen_NotSupportedIconTestTag)
            )
        }
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
fun ColumnScope.DeviceSoftwareInfo(showHeader: Boolean = true) {
    if (showHeader) Header(R.string.device_information_software_header)

    Item(
        icon = Icons.Rounded.Android,
        titleResId = R.string.device_information_os_version,
        text = Build.VERSION.RELEASE,
    )

    SystemVersionProperties.osVersion.takeIf { it != UNKNOWN }?.let {
        Item(
            icon = Icons.Rounded.TripOrigin,
            titleResId = R.string.device_information_oxygen_os_version,
            text = UpdateDataVersionFormatter.getFormattedOsVersion(it),
        )
    }

    SystemVersionProperties.otaVersion.takeIf { it != UNKNOWN }?.let {
        Item(
            icon = Icons.Rounded.TripOrigin,
            titleResId = R.string.device_information_oxygen_os_ota_version,
            text = it
        )
    }

    Item(
        icon = CustomIcons.Incremental,
        titleResId = R.string.device_information_incremental_os_version,
        text = Build.VERSION.INCREMENTAL,
    )

    SystemVersionProperties.securityPatchDate.takeIf { it != UNKNOWN }?.let {
        Item(
            icon = Icons.Rounded.Security,
            titleResId = R.string.device_information_patch_level_version,
            text = it,
        )
    }
}

@Composable
@NonRestartableComposable
private fun Header(@StringRes textResId: Int) = Text(
    text = stringResource(textResId),
    color = MaterialTheme.colorScheme.primary,
    style = MaterialTheme.typography.bodySmall,
    modifier = modifierDefaultPaddingStartTopEnd
)

@Suppress("UnusedReceiverParameter")
@Composable
private fun ColumnScope.DeviceHardwareInfo() {
    Header(R.string.device_information_hardware_header)

    val context = LocalContext.current
    val bytes = remember(context) { Formatter.formatShortFileSize(context, getRamBytes(context)) }
    if (bytes.isNotEmpty()) Item(
        icon = Icons.Rounded.Memory,
        titleResId = R.string.device_information_amount_of_memory,
        text = bytes,
    )

    Item(
        icon = Icons.Rounded.DeveloperBoard,
        titleResId = R.string.device_information_system_on_a_chip,
        text = Build.BOARD.let { board ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val socManufacturer = Build.SOC_MANUFACTURER
                val socModel = Build.SOC_MODEL
                val validManufacturer = socManufacturer != UNKNOWN
                val validModel = socModel != UNKNOWN

                if (validManufacturer && validModel) {
                    "$socManufacturer $socModel ($board)"
                } else if (validManufacturer) {
                    "$socManufacturer ($board)"
                } else if (validModel) {
                    "$socModel ($board)"
                } else board
            } else board
        },
    )

    CpuFrequencies.let { cpuFrequencies ->
        if (cpuFrequencies.isEmpty()) return@let

        val locale = currentLocale()
        // Format according to locale (decimal vs comma)
        val formatted = remember(locale) {
            val instance = NumberFormat.getInstance(locale)
            var validCount = 0
            buildString {
                cpuFrequencies.forEach { frequency, count ->
                    val formatted = try {
                        context.getString(
                            R.string.device_information_gigahertz,
                            instance.format(frequency).ifEmpty { return@forEach }
                        )
                    } catch (e: Exception) {
                        return@forEach
                    }
                    append("• ").append(formatted)
                    append(" (").append(count).append(')')
                    appendLine()
                    validCount++
                }
            }.trimEnd().let {
                // Remove bullet prefix if single line (single-core or multi-core with the same GHz values)
                if (validCount == 1) it.removePrefix("• ") else it
            }
        }
        if (formatted.isNotEmpty()) Item(
            icon = Icons.Rounded.Speed,
            titleResId = R.string.device_information_cpu_frequency,
            text = formatted,
        )
    }

    @Suppress("DEPRECATION")
    @SuppressLint("HardwareIds")
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        // Not shown on Android 8/O & above because it requires too many permissions
        Item(
            icon = Icons.Rounded.PermDeviceInformation,
            titleResId = R.string.device_information_serial_number,
            text = Build.SERIAL,
        )
    }

    Spacer(modifierDefaultPaddingTop)
}

@Composable
@NonRestartableComposable
private fun Item(
    icon: ImageVector,
    @StringRes titleResId: Int,
    text: String,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifierDefaultPaddingStartTopEnd
) {
    Icon(icon, stringResource(R.string.icon), tint = MaterialTheme.colorScheme.primary)

    Column(modifierDefaultPaddingStart) {
        Text(stringResource(titleResId), style = MaterialTheme.typography.titleMedium)
        SelectionContainer {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun getRamBytes(context: Context) = try {
    val totalMem = ActivityManager.MemoryInfo().apply {
        context.getSystemService<ActivityManager>()?.getMemoryInfo(this)
    }.totalMem

    // Round up
    var approxRam = (totalMem / 1_000_000_000.toDouble()).roundToLong()
    // RAM can never be an odd number in OP devices
    if (approxRam % 2 == 1L) approxRam++

    // `mi.totalMem` is in bytes, but since we're using it in android.text.format.Formatter.formatShortFileSize,
    // we need to make sure to use SI units across all Android versions
    // The version check is required because `formatShortFileSize` uses SI units only in Oreo and above.
    // It uses IEC units pre-Oreo.
    approxRam * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 1_000_000_000L else 1_073_741_824
} catch (e: Exception) {
    logInfo("DeviceScreen", "Memory information is unavailable due to error", e)
    0L
}

private const val TAG = "DeviceScreen"

@VisibleForTesting
const val DeviceScreen_ScrollableColumnTestTag = TAG + "_ScrollableColumn"

@VisibleForTesting
const val DeviceScreen_NameTestTag = TAG + "_Name"

@VisibleForTesting
const val DeviceScreen_ModelTestTag = TAG + "_Model"

@VisibleForTesting
const val DeviceScreen_SupportStatusTestTag = TAG + "_SupportStatus"

@VisibleForTesting
const val DeviceScreen_ImageTestTag = TAG + "_Image"

@VisibleForTesting
const val DeviceScreen_NotSupportedIconTestTag = TAG + "_NotSupportedIcon"

@VisibleForTesting
const val DeviceScreen_MismatchTextTestTag = TAG + "_MismatchText"

@PreviewThemes
@Composable
fun PreviewDeviceScreen() = PreviewAppTheme {
    val name = DefaultDeviceName
    val windowWidthSize = PreviewWindowSize.widthSizeClass
    DeviceScreen(
        navType = NavType.from(windowWidthSize),
        windowWidthSize = windowWidthSize,
        deviceName = name,
        deviceOsSpec = DeviceOsSpec.SupportedDeviceAndOs,
        deviceMismatchStatus = Triple(false, name, name),
    )
}
