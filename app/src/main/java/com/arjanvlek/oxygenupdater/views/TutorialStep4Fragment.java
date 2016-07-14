package com.arjanvlek.oxygenupdater.views;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.arjanvlek.oxygenupdater.Model.UpdateMethod;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Support.CustomDropdown;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;

import java.util.ArrayList;
import java.util.List;

import static com.arjanvlek.oxygenupdater.Support.SettingsManager.*;

public class TutorialStep4Fragment extends AbstractFragment {

    private View rootView;
    private SettingsManager settingsManager;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_tutorial_4, container, false);
        settingsManager = new SettingsManager(getActivity().getApplicationContext());
        progressBar = (ProgressBar) rootView.findViewById(R.id.settingsUpdateMethodProgressBar);
        return rootView;
    }


    public void fetchUpdateMethods() {
        long deviceId = settingsManager.getLongPreference(PROPERTY_DEVICE_ID);
        new UpdateDataFetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, deviceId);
    }

    private class UpdateDataFetcher extends AsyncTask<Long, Integer, List<UpdateMethod>> {

        @Override
        protected void onPreExecute() {
            try {
                progressBar.setVisibility(View.VISIBLE);
            } catch (Exception ignored) {

            }
        }

        @Override
        public List<UpdateMethod> doInBackground(Long... deviceIds) {
            long deviceId = deviceIds[0];
            return getApplicationContext().getServerConnector().getUpdateMethods(deviceId);
        }

        @Override
        public void onPostExecute(List<UpdateMethod> updateMethods) {
            fillUpdateSettings(updateMethods);
        }
    }

    private void fillUpdateSettings(final List<UpdateMethod> updateMethods) {
        Spinner spinner = (Spinner) rootView.findViewById(R.id.settingsUpdateMethodSpinner);

        List<Integer> recommendedPositions = new ArrayList<>();

        for (int i = 0; i < updateMethods.size(); i++) {
            if(updateMethods.get(i).isRecommended()) {
                recommendedPositions.add(i);
            }
        }

        final List<Integer> recommendedPositionsDefinitive = recommendedPositions;

        if (settingsManager.containsPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID)) {
            for(UpdateMethod updateMethod : updateMethods) {
                if(updateMethod.getId() == settingsManager.getLongPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID) ){
                    recommendedPositions = new ArrayList<>();
                    recommendedPositions.add(updateMethods.indexOf(updateMethod));
                }
            }
        }

        if(getActivity() != null) {
            ArrayAdapter<UpdateMethod> adapter = new ArrayAdapter<UpdateMethod>(getActivity(), android.R.layout.simple_spinner_item, updateMethods) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    return CustomDropdown.initCustomUpdateMethodDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, updateMethods, recommendedPositionsDefinitive, this.getContext());
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    return CustomDropdown.initCustomUpdateMethodDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, updateMethods, recommendedPositionsDefinitive, this.getContext());
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            if(recommendedPositions.size() > 0) {
                spinner.setSelection(recommendedPositions.get(0));
            }
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    UpdateMethod updateMethod= (UpdateMethod) adapterView.getItemAtPosition(i);

                    //Set update method in preferences.
                    settingsManager.saveLongPreference(PROPERTY_UPDATE_METHOD_ID, updateMethod.getId());
                    settingsManager.savePreference(PROPERTY_UPDATE_METHOD, updateMethod.getUpdateMethod());
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            try {
                progressBar.setVisibility(View.GONE);
            } catch (Exception ignored) {

            }
        }
    }
}
