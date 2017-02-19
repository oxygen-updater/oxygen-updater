package com.arjanvlek.oxygenupdater.views;

import android.support.v4.app.Fragment;

import com.arjanvlek.oxygenupdater.ApplicationContext;

import java.util.Arrays;
import java.util.List;


public abstract class AbstractFragment extends Fragment {

    private ApplicationContext applicationContext;
    //Test devices for ads.
    public static final List<String> ADS_TEST_DEVICES = Arrays.asList("");

    public ApplicationContext getApplicationContext() {
        if(applicationContext == null) {
            try {
                applicationContext = (ApplicationContext) getActivity().getApplication();
            } catch (Exception e) {
                applicationContext = new ApplicationContext();
            }
        }
        return applicationContext;
    }
}
