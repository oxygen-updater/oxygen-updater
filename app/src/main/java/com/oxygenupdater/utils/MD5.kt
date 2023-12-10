/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */
package com.oxygenupdater.utils

import java.math.BigInteger
import java.security.MessageDigest

fun calculateMD5(data: String): String {
    // Create MD5 Hash
    val digest = MessageDigest.getInstance("MD5")
    val messageDigest = digest.digest(data.toByteArray())

    // Create Hex String
    return String.format("%032x", BigInteger(1, messageDigest))
}
