package com.arjanvlek.oxygenupdater.setupwizard;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.views.AbstractFragment;
import com.arjanvlek.oxygenupdater.views.CustomDropdown;

import java.util.List;

import java8.util.stream.StreamSupport;

public class SetupStep3Fragment extends AbstractFragment {

    private View rootView;
    private SettingsManager settingsManager;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_setup_3, container, false);
        settingsManager = new SettingsManager(getApplicationData());
        progressBar = rootView.findViewById(R.id.introduction_step_3_device_loading_bar);
        return rootView;
    }

    public void fetchDevices() {
        getApplicationData().getServerConnector().getDevices(this::fillDeviceSettings);
    }


    private void fillDeviceSettings(final List<Device> devices) {
        if (getActivity() == null || !isAdded()) {
            return; // Do not load if app is in process of being exited when data arrives from server.
        }
        Spinner spinner = rootView.findViewById(R.id.introduction_step_3_device_dropdown);

        SystemVersionProperties systemVersionProperties = ((ApplicationData) getActivity().getApplication()).getSystemVersionProperties();

        int selectedIndex;
        int recommendedIndex;
        long deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L);

        recommendedIndex = StreamSupport.stream(devices)
                .filter(d -> d.getProductNames() != null && d.getProductNames().contains(systemVersionProperties.getOxygenDeviceName()))
                .mapToInt(devices::indexOf)
                .findAny()
                .orElse(-1);

        if(deviceId != -1L) {
            selectedIndex = StreamSupport.stream(devices)
                    .filter(d -> d.getId() == deviceId)
                    .mapToInt(devices::indexOf)
                    .findAny()
                    .orElse(-1);
        } else {
            selectedIndex = recommendedIndex;
        }

        ArrayAdapter<Device> adapter = new ArrayAdapter<Device>(getActivity(), android.R.layout.simple_spinner_item, devices) {

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, devices, recommendedIndex, this.getContext());
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, devices, recommendedIndex, this.getContext());
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if(selectedIndex != -1) {
            spinner.setSelection(selectedIndex);
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Device device = (Device)adapterView.getItemAtPosition(i);
                settingsManager.savePreference(SettingsManager.PROPERTY_DEVICE, device.getName());
                settingsManager.savePreference(SettingsManager.PROPERTY_DEVICE_ID, device.getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }

        });

        progressBar.setVisibility(View.GONE);
    }
}
