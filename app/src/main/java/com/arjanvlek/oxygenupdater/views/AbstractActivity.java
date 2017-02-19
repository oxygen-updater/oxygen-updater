package com.arjanvlek.oxygenupdater.views;

import android.support.v7.app.AppCompatActivity;

import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.Server.ServerConnector;

public class AbstractActivity extends AppCompatActivity {

    private ServerConnector serverConnector;
    private ApplicationContext applicationContext;

    public ServerConnector getServerConnector() {
        if(serverConnector == null) {
            serverConnector = getAppApplicationContext().getServerConnector();
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
