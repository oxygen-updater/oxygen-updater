package com.arjanvlek.oxygenupdater.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.domain.UpdateMethod;
import com.arjanvlek.oxygenupdater.internal.ThreeTuple;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber;
import com.arjanvlek.oxygenupdater.views.AbstractActivity;
import com.arjanvlek.oxygenupdater.views.CustomDropdown;

import java.util.Arrays;
import java.util.List;

import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_NEWS_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPLOAD_LOGS;

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

        progressBar.setVisibility(View.VISIBLE);
        deviceProgressBar.setVisibility(View.VISIBLE);

        getApplicationData().getServerConnector().getDevices(true, this::fillDeviceSettings);

        initSwitches();
    }

    private void initSwitches() {
        List<ThreeTuple<Integer, String, Boolean>> switchesAndSettingsItemsAndDefaultValues = Arrays.asList(
                ThreeTuple.create(R.id.settingsAppUpdatesSwitch, PROPERTY_SHOW_APP_UPDATE_MESSAGES, true),
                ThreeTuple.create(R.id.settingsAppMessagesSwitch, PROPERTY_SHOW_NEWS_MESSAGES, true),
                ThreeTuple.create(R.id.settingsImportantPushNotificationsSwitch, PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS, true),
                ThreeTuple.create(R.id.settingsNewVersionPushNotificationsSwitch, PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS, true),
                ThreeTuple.create(R.id.settingsNewDevicePushNotificationsSwitch, PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS, true),
                ThreeTuple.create(R.id.settingsNewsPushNotificationsSwitch, PROPERTY_RECEIVE_NEWS_NOTIFICATIONS, true),
                ThreeTuple.create(R.id.settingsSystemIsUpToDateSwitch, PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true),
                ThreeTuple.create(R.id.settingsUploadLogsSwitch, PROPERTY_UPLOAD_LOGS, true)
        );

        StreamSupport.stream(switchesAndSettingsItemsAndDefaultValues).forEach(tuple -> {
            SwitchCompat switchView = (SwitchCompat) findViewById(tuple.getFirst());
            switchView.setOnCheckedChangeListener(((buttonView, isChecked) -> settingsManager.savePreference(tuple.getSecond(), isChecked)));
            switchView.setChecked(settingsManager.getPreference(tuple.getSecond(), tuple.getThird()));
        });
    }

    private void fillDeviceSettings(final List<Device> devices) {
        if (devices != null && !devices.isEmpty()) {
            SystemVersionProperties systemVersionProperties = ((ApplicationData) getApplication()).getSystemVersionProperties();

            Spinner spinner = (Spinner) findViewById(R.id.settingsDeviceSpinner);

            if (spinner == null) return;

            // Set the spinner to the previously selected device.
            final int recommendedPosition = StreamSupport.stream(devices)
                    .filter(d -> d.getProductName() != null && d.getProductName().equals(systemVersionProperties.getOxygenDeviceName()))
                    .mapToInt(devices::indexOf).findAny().orElse(-1);

            final int selectedPosition = StreamSupport.stream(devices)
                    .filter(d -> d.getId() == settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L))
                    .mapToInt(devices::indexOf).findAny().orElse(-1);


            ArrayAdapter<Device> adapter = new ArrayAdapter<Device>(this, android.R.layout.simple_spinner_item, devices) {

                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, devices, recommendedPosition, this.getContext());
                }

                @Override
                public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                    return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, devices, recommendedPosition, this.getContext());
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinner.setAdapter(adapter);
            if (selectedPosition != -1) {
                spinner.setSelection(selectedPosition);
            }

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Device device = (Device) adapterView.getItemAtPosition(i);
                    settingsManager.savePreference(SettingsManager.PROPERTY_DEVICE, device.getName());
                    settingsManager.savePreference(SettingsManager.PROPERTY_DEVICE_ID, device.getId());

                    updateMethodsProgressBar.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);

                    getApplicationData().getServerConnector().getUpdateMethods(device.getId(), SettingsActivity.this::fillUpdateMethodSettings);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });


            deviceProgressBar.setVisibility(View.GONE);

        } else {
            hideDeviceAndUpdateMethodSettings();
            progressBar.setVisibility(View.GONE);

        }
    }

    private void fillUpdateMethodSettings(final List<UpdateMethod> updateMethods) {
        if(updateMethods != null && !updateMethods.isEmpty()) {
            Spinner spinner = (Spinner) findViewById(R.id.settingsUpdateMethodSpinner);
            if (spinner == null) return;

            long currentUpdateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L);

            final int[] recommendedPositions = StreamSupport.stream(updateMethods).filter(UpdateMethod::isRecommended).mapToInt(updateMethods::indexOf).toArray();
            final int selectedPosition = StreamSupport.stream(updateMethods).filter(um -> um.getId() == currentUpdateMethodId).mapToInt(updateMethods::indexOf).findAny().orElse(-1);

            ArrayAdapter<UpdateMethod> adapter = new ArrayAdapter<UpdateMethod>(this, android.R.layout.simple_spinner_item, updateMethods) {

                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    return CustomDropdown.initCustomUpdateMethodDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, updateMethods, recommendedPositions, this.getContext());
                }

                @Override
                public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                    return CustomDropdown.initCustomUpdateMethodDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, updateMethods, recommendedPositions, this.getContext());
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinner.setAdapter(adapter);

            if (selectedPosition != -1) {
                spinner.setSelection(selectedPosition);
            }

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    UpdateMethod updateMethod = (UpdateMethod) adapterView.getItemAtPosition(i);

                    settingsManager.savePreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, updateMethod.getId());
                    settingsManager.savePreference(SettingsManager.PROPERTY_UPDATE_METHOD, updateMethod.getEnglishName());

                    // Google Play services are not required if the user doesn't notifications
                    if (getApplicationData().checkPlayServices(getParent(), false)) {
                        // Subscribe to notifications for the newly selected device and update method
                        NotificationTopicSubscriber.subscribe(getApplicationData());
                    } else {
                        Toast.makeText(getApplication().getApplicationContext(), getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show();
                    }

                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            updateMethodsProgressBar.setVisibility(View.GONE);
        } else {
            hideUpdateMethodSettings();
            progressBar.setVisibility(View.GONE);
        }
    }

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

    private void hideUpdateMethodSettings() {
        findViewById(R.id.settingsUpdateMethodProgressBar).setVisibility(View.GONE);
        findViewById(R.id.settingsUpdateMethodSpinner).setVisibility(View.GONE);
        findViewById(R.id.settingsUpdateMethodView).setVisibility(View.GONE);
        findViewById(R.id.settingsDescriptionView).setVisibility(View.GONE);
        findViewById(R.id.settingsUpperDivisor).setVisibility(View.GONE);
    }

    private void showSettingsWarning() {
        Long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L);
        Long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L);

        if (deviceId == -1L || updateMethodId == -1L) {
            Logger.logWarning("SettingsActivity", "Settings screen did *NOT* save settings correctly. Selected device id: " + deviceId + ", selected update method id: " + updateMethodId);
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show();
        } else {
            Logger.logWarning(false, "SettingsActivity", "Settings screen did *NOT* save settings correctly. Selected device id: " + deviceId + ", selected update method id: " + updateMethodId);
            Toast.makeText(this, getString(R.string.settings_saving), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onBackPressed() {
        if (settingsManager.checkIfSetupScreenHasBeenCompleted() && !progressBar.isShown()) {
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
                if (settingsManager.checkIfSetupScreenHasBeenCompleted() && !progressBar.isShown()) {
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
