package com.arjanvlek.oxygenupdater.internal

import com.arjanvlek.oxygenupdater.internal.iab.IabResult
import com.arjanvlek.oxygenupdater.internal.iab.Inventory
import com.arjanvlek.oxygenupdater.internal.iab.Purchase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(
    // coerce strings to booleans
    SimpleModule().addDeserializer(Boolean::class.java, BooleanDeserializer())
).setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
// ^ Tell ObjectMapper to convert camelCase to snake_case (Server API response have fields in snake case)
// This helps us avoid annotating every field with `@JsonProperty` unnecessarily

/**
 * Generic Kotlin callback. This used to be [java.util.function.Consumer] in Java
 */
typealias KotlinCallback<E> = (E) -> Unit

/**
 * Generic Kotlin function. This used to be [java.util.function.Function] in Java
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
