package com.oxygenupdater.ui.settings

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollTo
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.UsesSharedPreferencesTest
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.get
import com.oxygenupdater.internal.settings.KeyShareAnalyticsAndLogs
import com.oxygenupdater.repositories.BillingRepository.SkuState
import com.oxygenupdater.ui.Theme
import com.oxygenupdater.ui.common.OutlinedIconButtonTestTag
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.ui.dialogs.BottomSheetTestTag
import com.oxygenupdater.ui.dialogs.BottomSheet_ItemRowTestTag
import com.oxygenupdater.ui.dialogs.Themes
import com.oxygenupdater.ui.main.NavType
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest : UsesSharedPreferencesTest() {

    private val sheet = rule[BottomSheetTestTag]
    private var adFreeState by mutableStateOf<SkuState?>(null)

    private lateinit var children: SemanticsNodeInteractionCollection

    /**
     * Only required for Android 13/T+ to ensure notification item's summary is [R.string.summary_on]
     *
     * @see deviceSection
     */
    @get:Rule
    val postNotificationsPermissionRule = GrantPostNotificationsPermissionRule()

    @Test
    fun settingsScreen() {
        setContent {
            SettingsScreen(
                navType = NavType.BottomBar,
                adFreePrice = "price",
                adFreeConfig = remember(adFreeState) {
                    adFreeConfig(
                        state = adFreeState,
                        logBillingError = { trackCallback("logBillingError") },
                        makePurchase = { trackCallback("makePurchase: ${it.sku}") },
                        markPending = { trackCallback("markPending") },
                    )
                },
                onContributorEnrollmentChange = { trackCallback("onContributorEnrollmentChange: $it") },
                deviceConfig = DeviceSettingsListConfig,
                onDeviceSelect = { trackCallback("onDeviceSelect: ${it.id}") },
                methodConfig = MethodSettingsListConfig,
                onMethodSelect = { trackCallback("onMethodSelect: ${it.id}") },
                onThemeSelect = { trackCallback("onThemeSelect: $it") },
                advancedMode = false,
                onAdvancedModeChange = { trackCallback("onAdvancedModeChange: $it") },
                isPrivacyOptionsRequired = true,
                showPrivacyOptionsForm = { trackCallback("showPrivacyOptionsForm") },
                openAboutScreen = { trackCallback("openAboutScreen") },
                getPrefStr = ::getPrefStr,
                getPrefBool = ::getPrefBool,
                persistBool = ::persistBool,
            )
        }

        children = rule[SettingsScreenTestTag].run {
            assertHasScrollAction()
            onChildren()
        }.also { it.assertCountEquals(19) }

        supportSection()
        deviceSection()
        uiSection()

        // By now we must have passed more than half the children, so
        // scroll down to the last child to keep remaining ones in view.
        children.onLast().performScrollTo()

        advancedSection()
        aboutSection()
    }

    private fun supportSection() {
        children[0].assertHasTextExactly(R.string.preference_header_support)
        children[2].assertHasTextExactly(R.string.label_buy_ad_free)
        children[3].run {
            // First we test for the initial null value
            assertHasTextExactly(R.string.settings_buy_ad_free_label)
            ensureNoCallbacksWereInvoked()

            // Then for other non null values
            adFreeState = SkuState.Purchased
            assertHasTextExactly(R.string.settings_buy_ad_free_label)
            ensureNoCallbacksWereInvoked()

            adFreeState = SkuState.Unknown
            assertHasTextExactly(R.string.settings_buy_button_not_possible)
            ensureCallbackInvokedExactlyOnce("logBillingError")

            adFreeState = SkuState.PurchaseInitiated
            assertHasTextExactly(R.string.summary_please_wait)
            ensureNoCallbacksWereInvoked()

            adFreeState = SkuState.Pending
            assertHasTextExactly(R.string.processing)
            ensureCallbackInvokedExactlyOnce("markPending")

            adFreeState = SkuState.PurchasedAndAcknowledged
            assertHasTextExactly(R.string.settings_buy_button_bought)
            ensureNoCallbacksWereInvoked()
        }

        /**
         * Finally, for the [SkuState.NotPurchased] state. As this is the only
         * "enabled" state (with non-null onClick), this item is grouped into
         * being the first child. This is in contrast to how it was above: the
         * first child was an icon, second & third were title & subtitle resp.
         * As such, the total number of children will now be 17, not 19.
         */
        adFreeState = SkuState.NotPurchased
        children[1].run {
            assertHasTextExactly(
                R.string.label_buy_ad_free,
                activity.getString(R.string.settings_buy_button_buy, "price"),
            )
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("makePurchase: ${PurchaseType.AD_FREE.sku}")
        }
    }

    private fun deviceSection() {
        // Header
        children[2].assertHasTextExactly(R.string.preference_header_device)

        // DeviceChooser
        children[3].run {
            assertHasTextExactly(R.string.settings_device, androidx.compose.ui.R.string.not_selected)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onDeviceSelect: ${DeviceSettingsListConfig.list[0].id}")
            sheet.assertIsNotDisplayed()
        }

        // MethodChooser
        children[4].run {
            assertHasTextExactly(R.string.settings_update_method, androidx.compose.ui.R.string.not_selected)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onMethodSelect: ${MethodSettingsListConfig.list[0].id}")
            sheet.assertIsNotDisplayed()
        }

        // Notifications
        children[5].run {
            assertHasTextExactly(R.string.preference_header_notifications, R.string.summary_on)
            assertHasClickAction()
            // Don't performClick because that'll leave the app, which will fail subsequent tests
        }
    }

    private fun uiSection() {
        children[6].assertHasTextExactly(R.string.preference_header_ui)

        // Theme
        children[7].run {
            assertHasTextExactly(R.string.label_theme, Theme.System.titleResId)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onThemeSelect: ${Themes[0]}")
            sheet.assertIsNotDisplayed()
        }

        // Language
        children[8].run {
            assertHasTextExactly(
                R.string.label_language,
                with(currentLocale) {
                    displayName.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(this) else it.toString()
                    }
                },
            )

            if (SDK_INT >= VERSION_CODES.TIRAMISU) {
                sheet.assertIsNotDisplayed() // should open system's app locale screen
                assertHasClickAction()
                // Don't performClick because that'll leave the app, which will fail subsequent tests
            } else {
                assertAndPerformClick()
                // Our own sheet is used only below Android 13
                sheet.assertIsDisplayed()
                rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[1].assertAndPerformClick()
                sheet.assertIsNotDisplayed()
            }
        }
    }

    private fun advancedSection() {
        children[9].assertHasTextExactly(R.string.preference_header_advanced)

        // Advanced mode switch
        children[10].run {
            assertHasTextExactly(R.string.settings_advanced_mode, R.string.summary_off)
            val switch = onChild().assertIsOff()

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule[OutlinedIconButtonTestTag].assertAndPerformClick()
            sheet.assertIsNotDisplayed()
            ensureCallbackInvokedExactlyOnce("onAdvancedModeChange: true")
            assertHasTextExactly(R.string.settings_advanced_mode, R.string.summary_on)
            switch.assertIsOn()
        }

        // Analytics switch
        children[11].run {
            assertHasTextExactly(R.string.settings_upload_logs, R.string.summary_on)
            val switch = onChild().assertIsOn()

            assertAndPerformClick()
            sheet.assertIsNotDisplayed()
            ensureCallbackInvokedExactlyOnce("persistBool: $KeyShareAnalyticsAndLogs=false")
            assertHasTextExactly(R.string.settings_upload_logs, R.string.summary_off)
            switch.assertIsOff()
        }

        // GDPR ad consent
        children[12].run {
            assertHasTextExactly(R.string.settings_ad_privacy, R.string.settings_ad_privacy_subtitle)
            assertHasClickAction()
            // Don't performClick because that'll leave the app, which will fail subsequent tests
        }
    }

    private fun aboutSection() {
        children[13].assertHasTextExactly(R.string.preference_header_about)

        // Privacy policy
        children[14].run {
            assertHasTextExactly(R.string.label_privacy_policy, R.string.summary_privacy_policy)
            assertHasClickAction()
            // Don't performClick because that'll leave the app, which will fail subsequent tests
        }

        // Rate app
        children[15].run {
            assertHasTextExactly(R.string.label_rate_app, R.string.summary_rate_app)
            assertHasClickAction()
            // Don't performClick because that'll leave the app, which will fail subsequent tests
        }

        // App version
        children[16].run {
            assertHasTextExactly(R.string.app_name, "v${BuildConfig.VERSION_NAME}")
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("openAboutScreen")
        }
    }
}
