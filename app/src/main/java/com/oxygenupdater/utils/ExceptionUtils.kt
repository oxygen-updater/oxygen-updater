package com.oxygenupdater.utils

import java.io.FileNotFoundException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
fun isNetworkError(t: Throwable): Boolean {
    return (t is SocketException
            || t is SocketTimeoutException
            || t is SSLException
            || t is ProtocolException
            || t is UnknownHostException
            || t is FileNotFoundException && t.message?.contains("http") == true)
}
