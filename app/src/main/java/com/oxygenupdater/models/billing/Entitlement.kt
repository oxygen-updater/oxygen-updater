package com.oxygenupdater.models.billing

import androidx.room.PrimaryKey

/**
 * Normally this would just be an interface. But since each of the entitlements only has
 * one item/row and so primary key is fixed, we can put the primary key here and so make
 * the class abstract.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 **/
abstract class Entitlement {

    @PrimaryKey
    var id: Int = 1

    /**
     * This method tells clients whether a user _should_ buy a particular item at the moment.
     * Example: if the ad-free unlock has already been purchased, the user should not be buying it again.
     * This method is **NOT** a reflection on whether Google Play Billing can make a purchase.
     */
    abstract fun mayPurchase(): Boolean
}
