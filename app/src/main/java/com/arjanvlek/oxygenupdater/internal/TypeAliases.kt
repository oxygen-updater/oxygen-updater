package com.arjanvlek.oxygenupdater.internal

import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.IabResult
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.Inventory
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.Purchase

/**
 * Generic Kotlin callback. This used to be [java8.util.function.Consumer] in Java
 */
typealias KotlinCallback<E> = (E) -> Unit

/**
 * Generic Kotlin function. This used to be [java8.util.function.Function] in Java
 */
typealias KotlinFunction<T, R> = (T) -> R

/**
 * Callback that notifies when a purchase is finished.
 *
 * Called to notify that an in-app purchase finished. If the purchase was successful, then
 * the sku parameter specifies which item was purchased. If the purchase failed, the sku and
 * extraData parameters may or may not be null, depending on how far the purchase process
 * went.
 */
typealias OnIabPurchaseFinishedListener = (result: IabResult, purchase: Purchase?) -> Unit

/**
 * Callback for setup process. Called when the setup process is complete.
 */
typealias OnIabSetupFinishedListener = (result: IabResult) -> Unit

/**
 * Listener that notifies when an inventory query operation completes.
 */
typealias QueryInventoryFinishedListener = (result: IabResult, inv: Inventory?) -> Unit

/**
 * Callback that notifies when a consumption operation finishes.
 */
typealias OnConsumeFinishedListener = (purchase: Purchase?, result: IabResult?) -> Unit

/**
 * Callback that notifies when a multi-item consumption operation finishes.
 */
typealias OnConsumeMultiFinishedListener = (purchases: List<Purchase>?, results: List<IabResult?>?) -> Unit
