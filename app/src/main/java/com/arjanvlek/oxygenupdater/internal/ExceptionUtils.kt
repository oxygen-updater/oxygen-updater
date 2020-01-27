package com.arjanvlek.oxygenupdater.internal

import java.io.FileNotFoundException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Oxygen Updater, copyright 2018 Arjan Vlek. File created by arjan.vlek on 18-01-18.
 */
object ExceptionUtils {
    fun isNetworkError(t: Throwable): Boolean {
        return (t is SocketException
                || t is SocketTimeoutException
                || t is SSLException
                || t is UnknownHostException
                || t is FileNotFoundException && t.message!!.contains("http"))
    }
}
