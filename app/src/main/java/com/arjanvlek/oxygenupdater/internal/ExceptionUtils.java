package com.arjanvlek.oxygenupdater.internal;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

/**
 * Oxygen Updater, copyright 2018 Arjan Vlek. File created by arjan.vlek on 18-01-18.
 */
public class ExceptionUtils {

    public static boolean isNetworkError(Throwable t) {
        return (t instanceof SocketException || t instanceof SocketTimeoutException || t instanceof SSLException || t instanceof UnknownHostException || (t instanceof FileNotFoundException && t.getMessage().contains("http")));
    }
}
