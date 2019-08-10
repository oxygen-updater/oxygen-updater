package com.arjanvlek.oxygenupdater.internal.server

import com.arjanvlek.oxygenupdater.ApplicationData.Companion.APP_USER_AGENT
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/**
 * Oxygen Updater - © 2018 Arjan Vlek
 */
object RedirectingResourceStream {

    private const val USER_AGENT_TAG = "User-Agent"

    @Throws(IOException::class)
    fun getInputStream(url: String): InputStream {
        val url = url
        val resourceUrl: URL
        val conn: HttpURLConnection
        var location: String
        val base: URL
        val next: URL

        while (true) {
            resourceUrl = URL(url)
            conn = resourceUrl.openConnection() as HttpURLConnection

            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = false   // Make the logic below easier to detect redirections
            conn.setRequestProperty(USER_AGENT_TAG, APP_USER_AGENT)

            when (conn.responseCode) {
                HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
                    location = conn.getHeaderField("Location")
                    location = URLDecoder.decode(location, "UTF-8")
                    base = URL(url)
                    next = URL(base, location)  // Deal with relative URLs
                    next.toExternalForm()
                }
            }

            break
        }

        return conn.inputStream
    }
}
