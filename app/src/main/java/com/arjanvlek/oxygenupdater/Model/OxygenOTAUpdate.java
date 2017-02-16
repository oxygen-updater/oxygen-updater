package com.arjanvlek.oxygenupdater.Model;

import android.app.Activity;

import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.arjanvlek.oxygenupdater.ApplicationContext.NO_OXYGEN_OS;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OxygenOTAUpdate {

    private String versionNumber;
    private String otaVersionNumber;
    private String description;
    private String downloadUrl;
    private int downloadSize;
    private String filename;
    private String MD5Sum;
    private String information;
    private boolean updateInformationAvailable;
    private boolean systemIsUpToDate;



    public String getVersionNumber() {
        return versionNumber;
    }

    @JsonProperty("version_number")
    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }


    public String getOtaVersionNumber() {
        return otaVersionNumber;
    }

    @JsonProperty("ota_version_number")
    public void setOtaVersionNumber(String otaVersionNumber) {
        this.otaVersionNumber = otaVersionNumber;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getDownloadUrl() {
        return downloadUrl;
    }

    @JsonProperty("download_url")
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }


    public int getDownloadSize() {
        return (downloadSize / 1048576 );
    }

    @JsonProperty("download_size")
    public void setDownloadSize(int downloadSize) {
        this.downloadSize = downloadSize;
    }


    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


    public String getMD5Sum() {
        return MD5Sum;
    }

    @JsonProperty("md5sum")
    public void setMD5Sum(String MD5Sum) {
        this.MD5Sum = MD5Sum;
    }


    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public boolean isUpdateInformationAvailable() {
        return updateInformationAvailable || (versionNumber != null);
    }

    @JsonProperty("update_information_available")
    public void setUpdateInformationAvailable(boolean updateInformationAvailable) {
        this.updateInformationAvailable = updateInformationAvailable;
    }

    public boolean isSystemIsUpToDate(SettingsManager settingsManager) {
        //noinspection SimplifiableIfStatement
        if(settingsManager != null && settingsManager.getPreference(SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true)) {
            return systemIsUpToDate;
        } else {
            return false;
        }
    }

    /**
     * Additional check if system is up to date by comparing version Strings.
     * This is needed to show the "System is up to date" message for full updates as incremental (parent) versions are not checked there.
     * @return True if the system is up to date, false if not.
     */
    public boolean isSystemUpToDateStringCheck(SettingsManager settingsManager, Activity activity) {
        if(settingsManager.getPreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true)) {
            // This grabs the Oxygen OS version from build.prop. As there is no direct SDK way to do this, it has to be done in this way.
            SystemVersionProperties systemVersionProperties = ((ApplicationContext)activity.getApplication()).getSystemVersionProperties();

            String oxygenOSVersion = systemVersionProperties.getOxygenOSVersion();
            String newOxygenOSVersion = getVersionNumber();

            if(newOxygenOSVersion == null || newOxygenOSVersion.isEmpty()) {
                return false;
            }

            if (oxygenOSVersion == null || oxygenOSVersion.isEmpty() || oxygenOSVersion.equals(NO_OXYGEN_OS)) {
                return false;
            } else {
                if (newOxygenOSVersion.equals(oxygenOSVersion)) {
                    return true;
                } else {
                    // remove incremental version naming.
                    newOxygenOSVersion = newOxygenOSVersion.replace(" Incremental", "");
                    if (newOxygenOSVersion.equals(oxygenOSVersion)) {
                        return true;
                    } else {
                        newOxygenOSVersion = newOxygenOSVersion.replace("-", " ");
                        oxygenOSVersion = oxygenOSVersion.replace("-", " ");
                        return newOxygenOSVersion.contains(oxygenOSVersion);
                    }
                }
            }
        }
        else {
            return false; // Always show update info if user does not want to see if system is up to date.
        }
    }


    public boolean isSystemIsUpToDateCheck() {
        return systemIsUpToDate;
    }

    @JsonProperty("system_is_up_to_date")
    public void setSystemIsUpToDate(boolean systemIsUpToDate) {
        this.systemIsUpToDate = systemIsUpToDate;
    }

 }
