package com.arjanvlek.oxygenupdater.setupwizard;

import android.app.AlertDialog;
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

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.UpdateMethod;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.views.AbstractFragment;
import com.arjanvlek.oxygenupdater.views.CustomDropdown;

import java.util.List;

import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

public class SetupStep4Fragment extends AbstractFragment {

    private View rootView;
    private SettingsManager settingsManager;
    private ProgressBar progressBar;
    private boolean rootMessageShown = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_setup_4, container, false);

        if (getActivity() == null) {
            throw new RuntimeException("SetupStep4Fragment: Can not initialize: not called from Activity");
        }

        settingsManager = new SettingsManager(getActivity().getApplicationContext());
        progressBar = rootView.findViewById(R.id.introduction_step_4_update_method_progress_bar);


        return rootView;
    }


    public void fetchUpdateMethods() {
        if(!rootMessageShown) {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.root_check_title));
                builder.setMessage(getString(R.string.root_check_message));
                builder.setOnDismissListener(dialog -> {
                    rootMessageShown = true;
                    fetchUpdateMethods();
                });
                builder.setPositiveButton(getString(R.string.download_error_close), (dialog, which) -> {
                    rootMessageShown = true;
                    fetchUpdateMethods();
                });
                builder.show();
            } catch (Throwable e) {
                Logger.logError("SetupStep4", "Failed to display root check dialog: " , e);
                rootMessageShown = true;
                fetchUpdateMethods();
            }
        } else {
            if (settingsManager.containsPreference(PROPERTY_DEVICE_ID)) {
                progressBar.setVisibility(View.VISIBLE);

                getApplicationData().getServerConnector().getUpdateMethods(settingsManager.getPreference(PROPERTY_DEVICE_ID, 1L), this::fillUpdateMethodSettings);
            }
        }
    }

    private void fillUpdateMethodSettings(final List<UpdateMethod> updateMethods) {
        Spinner spinner = rootView.findViewById(R.id.introduction_step_4_update_method_dropdown);

        final int[] recommendedPositions = StreamSupport
                .stream(updateMethods)
                .filter(UpdateMethod::isRecommended)
                .mapToInt(updateMethods::indexOf)
                .toArray();

        int selectedPosition = -1;
        long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L);

        if(updateMethodId != -1L) {
            selectedPosition = StreamSupport
                    .stream(updateMethods)
                    .filter(updateMethod -> updateMethod.getId() == updateMethodId)
                    .mapToInt(updateMethods::indexOf)
                    .findAny()
                    .orElse(-1);
        } else if(recommendedPositions.length > 0) {
            selectedPosition = recommendedPositions[recommendedPositions.length - 1];
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
