/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */
package com.arjanvlek.oxygenupdater.download

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

internal object MD5 {

    private const val TAG = "MD5"

    @JvmStatic
    fun checkMD5(md5: String?, updateFile: File?): Boolean {
        if (md5.isNullOrEmpty() || updateFile == null) {
            logError(TAG, UpdateVerificationException("MD5 string empty or updateFile null"))
            return false
        }

        val calculatedDigest = calculateMD5(updateFile, 0)
        if (calculatedDigest == null) {
            logError(TAG, UpdateVerificationException("calculatedDigest null"))
            return false
        }

        logVerbose(TAG, "Calculated digest: $calculatedDigest")
        logVerbose(TAG, "Provided digest: $md5")

        return calculatedDigest.equals(md5, ignoreCase = true)
    }

    private fun calculateMD5(updateFile: File, retryCount: Int): String? {
        val digest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            logError(TAG, "Exception while getting digest", e)
            return null
        }

        val stream = try {
            FileInputStream(updateFile)
        } catch (e: FileNotFoundException) {
            logError(TAG, "Exception while getting FileInputStream", e)

            // If the downloaded file may not yet be accessed (because it's still being flushed or previously-existing files are being rotated, wait a bit and try verifying it again.
            return if (retryCount < 5) {
                try {
                    Thread.sleep(2000)
                } catch (i: InterruptedException) {
                    logError(TAG, "Error while trying to re-verify file after 2 seconds", i)
                }

                calculateMD5(updateFile, retryCount + 1)
            } else {
                null
            }
        }

        val buffer = ByteArray(8192)
        var read: Int

        return try {
            while (stream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }

            val md5sum = digest.digest()
            val bigInt = BigInteger(1, md5sum)

            // Fill to 32 chars
            String.format("%32s", bigInt.toString(16)).replace(' ', '0')
        } catch (e: IOException) {
            throw RuntimeException("Unable to process file for MD5", e)
        } finally {
            try {
                stream.close()
            } catch (e: IOException) {
                logError(TAG, "Exception on closing MD5 input stream", e)
            }
        }
    }

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
