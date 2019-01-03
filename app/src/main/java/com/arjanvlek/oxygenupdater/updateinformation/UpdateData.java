package com.arjanvlek.oxygenupdater.updateinformation;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseBooleanArray;

import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.versionformatter.FormattableUpdateData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateData implements Parcelable, FormattableUpdateData {

    private Long id;
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


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
        return downloadSize;
    }

    public int getDownloadSizeInMegabytes() {
        return (downloadSize / 1048576);
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

    public boolean isSystemIsUpToDateCheck(SettingsManager settingsManager) {
        //noinspection SimplifiableIfStatement
        if (settingsManager != null && settingsManager.getPreference(SettingsManager.PROPERTY_ADVANCED_MODE, false)) {
            return false;
        }

        return systemIsUpToDate;
    }

    public boolean isSystemIsUpToDate() {
        return systemIsUpToDate;
    }

    @JsonProperty("system_is_up_to_date")
    public void setSystemIsUpToDate(boolean systemIsUpToDate) {
        this.systemIsUpToDate = systemIsUpToDate;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public static final Parcelable.Creator<UpdateData> CREATOR = new Parcelable.Creator<UpdateData>() {

        @Override
        public UpdateData createFromParcel(Parcel in) {
            UpdateData data = new UpdateData();

            data.setId(in.readLong());
            data.setVersionNumber(in.readString());
            data.setOtaVersionNumber(in.readString());
            data.setDescription(in.readString());
            data.setDownloadUrl(in.readString());
            data.setDownloadSize(in.readInt());
            data.setFilename(in.readString());
            data.setMD5Sum(in.readString());
            data.setInformation(in.readString());

            SparseBooleanArray booleanValues = in.readSparseBooleanArray();
            data.setUpdateInformationAvailable(booleanValues.get(0));
            data.setSystemIsUpToDate(booleanValues.get(1));
            return data;
        }

        @Override
        public UpdateData[] newArray(int size) {
            return new UpdateData[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id != null) {
            dest.writeLong(id);
        } else {
            dest.writeLong(-1L);
        }

        dest.writeString(versionNumber);
        dest.writeString(otaVersionNumber);
        dest.writeString(description);
        dest.writeString(downloadUrl);
        dest.writeInt(downloadSize);
        dest.writeString(filename);
        dest.writeString(MD5Sum);
        dest.writeString(information);

        SparseBooleanArray booleanValues = new SparseBooleanArray();
        booleanValues.put(0, updateInformationAvailable);
        booleanValues.put(1, systemIsUpToDate);

        dest.writeSparseBooleanArray(booleanValues);
    }

    // Formatting library: interface FormattableUpdateData

    @Override
    public String getInternalVersionNumber() {
        return versionNumber;
    }

    @Override
    public String getUpdateDescription() {
        return description;
    }
}
