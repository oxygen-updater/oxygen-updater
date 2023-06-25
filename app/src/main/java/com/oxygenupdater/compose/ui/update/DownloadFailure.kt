package com.oxygenupdater.compose.ui.update

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Immutable
@JvmInline
value class DownloadFailure(val value: Int) {

    override fun toString() = when (this) {
        NullUpdateDataOrDownloadUrl -> "NullUpdateDataOrDownloadUrl"
        DownloadUrlInvalidScheme -> "DownloadUrlInvalidScheme"
        ServerError -> "ServerError"
        ConnectionError -> "ConnectionError"
        UnsuccessfulResponse -> "UnsuccessfulResponse"
        CouldNotMoveTempFile -> "CouldNotMoveTempFile"
        Unknown -> "Unknown"
        else -> "Invalid"
    }

    companion object {
        /**
         * If update data or download URL is null.
         *
         * Note: we recreate [com.oxygenupdater.models.UpdateData] from [androidx.work.Data],
         * which is passed into the worker while creating the [androidx.work.OneTimeWorkRequest]
         */
        @Stable
        val NullUpdateDataOrDownloadUrl = DownloadFailure(0)

        /** If the download URL doesn't contain `http` */
        @Stable
        val DownloadUrlInvalidScheme = DownloadFailure(1)

        /**
         * If a network-related exception occurs while downloading/writing the file
         *
         * @see com.oxygenupdater.utils.ExceptionUtils.isNetworkError
         */
        @Stable
        val ServerError = DownloadFailure(2)

        /** If an [java.io.IOException] is thrown while downloading/writing the file */
        @Stable
        val ConnectionError = DownloadFailure(2)

        /**
         * If the Retrofit response returns unsuccessfully, i.e. an HTTP code that does not lie between 200 and 300.
         * Most of the times this would be because of an invalid link
         * (e.g. when OnePlus pulls an update, or a human error while adding update data)
         */
        @Stable
        val UnsuccessfulResponse = DownloadFailure(3)

        /**
         * If the temporary downloaded file can't be copied to the root directory of internal storage
         */
        @Stable
        val CouldNotMoveTempFile = DownloadFailure(4)

        /** Unknown error */
        @Stable
        val Unknown = DownloadFailure(5)

        // If new failures are added, adjust show download link logic in UpdateInformationContent.kt
    }
}
