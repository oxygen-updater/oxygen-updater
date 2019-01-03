package com.arjanvlek.oxygenupdater.download;

import android.content.Context;
import android.os.Environment;

import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData;

import java.io.File;

import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOAD_ID;


public class DownloadHelper {

    private final SettingsManager settingsManager;

    public static final String DIRECTORY_ROOT = "";

    public DownloadHelper(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null!");
        }

        this.settingsManager = new SettingsManager(context);
    }

    public boolean checkIfUpdateIsDownloaded(UpdateData updateData) {
        if (updateData == null || updateData.getId() == null) return false;
        File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getPath(), updateData.getFilename());
        return (file.exists() && !settingsManager.containsPreference(PROPERTY_DOWNLOAD_ID));
    }


    public static String getFilePath(UpdateData updateData) {
        return Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getPath() + File.separator + updateData.getFilename();
    }
}
