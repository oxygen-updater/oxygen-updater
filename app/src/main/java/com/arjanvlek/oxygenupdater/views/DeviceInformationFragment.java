package com.arjanvlek.oxygenupdater.views;

import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.DeviceInformationData;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Support.NetworkConnectionManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.List;

import static com.arjanvlek.oxygenupdater.ApplicationContext.NO_OXYGEN_OS;

public class DeviceInformationFragment extends AbstractFragment {
    private RelativeLayout rootView;
    private AdView adView;
    private NetworkConnectionManager networkConnectionManager = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        //Inflate the layout for this fragment
        rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_deviceinformation, container, false);
        if(getActivity() != null) {
            networkConnectionManager = new NetworkConnectionManager(getActivity().getApplicationContext());
        }
        return rootView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if(isAdded()) {
            displayDeviceInformation(null); // To fast load device information with generic / non-pretty device name.
            new GetDevices().execute();
        }
    }

    private void displayDeviceInformation(@Nullable List<Device> devices) {
        if(isAdded()) {
            DeviceInformationData deviceInformationData = new DeviceInformationData();

            String deviceName = null;
            SystemVersionProperties systemVersionProperties = getApplicationContext().getSystemVersionProperties();

            if (devices != null) {
                for (Device device : devices) {
                    if (device.getModelNumber() != null && device.getModelNumber().equals(systemVersionProperties.getOxygenDeviceName())) {
                        deviceName = device.getDeviceName();
                    }
                }
            }


            TextView deviceNameView = (TextView) rootView.findViewById(R.id.device_information_header);
            if (devices == null || deviceName == null) {
                deviceNameView.setText(String.format(getString(R.string.device_information_device_name), deviceInformationData.getDeviceManufacturer(), deviceInformationData.getDeviceName()));
            } else {
                deviceNameView.setText(deviceName);
            }

            TextView socView = (TextView) rootView.findViewById(R.id.device_information_soc_field);
            socView.setText(deviceInformationData.getSoc());

            String cpuFreqString = deviceInformationData.getCpuFrequency();
            TextView cpuFreqView = (TextView) rootView.findViewById(R.id.device_information_cpu_freq_field);
            if (!cpuFreqString.equals(DeviceInformationData.UNKNOWN)) {
                cpuFreqView.setText(String.format(getString(R.string.device_information_gigahertz), deviceInformationData.getCpuFrequency()));
            } else {
                cpuFreqView.setText(getString(R.string.device_information_unknown));
            }

            long totalMemory = 0;
            try {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) getActivity().getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(mi);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    totalMemory = mi.totalMem / 1048576L;
                } else {
                    totalMemory = 1;
                }
            } catch (Exception ignored) {

            }
            TextView memoryView = (TextView) rootView.findViewById(R.id.device_information_memory_field);
            if (totalMemory != 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    memoryView.setText(String.format(getString(R.string.download_size_megabyte), totalMemory));
                } else {
                    View memoryLabel = rootView.findViewById(R.id.device_information_memory_label);
                    memoryLabel.setVisibility(View.GONE);
                    memoryView.setVisibility(View.GONE);
                }
            } else {
                memoryView.setText(getString(R.string.device_information_unknown));
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

            if (networkConnectionManager != null && networkConnectionManager.checkNetworkConnection()) {
                showAds();
            } else {
                hideAds();
            }
        }
    }

    private class GetDevices extends AsyncTask<Void, Void, List<Device>> {

        @Override
        protected List<Device> doInBackground(Void... params) {
            try {
                return getApplicationContext().getDevices();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Device> devices) {
            displayDeviceInformation(devices);
        }
    }

    private void hideAds() {
        if (adView != null) {
            adView.destroy();
        }
    }

    private void showAds() {

        // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
        // values/strings.xml.
        adView = (AdView) rootView.findViewById(R.id.device_information_banner_field);

        // Create an ad request. Check logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(ADS_TEST_DEVICE_ID_OWN_DEVICE)
                .addTestDevice(ADS_TEST_DEVICE_ID_EMULATOR_1)
                .addTestDevice(ADS_TEST_DEVICE_ID_EMULATOR_2)
                .addTestDevice(ADS_TEST_DEVICE_ID_EMULATOR_3)
                .build();

        // Start loading the ad in the background.
        adView.loadAd(adRequest);
    }

    /**
     * Called when leaving the activity
     */
    @Override
    public void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }

    }

    /**
     * Called when the activity enters the foreground
     */
    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    /**
     * Called before the activity is destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
        }
    }

}
