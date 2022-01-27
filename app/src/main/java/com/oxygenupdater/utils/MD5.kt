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
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

internal object MD5 {

    private const val TAG = "MD5"

    fun calculateMD5(deviceId: String): String {
        return try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            val messageDigest = digest.digest(deviceId.toByteArray())

            // Create Hex String
            String.format("%032x", BigInteger(1, messageDigest))
        } catch (e: NoSuchAlgorithmException) {
            logError(TAG, OxygenUpdaterException(e.message))
            ""
        }
    }
}
