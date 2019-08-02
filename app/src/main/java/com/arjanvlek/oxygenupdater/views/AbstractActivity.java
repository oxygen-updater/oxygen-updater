package com.arjanvlek.oxygenupdater.views;

import androidx.appcompat.app.AppCompatActivity;

import com.arjanvlek.oxygenupdater.ApplicationData;

public class AbstractActivity extends AppCompatActivity {

    private ApplicationData applicationData;


    protected ApplicationData getApplicationData() {
        if (applicationData == null) {
            applicationData = (ApplicationData) getApplication();
        }
        return applicationData;
    }
}
