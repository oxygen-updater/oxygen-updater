package com.arjanvlek.oxygenupdater.internal.server

import com.arjanvlek.oxygenupdater.ApplicationData
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
    fun getInputStream(url: String?): InputStream {
        var url = url

        var resourceUrl: URL
        var conn: HttpURLConnection
        var location: String?
        var base: URL
        var next: URL

        loop@ while (true) {
            resourceUrl = URL(url)

            conn = resourceUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = false // Make the logic below easier to detect redirections
            conn.setRequestProperty(USER_AGENT_TAG, ApplicationData.APP_USER_AGENT)

            when (conn.responseCode) {
                HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
                    location = conn.getHeaderField("Location")
                    location = URLDecoder.decode(location, "UTF-8")

                    base = URL(url)

                    // Deal with relative URLs
                    next = URL(base, location)

                    url = next.toExternalForm()
                    continue@loop
                }
            }

            break
        }

        return conn.inputStream
    }
}
