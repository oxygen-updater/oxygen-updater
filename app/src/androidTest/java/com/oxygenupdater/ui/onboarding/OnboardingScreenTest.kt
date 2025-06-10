package com.oxygenupdater.ui.onboarding

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import com.oxygenupdater.R
import com.oxygenupdater.UsesSharedPreferencesTest
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.get
import com.oxygenupdater.internal.settings.KeyShareAnalyticsAndLogs
import com.oxygenupdater.ui.common.OutlinedIconButtonTestTag
import com.oxygenupdater.ui.dialogs.BottomSheetTestTag
import com.oxygenupdater.ui.dialogs.BottomSheet_ItemRowTestTag
import com.oxygenupdater.ui.settings.DeviceSettingsListConfig
import com.oxygenupdater.ui.settings.MethodSettingsListConfig
import com.oxygenupdater.validateColumnLayout
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class OnboardingScreenTest : UsesSharedPreferencesTest() {

    @Test
    fun onboardingScreen_expanded() {
        setContent(WindowWidthSizeClass.Expanded)

        common(rule[OnboardingScreen_MainColumnTestTag].run {
            assertHasScrollAction()
            validateColumnLayout(3)
        })

        rule[OnboardingScreen_SecondaryColumnTestTag].assertHasScrollAction().validateColumnLayout(2).run {
            get(0).assertHasTextExactly(R.string.onboarding_app_uses)
            get(1).assertHasTextExactly(R.string.onboarding_caption)
        }

        rule[OnboardingScreen_DisclaimerTestTag].assertHasTextExactly(R.string.onboarding_disclaimer)
    }

    @Test
    fun onboardingScreen_compact() {
        setContent(WindowWidthSizeClass.Compact)

        val children = rule[OnboardingScreen_MainColumnTestTag].run {
            assertHasScrollAction()
            validateColumnLayout(5)
        }

        common(children)

        children[3].assertHasTextExactly(R.string.onboarding_app_uses)
        children[4].assertHasTextExactly(R.string.onboarding_caption)
        rule[OnboardingScreen_DisclaimerTestTag].assertHasTextExactly(R.string.onboarding_disclaimer)
    }

    private fun common(children: SemanticsNodeInteractionCollection) {
        // Start app button
        rule[OutlinedIconButtonTestTag].run {
            assertHasTextExactly(R.string.onboarding_finished_button)
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onStartAppClick: true")
            /**
             * TODO(test/contributor): checkbox can be tested only on rooted devices
             *  above API 29, because checkbox & buttons are guarded behind
             *  `ContributorUtils.isAtLeastQAndPossiblyRooted`.
             *  "PossiblyRooted" is determined by Shell.isAppGrantedRoot(),
             *  which is false for emulators and managed devices.
             */
        }

        val sheet = rule[BottomSheetTestTag]

        // DeviceChooser
        children[0].run {
            assertHasTextExactly(R.string.settings_device, androidx.compose.ui.R.string.not_selected)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onDeviceSelect: ${DeviceSettingsListConfig.list[0].id}")
            sheet.assertIsNotDisplayed()
        }

        // MethodChooser
        children[1].run {
            assertHasTextExactly(
                R.string.settings_update_method,
                androidx.compose.ui.R.string.not_selected,
            )

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onMethodSelect: ${MethodSettingsListConfig.list[0].id}")
            sheet.assertIsNotDisplayed()
        }

        // Analytics
        children[2].run {
            assertHasTextExactly(
                R.string.settings_upload_logs,
                R.string.summary_on, // default is on
            )

            assertAndPerformClick() // should be toggled off now
            assertHasTextExactly(
                R.string.settings_upload_logs,
                R.string.summary_off,
            )
            ensureCallbackInvokedExactlyOnce("persistBool: $KeyShareAnalyticsAndLogs=false")
        }
    }

    private fun setContent(windowWidthSize: WindowWidthSizeClass) = setContent {
        OnboardingScreen(
            windowWidthSize = windowWidthSize,
            scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            deviceConfig = DeviceSettingsListConfig,
            onDeviceSelect = { trackCallback("onDeviceSelect: ${it.id}") },
            methodConfig = MethodSettingsListConfig,
            onMethodSelect = { trackCallback("onMethodSelect: ${it.id}") },
            getPrefStr = ::getPrefStr,
            getPrefBool = ::getPrefBool,
            persistBool = ::persistBool,
            onStartAppClick = { trackCallback("onStartAppClick: $it") },
        )
    }
}
