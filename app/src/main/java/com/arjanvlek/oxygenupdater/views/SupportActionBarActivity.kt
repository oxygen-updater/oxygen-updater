package com.arjanvlek.oxygenupdater.views

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updatePadding
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.doOnApplyWindowInsets

/**
 * Sets support action bar and enables home up button on the toolbar.
 * Additionally, it sets up a full screen activity if its theme is [R.style.Theme_Oxygen_FullScreen]
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
abstract class SupportActionBarActivity : AppCompatActivity() {

    protected var applicationData: ApplicationData? = null
        get() {
            if (field == null) {
                field = application as ApplicationData
            }

            return field
        }
        private set

    override fun setContentView(@LayoutRes layoutResId: Int) {
        super.setContentView(layoutResId)
        setupToolbar()
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        setupToolbar()
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams) {
        super.setContentView(view, params)
        setupToolbar()
    }

    private fun setupToolbar() {
        // Postpone the transition until the window's decor view has finished its layout.
        postponeEnterTransition()

        val decor = window.decorView
        decor.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                decor.viewTreeObserver.removeOnPreDrawListener(this)
                startPostponedEnterTransition()
                return true
            }
        })

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // allow activity to draw itself full screen
        if (packageManager.getActivityInfo(componentName, 0).themeResource == R.style.Theme_Oxygen_FullScreen) {
            findViewById<ViewGroup>(android.R.id.content).getChildAt(0).apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

                doOnApplyWindowInsets { view, insets, initialPadding ->
                    // initialPadding contains the original padding values after inflation
                    view.updatePadding(bottom = initialPadding.bottom + insets.systemWindowInsetBottom)
                }
            }
        }
    }
}
