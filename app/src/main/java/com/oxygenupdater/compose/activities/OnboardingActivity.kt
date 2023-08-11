package com.oxygenupdater.compose.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.Image
import com.oxygenupdater.compose.icons.LogoNotification
import com.oxygenupdater.compose.ui.CollapsingAppBar
import com.oxygenupdater.compose.ui.common.TransparentSystemBars
import com.oxygenupdater.compose.ui.onboarding.NOT_SET_L
import com.oxygenupdater.compose.ui.onboarding.OnboardingScreen
import com.oxygenupdater.compose.ui.settings.SettingsViewModel
import com.oxygenupdater.compose.ui.theme.AppTheme
import com.oxygenupdater.extensions.startMainActivity
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.hasRootAccess
import org.koin.androidx.viewmodel.ext.android.viewModel

class OnboardingActivity : ComposeBaseActivity() {

    private val viewModel by viewModel<SettingsViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) = super.onCreate(savedInstanceState).also {
        viewModel.fetchEnabledDevices()

        val startPage = intent?.getIntExtra(
            MainActivity.INTENT_START_PAGE, MainActivity.PAGE_UPDATE
        ) ?: MainActivity.PAGE_UPDATE

        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            val deviceName = viewModel.deviceName ?: remember(state.enabledDevices) {
                state.enabledDevices.find {
                    it.productNames.contains(SystemVersionProperties.oxygenDeviceName)
                }?.name
            }

            AppTheme {
                TransparentSystemBars()

                val context = LocalContext.current
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                Scaffold(Modifier.navigationBarsPadding(), topBar = {
                    CollapsingAppBar(scrollBehavior, image = { modifier ->
                        AsyncImage(
                            deviceName?.let {
                                val density = LocalDensity.current
                                remember(it, maxWidth) {
                                    ImageRequest.Builder(context)
                                        .data(Device.constructImageUrl(it))
                                        .size(density.run { Size(maxWidth.roundToPx(), 256.dp.roundToPx()) })
                                        .build()
                                }
                            },
                            stringResource(R.string.device_information_image_description), modifier,
                            placeholder = rememberVectorPainter(CustomIcons.Image),
                            error = rememberVectorPainter(CustomIcons.LogoNotification),
                            contentScale = ContentScale.Crop,
                            colorFilter = if (deviceName == null) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null
                        )
                    }, title = stringResource(R.string.onboarding_page_1_title))
                }) { innerPadding ->
                    HorizontalDivider()
                    Box(Modifier.padding(innerPadding)) {
                        OnboardingScreen(scrollBehavior, state, viewModel.initialDeviceIndex, {
                            viewModel.saveSelectedDevice(it)
                        }, viewModel.initialMethodIndex, {
                            viewModel.saveSelectedMethod(it)
                        }, finish = {
                            finish()
                        }) { contribute, submitLogs ->
                            if (Utils.checkPlayServices(this@OnboardingActivity, false)) {
                                // Subscribe to notifications for the newly selected device and update method
                                viewModel.subscribeToNotificationTopics(state.enabledDevices)
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
                                logWarning(TAG, "Required preferences not valid: $deviceId, $updateMethodId")
                                Toast.makeText(context, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "OnboardingActivity"
    }
}
