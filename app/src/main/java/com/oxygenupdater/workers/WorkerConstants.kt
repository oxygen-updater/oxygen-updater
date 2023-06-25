package com.oxygenupdater.workers

/**
 * This file contains constants that are common to all workers defined in this package
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */

const val WORK_UNIQUE_DOWNLOAD = "WORK_UNIQUE_DOWNLOAD"
const val WORK_UNIQUE_MD5_VERIFICATION = "WORK_UNIQUE_MD5_VERIFICATION"
const val WORK_UNIQUE_READ_OTA_DB = "WORK_UNIQUE_READ_OTA_DB"

const val WORK_DATA_DOWNLOAD_BYTES_DONE = "DOWNLOAD_BYTES_DONE"
const val WORK_DATA_DOWNLOAD_TOTAL_BYTES = "DOWNLOAD_TOTAL_BYTES"
const val WORK_DATA_DOWNLOAD_PROGRESS = "DOWNLOAD_PROGRESS"
const val WORK_DATA_DOWNLOAD_ETA = "DOWNLOAD_ETA"
const val WORK_DATA_DOWNLOAD_FAILURE_TYPE = "DOWNLOAD_FAILURE_TYPE"

/**
 * The following parameters are set only if failure type is [com.oxygenupdater.enums.DownloadFailure.UnsuccessfulResponse]
 */
const val WORK_DATA_DOWNLOAD_FAILURE_EXTRA_URL = "DOWNLOAD_FAILURE_EXTRA_URL"
const val WORK_DATA_DOWNLOAD_FAILURE_EXTRA_FILENAME = "DOWNLOAD_FAILURE_EXTRA_FILENAME"
const val WORK_DATA_DOWNLOAD_FAILURE_EXTRA_VERSION = "DOWNLOAD_FAILURE_EXTRA_VERSION"
const val WORK_DATA_DOWNLOAD_FAILURE_EXTRA_OTA_VERSION = "DOWNLOAD_FAILURE_EXTRA_OTA_VERSION"
const val WORK_DATA_DOWNLOAD_FAILURE_EXTRA_HTTP_CODE = "DOWNLOAD_FAILURE_EXTRA_HTTP_CODE"
const val WORK_DATA_DOWNLOAD_FAILURE_EXTRA_HTTP_MESSAGE = "DOWNLOAD_FAILURE_EXTRA_HTTP_MESSAGE"

const val WORK_DATA_MD5_VERIFICATION_FAILURE_TYPE = "MD5_VERIFICATION_FAILURE_TYPE"

/**
 * Used in calls to [android.os.Environment.getExternalStoragePublicDirectory],
 * to get the root directory of internal storage
 */
const val DIRECTORY_ROOT = ""

/**
 * Minimum milliseconds that should have passed since the previous publish progress event.
 * Currently: `2s`
 */
const val THRESHOLD_PUBLISH_PROGRESS_TIME_PASSED = 2000L
