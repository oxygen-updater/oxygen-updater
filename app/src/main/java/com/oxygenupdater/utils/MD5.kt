/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */
package com.oxygenupdater.utils

import com.oxygenupdater.exceptions.OxygenUpdaterException
import com.oxygenupdater.utils.Logger.logError
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

internal object MD5 {

    private const val TAG = "MD5"

    fun calculateMD5(deviceId: String): String {
        return try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")

            digest.update(deviceId.toByteArray())

            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuilder()
            for (b in messageDigest) {
                val h = StringBuilder(Integer.toHexString(0xFF and b.toInt()))

                while (h.length < 2) {
                    h.insert(0, "0")
                }

                hexString.append(h)
            }

            hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            logError(TAG, OxygenUpdaterException(e.message))
            ""
        }
    }
}
