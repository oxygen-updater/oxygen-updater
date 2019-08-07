package com.arjanvlek.oxygenupdater.deviceinformation;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.views.AbstractFragment;

import java.util.List;

import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning;

public class DeviceInformationFragment extends AbstractFragment {

	private static final String TAG = "DeviceInformationFragment";
	private NestedScrollView rootView;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		//Inflate the layout for this fragment
		rootView = (NestedScrollView) inflater.inflate(R.layout.fragment_device_information, container, false);
		return rootView;
	}


	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		if (!isAdded()) {
			logError(TAG, new OxygenUpdaterException("Fragment not added. Can not create the view!"));
			return;
		}

		displayDeviceInformation();
		getApplicationData().getServerConnector().getDevices(this::displayFormattedDeviceName);
	}

	private void displayFormattedDeviceName(List<Device> devices) {
		if (!isAdded()) {
			logError(TAG, new OxygenUpdaterException("Fragment not added. Can not display formatted device name!"));
			return;
		}

		TextView deviceNameView = rootView.findViewById(R.id.device_information_header);
		SystemVersionProperties systemVersionProperties = getApplicationData().getSystemVersionProperties();

		if (devices != null) {
			StreamSupport.stream(devices)
					.filter(device -> device.getProductNames() != null && device.getProductNames().contains(systemVersionProperties.getOxygenDeviceName()))
					.findAny()
					.ifPresent(device -> deviceNameView.setText(device.getName()));
		}
	}

	private void displayDeviceInformation() {
		if (!isAdded()) {
			logError(TAG, new OxygenUpdaterException("Fragment not added. Can not display device information!"));
			return;
		}

		DeviceInformationData deviceInformationData = new DeviceInformationData();
		SystemVersionProperties systemVersionProperties = getApplicationData().getSystemVersionProperties();

		// Device name (in top)
		TextView deviceNameView = rootView.findViewById(R.id.device_information_header);
		deviceNameView.setText(String.format(getString(R.string.device_information_device_name), deviceInformationData.getDeviceManufacturer(), deviceInformationData.getDeviceName()));

		// SoC
		TextView socView = rootView.findViewById(R.id.device_information_soc_field);
		socView.setText(deviceInformationData.getSoc());

		// CPU Frequency (if available)
		String cpuFreqString = deviceInformationData.getCpuFrequency();
		TextView cpuFreqView = rootView.findViewById(R.id.device_information_cpu_freq_field);

		if (!cpuFreqString.equals(DeviceInformationData.UNKNOWN)) {
			cpuFreqView.setText(String.format(getString(R.string.device_information_gigahertz), deviceInformationData.getCpuFrequency()));
		} else {
			cpuFreqView.setText(getString(R.string.device_information_unknown));
		}

		long totalMemory;
		try {
			ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
			ActivityManager activityManager = (ActivityManager) Utils.getSystemService(getApplicationData(), Context.ACTIVITY_SERVICE);
			activityManager.getMemoryInfo(mi);
			totalMemory = mi.totalMem / 1048576L;
		} catch (Exception e) {
			logWarning("DeviceInformationFragment", "Memory information is unavailable due to error", e);
			totalMemory = 0;
		}

		// Total amount of RAM (if available)
		TextView memoryView = rootView.findViewById(R.id.device_information_memory_field);

		if (totalMemory != 0) {
			memoryView.setText(String.format(getString(R.string.download_size_megabyte), totalMemory));
		} else {
			View memoryLabel = rootView.findViewById(R.id.device_information_memory_label);
			memoryLabel.setVisibility(View.GONE);
			memoryView.setVisibility(View.GONE);
		}

		// Oxygen OS version (if available)
		TextView oxygenOsVersionView = rootView.findViewById(R.id.device_information_oxygen_os_ver_field);

		if (!systemVersionProperties.getOxygenOSVersion().equals(NO_OXYGEN_OS)) {
			oxygenOsVersionView.setText(systemVersionProperties.getOxygenOSVersion());

		} else {
			TextView oxygenOsVersionLabel = rootView.findViewById(R.id.device_information_oxygen_os_ver_label);
			oxygenOsVersionLabel.setVisibility(View.GONE);
			oxygenOsVersionView.setVisibility(View.GONE);
		}

		// Oxygen OS OTA version (if available)
		TextView oxygenOsOtaVersionView = rootView.findViewById(R.id.device_information_oxygen_os_ota_ver_field);

		if (!systemVersionProperties.getOxygenOSOTAVersion().equals(NO_OXYGEN_OS)) {
			oxygenOsOtaVersionView.setText(getString(R.string.device_information_oxygen_os_ota_version, systemVersionProperties
					.getOxygenOSOTAVersion()));

		} else {
			oxygenOsOtaVersionView.setVisibility(View.GONE);
		}

		// Android version
		TextView osVerView = rootView.findViewById(R.id.device_information_os_ver_field);
		osVerView.setText(deviceInformationData.getOsVersion());

		// Incremental OS version
		TextView osIncrementalView = rootView.findViewById(R.id.device_information_incremental_os_ver_field);
		osIncrementalView.setText(deviceInformationData.getIncrementalOsVersion());

		// Security Patch Date (if available)
		TextView osPatchDateView = rootView.findViewById(R.id.device_information_os_patch_level_field);
		String securityPatchDate = systemVersionProperties.getSecurityPatchDate();

		if (!securityPatchDate.equals(NO_OXYGEN_OS)) {
			osPatchDateView.setText(securityPatchDate);
		} else {
			TextView osPatchDateLabel = rootView.findViewById(R.id.device_information_os_patch_level_label);
			osPatchDateLabel.setVisibility(View.GONE);
			osPatchDateView.setVisibility(View.GONE);
		}

		// Serial number (Android 7.1.2 and lower only)
		TextView serialNumberView = rootView.findViewById(R.id.device_information_serial_number_field);
		String serialNumber = deviceInformationData.getSerialNumber();

		if (serialNumber != null && !serialNumber.equals(DeviceInformationData.UNKNOWN)) {
			serialNumberView.setText(deviceInformationData.getSerialNumber());
		} else {
			View serialNumberLabel = rootView.findViewById(R.id.device_information_serial_number_label);
			serialNumberLabel.setVisibility(View.GONE);
			serialNumberView.setVisibility(View.GONE);
		}
	}

}
