package com.arjanvlek.oxygenupdater.internal.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import static com.arjanvlek.oxygenupdater.ApplicationData.APP_USER_AGENT;

/**
 * Oxygen Updater - © 2018 Arjan Vlek
 */
public class RedirectingResourceStream {

    private final static String USER_AGENT_TAG = "User-Agent";

    public static InputStream getInputStream(String url) throws IOException {
        URL resourceUrl;
        HttpURLConnection conn;
        String location;
        URL base;
        URL next;

        while (true)
        {
            resourceUrl = new URL(url);
            conn        = (HttpURLConnection) resourceUrl.openConnection();

            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(false);   // Make the logic below easier to detect redirections
            conn.setRequestProperty(USER_AGENT_TAG, APP_USER_AGENT);

            switch (conn.getResponseCode())
            {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    location = conn.getHeaderField("Location");
                    location = URLDecoder.decode(location, "UTF-8");
                    base     = new URL(url);
                    next     = new URL(base, location);  // Deal with relative URLs
                    url      = next.toExternalForm();
                    continue;
            }

            break;
        }

        return conn.getInputStream();
    }
}
