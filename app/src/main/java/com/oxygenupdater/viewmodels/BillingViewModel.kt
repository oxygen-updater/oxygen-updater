package com.oxygenupdater.viewmodels

import android.app.Activity
import android.app.Application
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.extensions.getDistinct
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.OnPurchaseFinishedListener
import com.oxygenupdater.models.ServerPostResult
import com.oxygenupdater.models.billing.AdFreeUnlock
import com.oxygenupdater.models.billing.AugmentedSkuDetails
import com.oxygenupdater.repositories.BillingRepository
import com.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class BillingViewModel(
    application: Application,
    private val billingRepository: BillingRepository,
    private val serverRepository: ServerRepository
) : AndroidViewModel(application) {

    val adFreeUnlockLiveData: LiveData<AdFreeUnlock?>
    val inappSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>>
    val pendingPurchasesLiveData: LiveData<Set<Purchase>>

    init {
        billingRepository.startDataSourceConnections()

        adFreeUnlockLiveData = billingRepository.adFreeUnlockLiveData.getDistinct()
        inappSkuDetailsListLiveData = billingRepository.inappSkuDetailsListLiveData.getDistinct()
        pendingPurchasesLiveData = billingRepository.pendingPurchasesLiveData.getDistinct()
    }

    /**
     * Not used yet, but could be used to force refresh (e.g. pull-to-refresh)
     */
    @UiThread
    fun queryPurchases() = billingRepository.queryPurchases()

    override fun onCleared() = super.onCleared().also {
        billingRepository.endDataSourceConnections()
    }

    @UiThread
    fun makePurchase(
        activity: Activity,
        augmentedSkuDetails: AugmentedSkuDetails,
        /**
         * Invoked within [BillingRepository.disburseNonConsumableEntitlement] and [BillingRepository.onPurchasesUpdated]
         */
        callback: OnPurchaseFinishedListener
    ) {
        billingRepository.launchBillingFlow(
            activity,
            augmentedSkuDetails
        ) { responseCode, purchase ->
            // Since we update UI after receiving the callback,
            // Make sure it's invoked on the main thread
            // (otherwise app would crash)
            viewModelScope.launch {
                callback.invoke(responseCode, purchase)
            }
        }
    }

    fun verifyPurchase(
        purchase: Purchase,
        amount: String?,
        purchaseType: PurchaseType,
        callback: KotlinCallback<ServerPostResult?>
    ) = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.verifyPurchase(
            purchase,
            amount,
            purchaseType
        ).let {
            withContext(Dispatchers.Main) {
                callback.invoke(it)
            }
        }
    }
}
