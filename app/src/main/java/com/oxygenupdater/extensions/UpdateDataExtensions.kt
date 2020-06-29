package com.oxygenupdater.extensions

import androidx.work.Data
import androidx.work.workDataOf
import com.oxygenupdater.models.UpdateData

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */

fun UpdateData.toWorkData() = workDataOf(
    "id" to id,
    "versionNumber" to versionNumber,
    "otaVersionNumber" to otaVersionNumber,
    "description" to description,
    "downloadUrl" to downloadUrl,
    "downloadSize" to downloadSize,
    "filename" to filename,
    "mD5Sum" to mD5Sum,
    "information" to information,
    "updateInformationAvailable" to updateInformationAvailable,
    "systemIsUpToDate" to systemIsUpToDate
)

fun createFromWorkData(inputData: Data?) = if (inputData != null) {
    UpdateData(
        id = inputData.getLong("id", -1L),
        versionNumber = inputData.getString("versionNumber"),
        otaVersionNumber = inputData.getString("otaVersionNumber"),
        description = inputData.getString("description"),
        downloadUrl = inputData.getString("downloadUrl"),
        downloadSize = inputData.getLong("downloadSize", -1L),
        filename = inputData.getString("filename"),
        mD5Sum = inputData.getString("mD5Sum"),
        information = inputData.getString("information"),
        updateInformationAvailable = inputData.getBoolean("updateInformationAvailable", false),
        systemIsUpToDate = inputData.getBoolean("systemIsUpToDate", false)
    )
} else {
    null
}
