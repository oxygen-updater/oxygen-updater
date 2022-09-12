package com.oxygenupdater.viewmodels

import android.app.Activity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.models.ServerPostResult
import com.oxygenupdater.repositories.BillingRepository
import com.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class BillingViewModel(
    private val billingRepository: BillingRepository,
    private val serverRepository: ServerRepository
) : ViewModel() {

    val lifecycleObserver: LifecycleObserver = billingRepository

    val adFreePrice = billingRepository.adFreePrice.asLiveData()
    val adFreeState = billingRepository.adFreeState.asLiveData()
    val hasPurchasedAdFree = billingRepository.hasPurchasedAdFree.asLiveData()
    val newPurchase = billingRepository.newPurchase.asLiveData()

    // Clients need to observe this LiveData so that internal logic is guaranteed to run
    val pendingPurchase by lazy(LazyThreadSafetyMode.NONE) {
        billingRepository.pendingPurchase.mapNotNull {
            logPendingAdFreePurchase(it)
        }.asLiveData()
    }

    // Clients need to observe this LiveData so that internal logic is guaranteed to run
    val purchaseStateChange by lazy(LazyThreadSafetyMode.NONE) {
        billingRepository.purchaseStateChange.mapNotNull {
            logPendingAdFreePurchase(it)
        }.asLiveData()
    }

    /**
     * Starts a billing flow for purchasing [type].
     *
     * @return whether or not we were able to start the flow
     */
    fun makePurchase(
        activity: Activity,
        type: PurchaseType
    ) = billingRepository.makePurchase(activity, type.sku)

    fun verifyPurchase(
        purchase: Purchase,
        amount: String?,
        purchaseType: PurchaseType,
        callback: KotlinCallback<ServerPostResult?>
    ) = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.verifyPurchase(purchase, amount, purchaseType).let {
            withContext(Dispatchers.Main) {
                callback.invoke(it)
            }
        }
    }

    /**
     * Updates the server with information about the pending purchase, and returns
     * the pending purchase object so that other LiveData observers can act on it
     */
    private fun logPendingAdFreePurchase(purchase: Purchase?) = purchase?.also {
        if (it.products.contains(PurchaseType.AD_FREE.sku)) {
            viewModelScope.launch(Dispatchers.IO) {
                serverRepository.verifyPurchase(purchase, null, PurchaseType.AD_FREE)
            }
        }
    }
}
