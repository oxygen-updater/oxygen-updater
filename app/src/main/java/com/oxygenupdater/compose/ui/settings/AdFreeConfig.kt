package com.oxygenupdater.compose.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import com.oxygenupdater.R
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.repositories.BillingRepository.SkuState
import com.oxygenupdater.utils.Logger.logBillingError

@Composable
fun adFreeConfig(
    state: SkuState?,
    markPending: () -> Unit,
    makePurchase: (PurchaseType) -> Unit,
) = remember {
    derivedStateOf(structuralEqualityPolicy()) {
        when (state) {
            SkuState.UNKNOWN -> {
                logBillingError(TAG, "SKU '${PurchaseType.AD_FREE.sku}' is not available")
                Triple(false, R.string.settings_buy_button_not_possible, null)
            }

            SkuState.NOT_PURCHASED -> Triple(true, R.string.settings_buy_button_buy) {
                // TODO(compose/settings): disable the Purchase button and set its text to "Processing…"
                // isEnabled = false
                // summary = mContext.getString(R.string.processing)

                // [newPurchaseObserver] handles the result
                makePurchase(PurchaseType.AD_FREE)
            }

            SkuState.PENDING -> {
                markPending()
                Triple(false, R.string.processing, null)
            }

            SkuState.PURCHASED_AND_ACKNOWLEDGED -> Triple(false, R.string.settings_buy_button_bought, null)

            // PURCHASED => already bought, but not yet acknowledged by the app.
            // This should never happen, as it's already handled within BillingDataSource.
            null, SkuState.PURCHASED -> null
        }
    }
}

private const val TAG = "SettingsScreen"
