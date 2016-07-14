package com.arjanvlek.oxygenupdater.views;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.Model.UpdateMethod;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Support.CustomDropdown;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;

import java.util.ArrayList;
import java.util.List;

import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_RECEIVE_WARNING_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES;

public class SettingsActivity extends AbstractActivity {
    private ProgressBar progressBar;
    private ProgressBar deviceProgressBar;
    private ProgressBar updateMethodsProgressBar;
    private SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        settingsManager = new SettingsManager(getApplicationContext());
        progressBar = (ProgressBar) findViewById(R.id.settingsProgressBar);
        deviceProgressBar = (ProgressBar) findViewById(R.id.settingsDeviceProgressBar);
        updateMethodsProgressBar = (ProgressBar) findViewById(R.id.settingsUpdateMethodProgressBar);
        try {
            progressBar.setVisibility(View.VISIBLE);
            deviceProgressBar.setVisibility(View.VISIBLE);
        } catch (Exception ignored) {

        }
        new DeviceDataFetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        initSwitches();
    }

    private void initSwitches() {
        SwitchCompat appUpdatesSwitch = (SwitchCompat) findViewById(R.id.settingsAppUpdatesSwitch);
        if (appUpdatesSwitch != null) {
            appUpdatesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settingsManager.saveBooleanPreference(PROPERTY_SHOW_APP_UPDATE_MESSAGES, isChecked);
                }
            });
            appUpdatesSwitch.setChecked(settingsManager.showAppUpdateMessages());
        }

        SwitchCompat appMessagesSwitch = (SwitchCompat) findViewById(R.id.settingsAppMessagesSwitch);
        if (appMessagesSwitch != null) {
            appMessagesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settingsManager.saveBooleanPreference(PROPERTY_SHOW_NEWS_MESSAGES, isChecked);
                }
            });
            appMessagesSwitch.setChecked(settingsManager.showNewsMessages());
        }

        SwitchCompat importantPushNotificationsSwitch = (SwitchCompat) findViewById(R.id.settingsImportantPushNotificationsSwitch);
        if (importantPushNotificationsSwitch != null) {
            importantPushNotificationsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settingsManager.saveBooleanPreference(PROPERTY_RECEIVE_WARNING_NOTIFICATIONS, isChecked);
                }
            });
            importantPushNotificationsSwitch.setChecked(settingsManager.receiveWarningNotifications());
        }

        SwitchCompat newVersionPushNotificationsSwitch = (SwitchCompat) findViewById(R.id.settingsNewVersionPushNotificationsSwitch);
        if (newVersionPushNotificationsSwitch != null) {
            newVersionPushNotificationsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settingsManager.saveBooleanPreference(PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS, isChecked);
                }
            });
            newVersionPushNotificationsSwitch.setChecked(settingsManager.receiveSystemUpdateNotifications());
        }

        SwitchCompat newDevicePushNotificationsSwitch = (SwitchCompat) findViewById(R.id.settingsNewDevicePushNotificationsSwitch);
        if (newDevicePushNotificationsSwitch != null) {
            newDevicePushNotificationsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settingsManager.saveBooleanPreference(PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS, isChecked);
                }
            });
            newDevicePushNotificationsSwitch.setChecked(settingsManager.receiveNewDeviceNotifications());
        }

        SwitchCompat systemIsUpToDateSwitch = (SwitchCompat) findViewById(R.id.settingsSystemIsUpToDateSwitch);
        if (systemIsUpToDateSwitch != null) {
            systemIsUpToDateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settingsManager.saveBooleanPreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, isChecked);
                }
            });
            systemIsUpToDateSwitch.setChecked(settingsManager.showIfSystemIsUpToDate());
        }
    }

    private class DeviceDataFetcher extends AsyncTask<Void, Integer, List<Device>> {

        @Override
        public List<Device> doInBackground(Void... voids) {
            return getAppApplicationContext().getDevices();
        }

        @Override
        public void onPostExecute(List<Device> devices) {
            fillDeviceSettings(devices);
        }
    }

    private void fillDeviceSettings(final List<Device> devices) {
        if (devices != null && !devices.isEmpty()) {
            Spinner spinner = (Spinner) findViewById(R.id.settingsDeviceSpinner);

            // Set the spinner to the previously selected device.
            Integer position = null;
            int tempRecommendedPosition = -1;
            String currentDeviceName = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE);
            SystemVersionProperties systemVersionProperties = ((ApplicationContext)getApplication()).getSystemVersionProperties();
            if (currentDeviceName != null) {
                for (int i = 0; i < devices.size(); i++) {
                    if (devices.get(i).getDeviceName().equals(currentDeviceName)) {
                        position = i;
                    }
                    if (devices.get(i).getModelNumber() != null && devices.get(i).getModelNumber().equals(systemVersionProperties.getOxygenDeviceName())) {
                        tempRecommendedPosition = i;
                    }
                }
            }

            final int recommendedPosition = tempRecommendedPosition;

            ArrayAdapter<Device> adapter = new ArrayAdapter<Device>(this, android.R.layout.simple_spinner_item, devices) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, devices, recommendedPosition, this.getContext());
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, devices, recommendedPosition, this.getContext());
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            if(spinner != null) {
                spinner.setAdapter(adapter);
                if (position != null) {
                    spinner.setSelection(position);
                }

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        Device device = (Device) adapterView.getItemAtPosition(i);
                        settingsManager.savePreference(SettingsManager.PROPERTY_DEVICE, device.getDeviceName());
                        settingsManager.saveLongPreference(SettingsManager.PROPERTY_DEVICE_ID, device.getId());

                        try {
                            updateMethodsProgressBar.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.VISIBLE);
                        } catch (Exception ignored) {
                        }

                        new UpdateDataFetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, device.getId());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            }


            try {
                deviceProgressBar.setVisibility(View.GONE);
            } catch (Exception ignored) {

            }
        } else {
            hideDeviceAndUpdateMethodSettings();
            progressBar.setVisibility(View.GONE);

        }
    }

    @SuppressWarnings("ConstantConditions")
    private void hideDeviceAndUpdateMethodSettings() {
        findViewById(R.id.settingsDeviceSpinner).setVisibility(View.GONE);
        findViewById(R.id.settingsDeviceProgressBar).setVisibility(View.GONE);
        findViewById(R.id.settingsDeviceView).setVisibility(View.GONE);
        findViewById(R.id.settingsUpdateMethodProgressBar).setVisibility(View.GONE);
        findViewById(R.id.settingsUpdateMethodSpinner).setVisibility(View.GONE);
        findViewById(R.id.settingsUpdateMethodView).setVisibility(View.GONE);
        findViewById(R.id.settingsDescriptionView).setVisibility(View.GONE);
        findViewById(R.id.settingsUpperDivisor).setVisibility(View.GONE);
    }

    private class UpdateDataFetcher extends AsyncTask<Long, Integer, List<UpdateMethod>> {

        @Override
        public List<UpdateMethod> doInBackground(Long... deviceIds) {
            long deviceId = deviceIds[0];
            return getServerConnector().getUpdateMethods(deviceId);
        }

        @Override
        public void onPostExecute(List<UpdateMethod> updateMethods) {
            fillUpdateSettings(updateMethods);

        }
    }

    private void fillUpdateSettings(final List<UpdateMethod> updateMethods) {
        if(updateMethods != null && !updateMethods.isEmpty()) {
            Spinner spinner = (Spinner) findViewById(R.id.settingsUpdateMethodSpinner);
            String currentUpdateMethod = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD);
            Integer position = null;
            final List<Integer> recommendedPositions = new ArrayList<>();

            for (int i = 0; i < updateMethods.size(); i++) {
                if (currentUpdateMethod != null && updateMethods.get(i).getUpdateMethod().equals(currentUpdateMethod) || updateMethods.get(i).getUpdateMethodNl().equalsIgnoreCase(currentUpdateMethod)) {
                    position = i;
                }
                if(updateMethods.get(i).isRecommended()) {
                    recommendedPositions.add(i);
                }
            }

            ArrayAdapter<UpdateMethod> adapter = new ArrayAdapter<UpdateMethod>(this, android.R.layout.simple_spinner_item, updateMethods) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    return CustomDropdown.initCustomUpdateMethodDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, updateMethods, recommendedPositions, this.getContext());
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    return CustomDropdown.initCustomUpdateMethodDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, updateMethods, recommendedPositions, this.getContext());
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            if(spinner != null) {
                spinner.setAdapter(adapter);

                if (position != null) {
                    spinner.setSelection(position);
                }

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        UpdateMethod updateMethod = (UpdateMethod) adapterView.getItemAtPosition(i);
                        try {
                            progressBar.setVisibility(View.VISIBLE);
                        } catch (Exception ignored) {
                        }

                        settingsManager.saveLongPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, updateMethod.getId());
                        settingsManager.savePreference(SettingsManager.PROPERTY_UPDATE_METHOD, updateMethod.getUpdateMethod());
                        try {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                        } catch (Exception ignored) {

                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }

            try {
                updateMethodsProgressBar.setVisibility(View.GONE);
            } catch (Exception ignored) {

            }
        } else {
            hideDeviceAndUpdateMethodSettings();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showSettingsWarning() {
        Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show();
    }


    @Override
    public void onBackPressed() {
        if (settingsManager.checkIfSettingsAreValid() && !progressBar.isShown()) {
            NavUtils.navigateUpFromSameTask(this);
        } else {
            showSettingsWarning();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (settingsManager.checkIfSettingsAreValid() && !progressBar.isShown()) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                } else {
                    showSettingsWarning();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

}
