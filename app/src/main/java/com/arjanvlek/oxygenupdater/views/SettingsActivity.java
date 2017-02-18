package com.arjanvlek.oxygenupdater.views;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.util.Pair;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber;
import com.arjanvlek.oxygenupdater.notifications.TopicSubscriptionData;

import java.util.Arrays;
import java.util.List;

import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

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

        new DeviceDataFetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        initSwitches();
    }

    private void initSwitches() {

        List<Pair<Integer, String>> switchesAndSettingsItems = Arrays.asList(
                Pair.create(R.id.settingsAppUpdatesSwitch, PROPERTY_SHOW_APP_UPDATE_MESSAGES),
                Pair.create(R.id.settingsAppMessagesSwitch, PROPERTY_SHOW_NEWS_MESSAGES),
                Pair.create(R.id.settingsImportantPushNotificationsSwitch, PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS),
                Pair.create(R.id.settingsNewVersionPushNotificationsSwitch, PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS),
                Pair.create(R.id.settingsNewDevicePushNotificationsSwitch, PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS),
                Pair.create(R.id.settingsSystemIsUpToDateSwitch, PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE)
        );

        StreamSupport.stream(switchesAndSettingsItems).forEach(pair -> {
            SwitchCompat switchView = (SwitchCompat) findViewById(pair.first);
            switchView.setOnCheckedChangeListener(((buttonView, isChecked) -> settingsManager.savePreference(pair.second, isChecked)));
            switchView.setChecked(settingsManager.getPreference(pair.second, true));
        });
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
            SystemVersionProperties systemVersionProperties = ((ApplicationContext) getApplication()).getSystemVersionProperties();

            Spinner spinner = (Spinner) findViewById(R.id.settingsDeviceSpinner);

            // Set the spinner to the previously selected device.
            final int recommendedPosition = StreamSupport.stream(devices)
                    .filter(d -> d.getProductName() != null && d.getProductName().equals(systemVersionProperties.getOxygenDeviceName()))
                    .filter(d -> d.getChipSet().equals("NOT_SET") || d.getChipSet().equals(Build.BOARD))
                    .mapToInt(devices::indexOf).findAny().orElse(-1);

            final int selectedPosition = StreamSupport.stream(devices)
                    .filter(d -> d.getId() == (long) settingsManager.getPreference(PROPERTY_DEVICE_ID))
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

            if(spinner != null) {
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

                        new UpdateDataFetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, device.getId());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            }


            deviceProgressBar.setVisibility(View.GONE);

        } else {
            hideDeviceAndUpdateMethodSettings();
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

    private class UpdateDataFetcher extends AsyncTask<Long, Integer, List<UpdateMethod>> {

        @Override
        public void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public List<UpdateMethod> doInBackground(Long... deviceIds) {
            return getServerConnector().getUpdateMethods(deviceIds[0]);
        }

        @Override
        public void onPostExecute(List<UpdateMethod> updateMethods) {
            fillUpdateSettings(updateMethods);

        }
    }

    private void fillUpdateSettings(final List<UpdateMethod> updateMethods) {
        if(updateMethods != null && !updateMethods.isEmpty()) {
            Spinner spinner = (Spinner) findViewById(R.id.settingsUpdateMethodSpinner);
            long currentUpdateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID);

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

            if(spinner != null) {
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
                        if(getAppApplicationContext().checkPlayServices(getParent(), false)) {
                            // Subscribe to notifications for the newly selected device and update method
                            TopicSubscriptionData data = new TopicSubscriptionData(getAppApplicationContext(), settingsManager.getPreference(PROPERTY_DEVICE_ID), settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID));
                            new NotificationTopicSubscriber().execute(data);
                        } else {
                            try {
                                Toast.makeText(getApplication().getApplicationContext(), getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show();
                            } catch (Exception ignored) {

                            }
                        }

                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }

            updateMethodsProgressBar.setVisibility(View.GONE);
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
