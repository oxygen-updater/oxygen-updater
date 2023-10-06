package com.oxygenupdater.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.extensions.startMainActivity
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Image
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.CollapsingAppBar
import com.oxygenupdater.ui.TopAppBar
import com.oxygenupdater.ui.common.rememberTypedCallback
import com.oxygenupdater.ui.onboarding.OnboardingScreen
import com.oxygenupdater.ui.settings.SettingsViewModel
import com.oxygenupdater.ui.theme.AppTheme
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.hasRootAccess
import org.koin.androidx.viewmodel.ext.android.viewModel

class OnboardingActivity : BaseActivity() {

    private val viewModel by viewModel<SettingsViewModel>()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        viewModel.fetchEnabledDevices()

        val startPage = intent?.getIntExtra(
            MainActivity.IntentStartPage, MainActivity.PageUpdate
        ) ?: MainActivity.PageUpdate

        setContent {
            val windowSize = calculateWindowSizeClass(this)

            AppTheme {
                EdgeToEdge()

                // We're using Surface to avoid Scaffold's recomposition-on-scroll issue (when using scrollBehaviour and consuming innerPadding)
                Surface {
                    Column {
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val enabledDevices = state.enabledDevices
                        val deviceName = viewModel.deviceName ?: remember(enabledDevices) {
                            enabledDevices.find {
                                it.productNames.contains(SystemVersionProperties.oxygenDeviceName)
                            }?.name
                        }

                        val context = LocalContext.current
                        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

                        // Use normal app bar if height isn't enough (e.g. landscape phones)
                        if (windowSize.heightSizeClass == WindowHeightSizeClass.Compact) TopAppBar(
                            scrollBehavior = scrollBehavior,
                            navIconClicked = {},
                            subtitleResId = R.string.onboarding,
                            showIcon = false,
                        ) else CollapsingAppBar(
                            scrollBehavior = scrollBehavior,
                            image = { modifier ->
                                AsyncImage(
                                    model = deviceName?.let {
                                        val density = LocalDensity.current
                                        remember(it, maxWidth) {
                                            ImageRequest.Builder(context)
                                                .data(Device.constructImageUrl(it))
                                                .size(density.run { Size(maxWidth.roundToPx(), 256.dp.roundToPx()) })
                                                .build()
                                        }
                                    },
                                    contentDescription = stringResource(R.string.device_information_image_description),
                                    placeholder = rememberVectorPainter(CustomIcons.Image),
                                    error = rememberVectorPainter(CustomIcons.LogoNotification),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = if (deviceName == null) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                                    modifier = modifier
                                )
                            },
                            title = stringResource(R.string.app_name),
                            subtitle = stringResource(R.string.onboarding),
                        )

                        OnboardingScreen(
                            windowWidthSize = windowSize.widthSizeClass,
                            scrollBehavior = scrollBehavior,
                            lists = state,
                            initialDeviceIndex = viewModel.initialDeviceIndex,
                            deviceChanged = rememberTypedCallback(viewModel::saveSelectedDevice),
                            initialMethodIndex = viewModel.initialMethodIndex,
                            methodChanged = rememberTypedCallback(viewModel::saveSelectedMethod),
                            startApp = rememberTypedCallback(enabledDevices) { contribute ->
                                if (Utils.checkPlayServices(this@OnboardingActivity, false)) {
                                    // Subscribe to notifications for the newly selected device and update method
                                    viewModel.subscribeToNotificationTopics(enabledDevices)
                                } else context.showToast(R.string.notification_no_notification_support)

                                if (PrefManager.checkIfSetupScreenIsFilledIn()) {
                                    PrefManager.putBoolean(PrefManager.KeySetupDone, true)
                                    (application as OxygenUpdater?)?.setupCrashReporting(
                                        PrefManager.getBoolean(PrefManager.KeyShareAnalyticsAndLogs, true)
                                    )

                                    // If user enables OTA contribution, check if device is rooted and ask for root permission
                                    if (ContributorUtils.isAtLeastQAndPossiblyRooted && contribute) {
                                        context.showToast(R.string.contribute_allow_storage)
                                        hasRootAccess {
                                            PrefManager.putBoolean(PrefManager.KeyContribute, true)
                                            startMainActivity(startPage)
                                            finish()
                                        }
                                    } else {
                                        // Skip shell creation and thus don't show root permission prompt
                                        PrefManager.putBoolean(PrefManager.KeyContribute, false)
                                        startMainActivity(startPage)
                                        finish()
                                    }
                                } else {
                                    val deviceId = PrefManager.getLong(PrefManager.KeyDeviceId, NotSetL)
                                    val updateMethodId = PrefManager.getLong(PrefManager.KeyUpdateMethodId, NotSetL)
                                    logWarning(TAG, "Required preferences not valid: $deviceId, $updateMethodId")
                                    context.showToast(R.string.settings_entered_incorrectly)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "OnboardingActivity"
    }
}
