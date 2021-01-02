package com.oxygenupdater.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.oxygenupdater.R
import com.oxygenupdater.extensions.enableEdgeToEdgeUiSupport
import com.oxygenupdater.extensions.startMainActivity

/**
 * Sets support action bar and enables home up button on the toolbar.
 * Additionally, it sets up:
 * * shared element transitions (see [MaterialContainerTransform])
 * * a full screen activity if its theme is [R.style.Theme_OxygenUpdater_DayNight_FullScreen]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
abstract class SupportActionBarActivity(
    @LayoutRes contentLayoutId: Int,

    /**
     * Used in the `onBackPressed` callback, only if this is the task root and
     * we need to reroute to [MainActivity].
     *
     * Certain activities (e.g. install, news) can take advantage of this to
     * tie back to the correct tab, if opened from a notification for example.
     */
    @IntRange(
        from = MainActivity.PAGE_UPDATE.toLong(),
        to = MainActivity.PAGE_SETTINGS.toLong()
    )
    private val startPage: Int
) : BaseActivity(contentLayoutId) {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        setupTransitions()
        setupToolbar()

        // allow activity to draw itself full screen
        enableEdgeToEdgeUiSupport()

        // Add a lifecycle-aware callback that handles hierarchy properly
        // This is the preferred way to handle a custom back pressed behaviour
        // Callbacks are called in reverse order, meaning the last callback added
        // will be called. This is the preferred way to handle a custom back pressed
        // behaviour. Previously, `onBackPressed` was overridden instead.
        // See https://developer.android.com/guide/navigation/navigation-custom-back#activity_onbackpressed
        onBackPressedDispatcher.addCallback(this) {
            if (isTaskRoot) {
                // If this is the only activity left in the stack, call [MainActivity].
                startMainActivity(startPage)
            } else {
                // Otherwise call [finishAfterTransition].
                finishAfterTransition()
            }
        }
    }

    /**
     * Respond to the action bar's Up/Home button.
     * Delegate to [onBackPressed] if [android.R.id.home] is clicked, otherwise call `super`.
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> onBackPressed().let { true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupTransitions() {
        // Postpone the transition until the window's decor view has finished its layout.
        postponeEnterTransition()
        window.decorView.doOnPreDraw { startPostponedEnterTransition() }

        // Setup shared element transitions
        findViewById<View>(android.R.id.content).transitionName = intent?.getStringExtra(INTENT_TRANSITION_NAME)
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window.apply {
            sharedElementEnterTransition = buildContainerTransform(DURATION_ENTER)
            sharedElementReturnTransition = buildContainerTransform(DURATION_RETURN)
        }
    }

    private fun buildContainerTransform(
        duration: Long
    ) = MaterialContainerTransform().apply {
        setDuration(duration)
        addTarget(android.R.id.content)
        pathMotion = MaterialArcMotion()
        fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        containerColor = ContextCompat.getColor(
            this@SupportActionBarActivity,
            R.color.background
        )
    }

    private fun setupToolbar() {
        // We must use `findViewById` because
        // neither ViewBinding nor Kotlin View Extensions will correctly resolve an individual activity's toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun setNavBarColorToBackground() {
        window.navigationBarColor = ContextCompat.getColor(
            this,
            R.color.background
        )
    }

    fun setNavBarColorToBackgroundVariant() {
        window.navigationBarColor = ContextCompat.getColor(
            this,
            R.color.backgroundVariant
        )
    }

    companion object {
        private const val DURATION_ENTER = 300L
        private const val DURATION_RETURN = 275L

        const val INTENT_TRANSITION_NAME = "TRANSITION_NAME"
    }
}
