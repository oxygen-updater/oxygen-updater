package com.arjanvlek.oxygenupdater.views;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.DeviceInformationData;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.R;

import java.util.List;

import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.ApplicationContext.NO_OXYGEN_OS;

public class DeviceInformationFragment extends AbstractFragment {
    private RelativeLayout rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        //Inflate the layout for this fragment
        rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_deviceinformation, container, false);
        return rootView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if(isAdded()) {
            displayDeviceInformation();
            getApplicationContext().getDevices(this::displayDeviceName);
        }
    }

    private void displayDeviceName(List<Device> devices) {

        if(isAdded()) {
            TextView deviceNameView = (TextView) rootView.findViewById(R.id.device_information_header);
            SystemVersionProperties systemVersionProperties = getApplicationContext().getSystemVersionProperties();

            if (devices != null) {
                StreamSupport.stream(devices)
                        .filter(device -> device.getProductName() != null && device.getProductName().equals(systemVersionProperties.getOxygenDeviceName()))
                        .forEach(device -> deviceNameView.setText(device.getName()));
            }
        }
    }

    private void displayDeviceInformation() {
        if(isAdded()) {
            DeviceInformationData deviceInformationData = new DeviceInformationData();
            SystemVersionProperties systemVersionProperties = getApplicationContext().getSystemVersionProperties();

            TextView deviceNameView = (TextView) rootView.findViewById(R.id.device_information_header);
            deviceNameView.setText(String.format(getString(R.string.device_information_device_name), deviceInformationData.getDeviceManufacturer(), deviceInformationData.getDeviceName()));

            TextView socView = (TextView) rootView.findViewById(R.id.device_information_soc_field);
            socView.setText(deviceInformationData.getSoc());

            String cpuFreqString = deviceInformationData.getCpuFrequency();
            TextView cpuFreqView = (TextView) rootView.findViewById(R.id.device_information_cpu_freq_field);
            if (!cpuFreqString.equals(DeviceInformationData.UNKNOWN)) {
                cpuFreqView.setText(String.format(getString(R.string.device_information_gigahertz), deviceInformationData.getCpuFrequency()));
            } else {
                cpuFreqView.setText(getString(R.string.device_information_unknown));
            }

            long totalMemory;
            try {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) getActivity().getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(mi);
                totalMemory = mi.totalMem / 1048576L;
            } catch (Exception ignored) {
                totalMemory = 0;
            }

            TextView memoryView = (TextView) rootView.findViewById(R.id.device_information_memory_field);
            if (totalMemory != 0) {
                memoryView.setText(String.format(getString(R.string.download_size_megabyte), totalMemory));
            } else {
                View memoryLabel = rootView.findViewById(R.id.device_information_memory_label);
                memoryLabel.setVisibility(View.GONE);
                memoryView.setVisibility(View.GONE);
            }

            TextView oxygenOsVersionView = (TextView) rootView.findViewById(R.id.device_information_oxygen_os_ver_field);

            if (!systemVersionProperties.getOxygenOSVersion().equals(NO_OXYGEN_OS)) {
                oxygenOsVersionView.setText(systemVersionProperties.getOxygenOSVersion());

            } else {
                TextView oxygenOsVersionLabel = (TextView) rootView.findViewById(R.id.device_information_oxygen_os_ver_label);
                oxygenOsVersionLabel.setVisibility(View.GONE);
                oxygenOsVersionView.setVisibility(View.GONE);
            }

            TextView osVerView = (TextView) rootView.findViewById(R.id.device_information_os_ver_field);
            osVerView.setText(deviceInformationData.getOsVersion());

            TextView osIncrementalView = (TextView) rootView.findViewById(R.id.device_information_incremental_os_ver_field);
            osIncrementalView.setText(deviceInformationData.getIncrementalOsVersion());

            TextView osPatchDateView = (TextView) rootView.findViewById(R.id.device_information_os_patch_level_field);

            if (!systemVersionProperties.getSecurityPatchDate().equals(NO_OXYGEN_OS)) {
                osPatchDateView.setText(systemVersionProperties.getSecurityPatchDate());
            } else {
                TextView osPatchDateLabel = (TextView) rootView.findViewById(R.id.device_information_os_patch_level_label);
                osPatchDateLabel.setVisibility(View.GONE);
                osPatchDateView.setVisibility(View.GONE);
            }

            TextView serialNumberView = (TextView) rootView.findViewById(R.id.device_information_serial_number_field);
            serialNumberView.setText(deviceInformationData.getSerialNumber());
        }
    }

}
