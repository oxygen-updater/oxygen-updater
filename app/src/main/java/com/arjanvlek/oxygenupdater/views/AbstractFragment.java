package com.arjanvlek.oxygenupdater.views;

import android.support.v4.app.Fragment;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.support.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public abstract class AbstractFragment extends Fragment {

    private ApplicationData applicationData;
    //Test devices for ads.
    public static final List<String> ADS_TEST_DEVICES = Collections.singletonList("0F6A86C5D00DC51588D523BE3905D484");

    public ApplicationData getApplicationData() {
        if (applicationData == null) {
            try {
                applicationData = (ApplicationData) getActivity().getApplication();
            } catch (Exception e) {
                Logger.logError("AbstractFragment", "FAILED to get application data: ", e);
            }
        }
        return applicationData;
    }
}
