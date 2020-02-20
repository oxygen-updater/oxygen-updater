package com.arjanvlek.oxygenupdater.utils

import java.io.FileNotFoundException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
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
