package com.oxygenupdater.ui.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Build.UNKNOWN
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PermDeviceInformation
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TripOrigin
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Incremental
import com.oxygenupdater.internal.DeviceInformationData
import com.oxygenupdater.internal.DeviceInformationData.cpuFrequency
import com.oxygenupdater.internal.DeviceInformationData.deviceManufacturer
import com.oxygenupdater.internal.DeviceInformationData.deviceName
import com.oxygenupdater.internal.DeviceInformationData.serialNumber
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStart
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.common.modifierDefaultPaddingTop
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.UpdateDataVersionFormatter
import java.text.NumberFormat
import kotlin.math.roundToLong

fun defaultDeviceName() = "$deviceManufacturer $deviceName"

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
    ) {
        DeviceMismatchStatus(status = deviceMismatchStatus)

        DeviceSoftwareInfo()
        ItemDivider(modifierDefaultPaddingTop)
        DeviceHardwareInfo(navType = navType)
    }
} else Column(Modifier.verticalScroll(rememberScrollState())) {
    DeviceHeaderCompact(
        deviceName = deviceName,
        deviceOsSpec = deviceOsSpec,
        deviceMismatchStatus = deviceMismatchStatus,
    )

    DeviceSoftwareInfo()
    ItemDivider(modifierDefaultPaddingTop)
    DeviceHardwareInfo(navType = navType)
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
                modifier = Modifier.weight(1f)
            )
        }
    }

    ItemDivider()
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
private fun DeviceNameWithSpec(
    deviceName: String,
    deviceOsSpec: DeviceOsSpec?,
    modifier: Modifier = Modifier,
) {
    Text(deviceName, style = MaterialTheme.typography.titleLarge)
    SelectionContainer(modifier) {
        Text(
            text = DeviceInformationData.model,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    Text(
        text = stringResource(remember(deviceOsSpec) {
            when (deviceOsSpec) {
                DeviceOsSpec.SupportedOxygenOs -> R.string.device_information_supported_oxygen_os
                DeviceOsSpec.CarrierExclusiveOxygenOs -> R.string.device_information_carrier_exclusive_oxygen_os
                DeviceOsSpec.UnsupportedOxygenOs -> R.string.device_information_unsupported_oxygen_os
                DeviceOsSpec.UnsupportedOs -> R.string.device_information_unsupported_os
                else -> R.string.device_information_unsupported_os
            }
        }),
        color = if (deviceOsSpec == DeviceOsSpec.SupportedOxygenOs) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else MaterialTheme.colorScheme.error,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 8.dp)
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
        modifier = modifierDefaultPadding
    )

    ItemDivider()
}

@Composable
private fun DeviceImage(deviceName: String, deviceOsSpec: DeviceOsSpec?, size: Dp) {
    val notSupported = deviceOsSpec != DeviceOsSpec.SupportedOxygenOs
    var showUnsupportedDialog by rememberSaveableState("showUnsupportedDialog", false)
    if (notSupported) deviceOsSpec?.let {
        UnsupportedDeviceOsSpecDialog(showUnsupportedDialog, { showUnsupportedDialog = false }, it)
    }

    Box(Modifier.animatedClickable(notSupported) { showUnsupportedDialog = true }) {
        val requiredSizeModifier = Modifier.requiredSize(size)

        val context = LocalContext.current
        val defaultImage = painterResource(R.drawable.oneplus7pro)
        AsyncImage(
            model = deviceName.let {
                val size = LocalDensity.current.run { size.roundToPx() }
                remember(it, size) {
                    ImageRequest.Builder(context)
                        .data(Device.constructImageUrl(it))
                        .size(size)
                        .build()
                }
            },
            contentDescription = stringResource(R.string.device_information_image_description),
            placeholder = defaultImage,
            error = defaultImage,
            modifier = requiredSizeModifier
        )

        if (notSupported) {
            Box(requiredSizeModifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .75f)))
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = stringResource(R.string.icon),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun DeviceSoftwareInfo(showHeader: Boolean = true) {
    if (showHeader) Header(R.string.device_information_software_header)

    Item(
        icon = Icons.Rounded.Android,
        titleResId = R.string.device_information_os_version,
        text = DeviceInformationData.osVersion,
    )

    SystemVersionProperties.oxygenOSVersion.takeIf { it != UNKNOWN }?.let {
        Item(
            icon = Icons.Rounded.TripOrigin,
            titleResId = R.string.device_information_oxygen_os_version,
            text = UpdateDataVersionFormatter.getFormattedOxygenOsVersion(it),
        )
    }

    SystemVersionProperties.oxygenOSOTAVersion.takeIf { it != UNKNOWN }?.let {
        Item(
            icon = Icons.Rounded.TripOrigin,
            titleResId = R.string.device_information_oxygen_os_ota_version,
            text = it
        )
    }

    Item(
        icon = CustomIcons.Incremental,
        titleResId = R.string.device_information_incremental_os_version,
        text = DeviceInformationData.incrementalOsVersion,
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

@Composable
private fun DeviceHardwareInfo(navType: NavType) {
    Header(R.string.device_information_hardware_header)

    val context = LocalContext.current
    val bytes = if (LocalInspectionMode.current) 1L else remember { getRamBytes(context) }
    if (bytes != 0L) Item(
        icon = Icons.Rounded.Memory,
        titleResId = R.string.device_information_amount_of_memory,
        text = Formatter.formatShortFileSize(context, bytes),
    )

    Item(
        icon = Icons.Rounded.DeveloperBoard,
        titleResId = R.string.device_information_system_on_a_chip,
        text = DeviceInformationData.soc,
    )

    if (cpuFrequency != null) {
        val locale = currentLocale()
        // Format according to locale (decimal vs comma)
        val formatted = remember(locale) {
            try {
                NumberFormat.getInstance(locale).format(cpuFrequency)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        if (formatted != null) Item(
            icon = Icons.Rounded.Speed,
            titleResId = R.string.device_information_cpu_frequency,
            text = stringResource(R.string.device_information_gigahertz, formatted),
        )
    }

    // Serial number (Android 7.1.2 and lower only)
    if (serialNumber != null) Item(
        icon = Icons.Rounded.PermDeviceInformation,
        titleResId = R.string.device_information_serial_number,
        text = serialNumber,
    )

    Spacer(modifierDefaultPaddingTop)
    ConditionalNavBarPadding(navType)
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
    logWarning("DeviceHardwareInfo", "Memory information is unavailable due to error", e)
    0L
}

@PreviewThemes
@Composable
fun PreviewDeviceScreen() = PreviewAppTheme {
    val name = defaultDeviceName()
    val windowWidthSize = PreviewWindowSize.widthSizeClass
    DeviceScreen(
        navType = NavType.from(windowWidthSize),
        windowWidthSize = windowWidthSize,
        deviceName = name,
        deviceOsSpec = DeviceOsSpec.SupportedOxygenOs,
        deviceMismatchStatus = Triple(false, name, name),
    )
}
