package com.oxygenupdater.compose.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.Image
import com.oxygenupdater.compose.icons.LogoNotification
import com.oxygenupdater.compose.ui.CollapsingAppBar
import com.oxygenupdater.compose.ui.TopAppBarDefaults
import com.oxygenupdater.compose.ui.onboarding.NOT_SET_L
import com.oxygenupdater.compose.ui.onboarding.OnboardingScreen
import com.oxygenupdater.compose.ui.settings.SettingsViewModel
import com.oxygenupdater.compose.ui.theme.AppTheme
import com.oxygenupdater.compose.ui.theme.light
import com.oxygenupdater.extensions.startMainActivity
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.SetupUtils
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.hasRootAccess
import org.koin.androidx.viewmodel.ext.android.viewModel

class OnboardingActivity : ComposeBaseActivity() {

    private val startPage by lazy(LazyThreadSafetyMode.NONE) {
        intent?.getIntExtra(
            MainActivity.INTENT_START_PAGE,
            MainActivity.PAGE_UPDATE
        ) ?: MainActivity.PAGE_UPDATE
    }

    private val viewModel by viewModel<SettingsViewModel>()

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) = super.onCreate(savedInstanceState).also {
        viewModel.fetchEnabledDevices()

        setContent {
            val (enabledDevices, methodsForDevice) = viewModel.state.collectAsStateWithLifecycle().value
            val deviceName = viewModel.deviceName ?: remember(enabledDevices) {
                enabledDevices.find {
                    it.productNames.contains(SystemVersionProperties.oxygenDeviceName)
                }?.name
            }

            AppTheme {
                val colors = MaterialTheme.colors
                val controller = rememberSystemUiController()
                controller.setSystemBarsColor(Color.Transparent, MaterialTheme.colors.light)

                val context = LocalContext.current
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                Scaffold(Modifier.navigationBarsPadding(), topBar = {
                    CollapsingAppBar(scrollBehavior, image = { modifier ->
                        AsyncImage(
                            deviceName?.let {
                                remember(it, maxWidth) {
                                    ImageRequest.Builder(context)
                                        .data(Device.constructImageUrl(it))
                                        .size(Utils.dpToPx(context, maxWidth.value).toInt(), Utils.dpToPx(context, 256f).toInt())
                                        .build()
                                }
                            },
                            stringResource(R.string.device_information_image_description), modifier,
                            placeholder = rememberVectorPainter(CustomIcons.Image),
                            error = rememberVectorPainter(CustomIcons.LogoNotification),
                            contentScale = ContentScale.Crop,
                            colorFilter = if (deviceName == null) ColorFilter.tint(colors.primary) else null
                        )
                    }, title = stringResource(R.string.onboarding_page_1_title))
                }) { innerPadding ->
                    Divider()
                    Box(Modifier.padding(innerPadding)) {
                        OnboardingScreen(scrollBehavior, enabledDevices, methodsForDevice, {
                            viewModel.saveSelectedDevice(it)
                        }, {
                            viewModel.saveSelectedMethod(it)
                        }, viewModel.initialDeviceIndex to viewModel.initialMethodIndex, finish = {
                            finish()
                        }) { contribute, submitLogs ->
                            if (Utils.checkPlayServices(this@OnboardingActivity, false)) {
                                // Subscribe to notifications for the newly selected device and update method
                                viewModel.subscribeToNotificationTopics(enabledDevices)
                            } else Toast.makeText(
                                context,
                                context.getString(R.string.notification_no_notification_support),
                                Toast.LENGTH_LONG
                            ).show()

                            if (PrefManager.checkIfSetupScreenIsFilledIn()) {
                                PrefManager.putBoolean(PrefManager.PROPERTY_SHARE_ANALYTICS_AND_LOGS, submitLogs)
                                PrefManager.putBoolean(PrefManager.PROPERTY_SETUP_DONE, true)
                                (application as OxygenUpdater?)?.setupCrashReporting(submitLogs)

                                // If user enables OTA contribution, check if device is rooted and ask for root permission
                                if (ContributorUtils.isAtLeastQAndPossiblyRooted && contribute) {
                                    Toast.makeText(context, R.string.contribute_allow_storage, Toast.LENGTH_LONG).show()
                                    hasRootAccess {
                                        PrefManager.putBoolean(PrefManager.PROPERTY_CONTRIBUTE, true)
                                        startMainActivity(startPage)
                                        finish()
                                    }
                                } else {
                                    // Skip shell creation and thus don't show root permission prompt
                                    PrefManager.putBoolean(PrefManager.PROPERTY_CONTRIBUTE, false)
                                    startMainActivity(startPage)
                                    finish()
                                }
                            } else {
                                val deviceId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, NOT_SET_L)
                                val updateMethodId = PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, NOT_SET_L)
                                logWarning("OnboardingActivity", SetupUtils.getAsError("Setup wizard", deviceId, updateMethodId))
                                Toast.makeText(context, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }
}
