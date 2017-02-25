package com.arjanvlek.oxygenupdater.views;

import android.support.v4.app.Fragment;

import com.arjanvlek.oxygenupdater.ApplicationData;

import java.util.Arrays;
import java.util.List;


public abstract class AbstractFragment extends Fragment {

    private ApplicationData applicationData;
    //Test devices for ads.
    public static final List<String> ADS_TEST_DEVICES = Arrays.asList(""); // TODO add test ads id of my phone...

    public ApplicationData getApplicationData() {
        if (applicationData == null) {
            try {
                applicationData = (ApplicationData) getActivity().getApplication();
            } catch (Exception e) {
                applicationData = new ApplicationData();
            }
        }
        return applicationData;
    }
}
