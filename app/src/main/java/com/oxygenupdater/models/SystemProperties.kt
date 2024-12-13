package com.oxygenupdater.models

import android.os.Build.UNKNOWN

@Volatile
var useSystemProperties = true

@Suppress("NOTHING_TO_INLINE")
@Throws(UnsupportedOperationException::class)
private inline fun invokeGet(key: String) = with(ClassAndMethod) {
    if (!useSystemProperties) throw UnsupportedOperationException("`useSystemProperties` is false")

    try {
        val value = second?.invoke(first, key) as? String
        if (value.isNullOrEmpty()) UNKNOWN else value // ensure we never return null/empty
    } catch (e: Exception) {
        useSystemProperties = false
        throw UnsupportedOperationException("`useSystemProperties` is false")
    }
}

fun systemProperty(key: String) = invokeGet(key)

/**
 * Reading system properties is an integral part of the app, without which it cannot perform
 * all its device/method/version detections, which is a very basic necessity to deliver the
 * correct OTA information to the user. These properties are read only once on a cold start.
 *
 * Prior to v5.10.0, we parsed the entire `getprop` (via [java.lang.Runtime.exec]) output,
 * which was quite slow of course, even after obvious optimizations. This took anywhere
 * between 18-30ms on debug builds running on KB2001_11_F.13 (OnePlus 8T OxygenOS 12).
 *
 * Using [android.os.SystemProperties] via reflection made it near-instantaneous (<1ms),
 * which reduces noticeable jank and makes the user experience far better.
 * **However, given that it's a `SystemApi`, this may not always work as expected (or at all)
 * in future Android releases.**
 */
private val ClassAndMethod = try {
    val clazz = Class.forName("android.os.SystemProperties")
    clazz to clazz.getMethod("get", String::class.java)
} catch (e: Exception) {
    useSystemProperties = false
    null to null
}
