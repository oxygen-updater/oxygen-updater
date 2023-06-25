package com.oxygenupdater.extensions

import androidx.work.Data
import com.oxygenupdater.models.UpdateData

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */

fun UpdateData.toWorkData() = Data.Builder().apply {
    putLong("id", id ?: -1L)
    putString("versionNumber", versionNumber)
    putString("otaVersionNumber", otaVersionNumber)
    putString("description", description)
    putString("downloadUrl", downloadUrl)
    putLong("downloadSize", downloadSize)
    putString("filename", filename)
    putString("mD5Sum", mD5Sum)
    putString("information", information)
    putBoolean("updateInformationAvailable", updateInformationAvailable)
    putBoolean("systemIsUpToDate", systemIsUpToDate)
}.build()

fun createFromWorkData(inputData: Data?) = if (inputData != null) UpdateData(
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
) else null
