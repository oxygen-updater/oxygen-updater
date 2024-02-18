package com.oxygenupdater.ui.settings

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.test.rule.GrantPermissionRule.grant
import org.junit.rules.TestRule

/** [TestRule] granting [POST_NOTIFICATIONS] on [Android 13/T][TIRAMISU]+ */
class GrantPostNotificationsPermissionRule : TestRule by if (SDK_INT >= TIRAMISU) grant(POST_NOTIFICATIONS) else grant()
