package com.arjanvlek.oxygenupdater.enums

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
enum class DownloadFailure {

    /**
     * If update data or download URL is null.
     *
     * Note: we recreate [com.arjanvlek.oxygenupdater.models.UpdateData] from [androidx.work.Data],
     * which is passed into the worker while creating the [androidx.work.OneTimeWorkRequest]
     */
    NULL_UPDATE_DATA_OR_DOWNLOAD_URL,

    /**
     * If the download URL doesn't contain `http`
     */
    DOWNLOAD_URL_INVALID_SCHEME,

    /**
     * If a network-related exception occurs while downloading/writing the file.
     *
     * @see com.arjanvlek.oxygenupdater.utils.ExceptionUtils.isNetworkError
     */
    SERVER_ERROR,

    /**
     * If an [java.io.IOException] is thrown while downloading/writing the file.
     */
    CONNECTION_ERROR,

    /**
     * If the temporary downloaded file can't be copied to the root directory of internal storage
     */
    COULD_NOT_MOVE_TEMP_FILE,

    /**
     * Unknown error
     */
    UNKNOWN;

    companion object {
        private val map = values().associateBy { it.name }

        /**
         * Returns a [DownloadFailure] based on name
         *
         * @param name
         *
         * @return [DownloadFailure]
         */
        operator fun get(name: String): DownloadFailure? = map[name]
    }
}
