package com.oxygenupdater.utils

import com.topjohnwu.superuser.Shell
import java.util.concurrent.atomic.AtomicBoolean

private val checkedOnce = AtomicBoolean(false)
private var rootAccess = false

/**
 * Checks if the device is rooted and the app has been granted root access.
 * The result of [Shell.isAppGrantedRoot] is cached in [rootAccess].
 *
 * Note: this should be called only after first checking if user has enabled OTA contribution,
 * since that's the only part of the app that requires root access.
 *
 * @param callback `true` if device is rooted and user granted root permission, `false` otherwise
 */
fun hasRootAccess(callback: (result: Boolean) -> Unit) = if (checkedOnce.get()) {
    callback.invoke(rootAccess)
} else Shell.isAppGrantedRoot()?.let {
    checkedOnce.set(true)
    rootAccess = it
    callback.invoke(it)
} ?: Shell.getShell {
    // No root shell existed, so we needed to create one. It is cached by the library.
    // The only way to check if a device is rooted is by attempting to create a root shell,
    // which automatically triggers the superuser request dialog.
    checkedOnce.set(true)
    rootAccess = Shell.isAppGrantedRoot() == true
    callback.invoke(rootAccess)
}
