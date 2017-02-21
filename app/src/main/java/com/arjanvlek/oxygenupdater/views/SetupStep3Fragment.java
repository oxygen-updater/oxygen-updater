package com.arjanvlek.oxygenupdater.views;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Support.CustomDropdown;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;

import java.util.List;

import java8.util.stream.StreamSupport;

public class SetupStep3Fragment extends AbstractFragment {

    private View rootView;
    private SettingsManager settingsManager;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_setup_3, container, false);
        settingsManager = new SettingsManager(getActivity().getApplicationContext());
        progressBar = (ProgressBar) rootView.findViewById(R.id.settingsDeviceProgressBar);
        return rootView;
    }

    public void fetchDevices() {
        getApplicationContext().getServerConnector().getDevices(this::fillDeviceSettings);
    }


    private void fillDeviceSettings(final List<Device> devices) {
        Spinner spinner = (Spinner) rootView.findViewById(R.id.settingsDeviceSpinner);

        SystemVersionProperties systemVersionProperties = ((ApplicationContext)getActivity().getApplication()).getSystemVersionProperties();

        final int selectedIndex = StreamSupport.stream(devices)
                .filter(d -> d.getProductName() != null)
                .filter(d -> d.getProductName().equals(systemVersionProperties.getOxygenDeviceName()))
                .filter(d -> (d.getChipSet().equals("NOT_SET") || d.getChipSet().equals(Build.BOARD)))
                .mapToInt(devices::indexOf).findAny().orElse(-1);

        ArrayAdapter<Device> adapter = new ArrayAdapter<Device>(getActivity(), android.R.layout.simple_spinner_item, devices) {

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, devices, selectedIndex, this.getContext());
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, devices, selectedIndex, this.getContext());
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
