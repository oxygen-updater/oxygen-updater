package com.arjanvlek.oxygenupdater.workers

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
enum class Md5VerificationFailure {

    /**
     * If update data is null.
     *
     * Note: we recreate [com.arjanvlek.oxygenupdater.models.UpdateData] from [androidx.work.Data],
     * which is passed into the worker while creating the [androidx.work.OneTimeWorkRequest]
     */
    NULL_UPDATE_DATA,

    /**
     * If the provided checksum (received from the backend) is null or empty
     */
    NULL_OR_EMPTY_PROVIDED_CHECKSUM,

    /**
     * Happens in one of the following cases:
     * * the device doesn't have a provider for the MD5 algorithm,
     * * downloaded file doesn't exist, even after retrying every 2 seconds upto 5 times
     */
    NULL_CALCULATED_CHECKSUM,

    /**
     * If calculated checksum and provided checksum don't match
     */
    CHECKSUMS_NOT_EQUAL;

    companion object {
        private val map = values().associateBy { it.name }

        /**
         * Returns a [Md5VerificationFailure] based on name
         *
         * @param name
         *
         * @return [Md5VerificationFailure]
         */
        operator fun get(name: String): Md5VerificationFailure? = map[name]
    }
}
