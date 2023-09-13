@file:Suppress("NOTHING_TO_INLINE")

package com.oxygenupdater.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.IntRange
import com.oxygenupdater.activities.InstallGuideActivity
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.activities.MainActivity.Companion.PageSettings
import com.oxygenupdater.activities.MainActivity.Companion.PageUpdate
import com.oxygenupdater.activities.NewsItemActivity
import com.oxygenupdater.activities.OnboardingActivity

inline fun <reified T : Activity> Context.startActivity() = startActivity(
    Intent(this, T::class.java)
)

inline fun Activity.startOnboardingActivity(
    @IntRange(PageUpdate.toLong(), PageSettings.toLong()) startPage: Int,
) = startActivity(
    Intent(this, OnboardingActivity::class.java).putExtra(
        MainActivity.IntentStartPage, startPage
    )
)

inline fun Activity.startMainActivity(
    @IntRange(PageUpdate.toLong(), PageSettings.toLong()) startPage: Int,
) = startActivity(
    Intent(this, MainActivity::class.java).putExtra(
        MainActivity.IntentStartPage, startPage
    )
)

// TODO(compose/news): handle shared element transition, see `movableContentOf` and `LookaheadScope`
inline fun Activity.startNewsItemActivity(id: Long) = startActivity(
    Intent(this, NewsItemActivity::class.java).putExtra(
        NewsItemActivity.IntentNewsItemId, id
    )
)

inline fun Context.startInstallActivity(
    showDownloadInstructions: Boolean,
) = startActivity(
    Intent(this, InstallGuideActivity::class.java).putExtra(
        InstallGuideActivity.IntentShowDownloadInstructions, showDownloadInstructions
    )
)
