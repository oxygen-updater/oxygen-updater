package com.oxygenupdater.workers

/**
 * This file contains constants that are common to all workers defined in this package
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */

const val WorkUniqueDownload = "WorkUniqueDownload"
const val WorkUniqueMd5Verification = "WorkUniqueMd5Verification"
const val WorkUniqueReadOtaDb = "WorkUniqueReadOtaDb"

const val WorkDataDownloadBytesDone = "DownloadBytesDone"
const val WorkDataDownloadTotalBytes = "DownloadTotalBytes"
const val WorkDataDownloadProgress = "DownloadProgress"
const val WorkDataDownloadEta = "DownloadEta"
const val WorkDataDownloadFailureType = "DownloadFailureType"

/**
 * The following parameters are set only if failure type is [com.oxygenupdater.ui.update.DownloadFailure.UnsuccessfulResponse]
 */
const val WorkDataDownloadFailureExtraUrl = "DownloadFailureExtraUrl"
const val WorkDataDownloadFailureExtraFilename = "DownloadFailureExtraFilename"
const val WorkDataDownloadFailureExtraVersion = "DownloadFailureExtraVersion"
const val WorkDataDownloadFailureExtraOtaVersion = "DownloadFailureExtraOtaVersion"
const val WorkDataDownloadFailureExtraHttpCode = "DownloadFailureExtraHttpCode"
const val WorkDataDownloadFailureExtraHttpMessage = "DownloadFailureExtraHttpMessage"

const val WorkDataMd5VerificationFailureType = "Md5VerificationFailureType"

/**
 * Used in calls to [android.os.Environment.getExternalStoragePublicDirectory],
 * to get the root directory of internal storage
 */
const val DirectoryRoot = ""

/**
 * Minimum milliseconds that should have passed since the previous publish progress event.
 * Currently: `1s`
 */
const val MinMsBetweenProgressPublish = 1000L
