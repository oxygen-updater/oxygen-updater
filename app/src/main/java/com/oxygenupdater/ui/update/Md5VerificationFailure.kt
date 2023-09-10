package com.oxygenupdater.ui.update

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Immutable
@JvmInline
value class Md5VerificationFailure(val value: Int) {

    override fun toString() = when (this) {
        NullUpdateData -> "NullUpdateData"
        NullOrEmptyProvidedChecksum -> "NullOrEmptyProvidedChecksum"
        NullCalculatedChecksum -> "NullCalculatedChecksum"
        ChecksumsNotEqual -> "ChecksumsNotEqual"
        else -> "Invalid"
    }

    companion object {
        /**
         * If update data is null.
         *
         * Note: we recreate [com.oxygenupdater.models.UpdateData] from [androidx.work.Data],
         * which is passed into the worker while creating the [androidx.work.OneTimeWorkRequest]
         */
        @Stable
        val NullUpdateData = Md5VerificationFailure(0)

        /** If the provided checksum (received from the backend) is null or empty */
        @Stable
        val NullOrEmptyProvidedChecksum = Md5VerificationFailure(1)

        /**
         * Happens in one of the following cases:
         * * the device doesn't have a provider for the MD5 algorithm,
         * * downloaded file doesn't exist, even after retrying every 2 seconds upto 5 times
         */
        @Stable
        val NullCalculatedChecksum = Md5VerificationFailure(2)

        /** If calculated checksum and provided checksum don't match */
        @Stable
        val ChecksumsNotEqual = Md5VerificationFailure(3)
    }
}
