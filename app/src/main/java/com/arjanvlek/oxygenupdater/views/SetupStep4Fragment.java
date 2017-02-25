package com.arjanvlek.oxygenupdater.views;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.arjanvlek.oxygenupdater.Model.UpdateMethod;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber;

import java.util.List;

import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_UPDATE_METHOD;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

public class SetupStep4Fragment extends AbstractFragment {

    private View rootView;
    private SettingsManager settingsManager;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_setup_4, container, false);
        settingsManager = new SettingsManager(getActivity().getApplicationContext());
        progressBar = (ProgressBar) rootView.findViewById(R.id.settingsUpdateMethodProgressBar);
        return rootView;
    }


    public void fetchUpdateMethods() {
        if (settingsManager.containsPreference(PROPERTY_DEVICE_ID)) {
            progressBar.setVisibility(View.VISIBLE);
            getApplicationData().getServerConnector().getUpdateMethods(settingsManager.getPreference(PROPERTY_DEVICE_ID, 1L), this::fillUpdateMethodSettings);
        }
    }

    private void fillUpdateMethodSettings(final List<UpdateMethod> updateMethods) {
        Spinner spinner = (Spinner) rootView.findViewById(R.id.settingsUpdateMethodSpinner);

        final int[] recommendedPositions = StreamSupport.stream(updateMethods).filter(UpdateMethod::isRecommended).mapToInt(updateMethods::indexOf).toArray();

        int selectedPosition = -1;

        if (settingsManager.containsPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID)) {
            long currentUpdateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID);

            selectedPosition = StreamSupport.stream(updateMethods).filter(um -> um.getId() == currentUpdateMethodId).mapToInt(updateMethods::indexOf).findAny().orElse(-1);
        }

        if(getActivity() != null) {
            ArrayAdapter<UpdateMethod> adapter = new ArrayAdapter<UpdateMethod>(getActivity(), android.R.layout.simple_spinner_item, updateMethods) {

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

                    //Set update method in preferences.
                    settingsManager.savePreference(PROPERTY_UPDATE_METHOD_ID, updateMethod.getId());
                    settingsManager.savePreference(PROPERTY_UPDATE_METHOD, updateMethod.getEnglishName());

                    if (getApplicationData().checkPlayServices(getActivity(), false)) {
                        // Subscribe to notifications
                        // Subscribe to notifications for the newly selected device and update method
                        NotificationTopicSubscriber.subscribe(getApplicationData());
                    } else {
                        Toast.makeText(getApplicationData(), getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            progressBar.setVisibility(View.GONE);
        }
    }
}
