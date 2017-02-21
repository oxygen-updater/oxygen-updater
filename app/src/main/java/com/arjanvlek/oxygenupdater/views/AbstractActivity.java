package com.arjanvlek.oxygenupdater.views;

import android.support.v7.app.AppCompatActivity;

import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.Server.ServerConnector;

public class AbstractActivity extends AppCompatActivity {

    private ApplicationContext applicationContext;


    protected ApplicationContext getAppApplicationContext() {
        if(applicationContext == null) {
            applicationContext = (ApplicationContext)getApplication();
        }
        return applicationContext;
    }
}
