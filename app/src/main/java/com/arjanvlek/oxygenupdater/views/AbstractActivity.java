package com.arjanvlek.oxygenupdater.views;

import android.support.v7.app.AppCompatActivity;

import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.Support.ServerConnector;

public class AbstractActivity extends AppCompatActivity {

    private ServerConnector serverConnector;
    private ApplicationContext applicationContext;

    public ServerConnector getServerConnector() {
        if(applicationContext == null) {
            applicationContext = getAppApplicationContext();
        }
        if(serverConnector == null) {
            serverConnector = applicationContext.getServerConnector();
        }
        return serverConnector;
    }

    protected ApplicationContext getAppApplicationContext() {
        if(applicationContext == null) {
            applicationContext = (ApplicationContext)getApplication();
        }
        return applicationContext;
    }
}
