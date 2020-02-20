package com.arjanvlek.oxygenupdater.internal.iab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver for the "com.android.vending.billing.PURCHASES_UPDATED" Action from the Play Store.
 *
 * It is possible that an in-app item may be acquired without the
 * application calling getBuyIntent(), for example if the item can be redeemed from inside the Play
 * Store using a promotional code. If this application isn't running at the time, then when it is
 * started a call to getPurchases() will be sufficient notification. However, if the application is
 * already running in the background when the item is acquired, a message to this BroadcastReceiver
 * will indicate that the an item has been acquired.
 */
class IabBroadcastReceiver(private val mListener: IabBroadcastListener?) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        mListener?.receivedBroadcast()
    }

    /**
     * Listener interface for received broadcast messages.
     */
    interface IabBroadcastListener {
        fun receivedBroadcast()
    }

    companion object {
        /**
         * The Intent action that this Receiver should filter for.
         */
        const val ACTION = "com.android.vending.billing.PURCHASES_UPDATED"
    }

}
