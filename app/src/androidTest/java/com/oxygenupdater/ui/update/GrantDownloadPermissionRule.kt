package com.oxygenupdater.ui.update

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.test.rule.GrantPermissionRule.grant
import org.junit.rules.TestRule

/**
 * [TestRule] granting the appropriate permission necessary for downloading an OTA ZIP.
 *
 * Note that for [Android 11/R][VERSION_CODES.R]+, requesting [MANAGE_EXTERNAL_STORAGE]
 * does not work by this method. Instead, it's done in [UpdateAvailableContentTest.setup].
 *
 * For older Android versions, [WRITE_EXTERNAL_STORAGE] is requested (which automatically
 * grants `READ_EXTERNAL_STORAGE` too).
 */
class GrantDownloadPermissionRule : TestRule by
if (SDK_INT >= VERSION_CODES.R) grant(/*no-op*/) else grant(WRITE_EXTERNAL_STORAGE)
